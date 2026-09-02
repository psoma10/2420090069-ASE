"""Order and payment business logic (PRD 01, FR-05 and FR-06).

Kept out of the views so the rules can be unit tested directly and reused by
any future interface (API, management command).
"""

import random
from decimal import Decimal

from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone

from catalogue.models import Product

from .models import Order, OrderItem, OrderStatusUpdate, Payment, Shipment


class CheckoutError(Exception):
    """Raised when an order cannot be created from the given cart."""


@transaction.atomic
def create_order_from_cart(cart, *, full_name, phone, shipping_address):
    """Convert an active cart into an Order, decrementing stock.

    Runs in a single transaction: if any line cannot be supplied, nothing is
    written at all. Rows are locked for update so two concurrent checkouts
    cannot oversell the same product.
    """
    if cart.is_empty:
        raise CheckoutError("Your cart is empty.")

    items = list(cart.items.select_related("product"))

    # Lock the products so concurrent checkouts serialise on stock.
    product_ids = [item.product_id for item in items]
    locked = {
        p.pk: p
        for p in Product.objects.select_for_update().filter(pk__in=product_ids)
    }

    for item in items:
        product = locked[item.product_id]
        if not product.can_supply(item.quantity):
            raise CheckoutError(
                f"{product.name} no longer has {item.quantity} unit(s) in stock."
            )

    total = sum((item.subtotal for item in items), Decimal("0.00"))

    order = Order.objects.create(
        user=cart.user,
        full_name=full_name,
        phone=phone,
        shipping_address=shipping_address,
        total_amount=total,
        status=Order.Status.PENDING,
    )

    for item in items:
        product = locked[item.product_id]
        OrderItem.objects.create(
            order=order,
            product=product,
            product_name=product.name,
            unit_price=product.price,
            quantity=item.quantity,
        )
        product.stock -= item.quantity
        product.save(update_fields=["stock"])

    OrderStatusUpdate.objects.create(
        order=order,
        status=Order.Status.PENDING,
        note="Order placed and awaiting payment.",
    )

    # Retire the cart so the customer starts a fresh one next time.
    cart.is_active = False
    cart.save(update_fields=["is_active"])

    return order


@transaction.atomic
def process_payment(order, *, method, simulate_success=None):
    """Run the simulated payment gateway for an order (FR-06).

    No external service is contacted. Cash on delivery always succeeds; other
    methods succeed with high probability so the failure path stays reachable
    for demonstration. Pass ``simulate_success`` to force an outcome in tests.
    """
    if order.is_paid:
        raise ValidationError("This order has already been paid for.")

    if simulate_success is None:
        # Cash on delivery defers payment, so treat it as accepted.
        simulate_success = True if method == Payment.Method.COD else random.random() < 0.9

    payment, _ = Payment.objects.get_or_create(
        order=order,
        defaults={"method": method, "amount": order.total_amount},
    )
    payment.method = method
    payment.amount = order.total_amount
    payment.status = Payment.Status.SUCCESS if simulate_success else Payment.Status.FAILED
    payment.save()

    if simulate_success:
        order.status = Order.Status.CONFIRMED
        order.save(update_fields=["status"])
        OrderStatusUpdate.objects.create(
            order=order,
            status=Order.Status.CONFIRMED,
            note=f"Payment received via {payment.get_method_display()}.",
        )
        Shipment.objects.get_or_create(order=order)

    return payment


@transaction.atomic
def advance_order_status(order, new_status, note=""):
    """Move an order to a new status and record it in the history (FR-07)."""
    if new_status not in Order.Status.values:
        raise ValidationError(f"Unknown status: {new_status}")

    order.status = new_status
    order.save(update_fields=["status"])
    OrderStatusUpdate.objects.create(order=order, status=new_status, note=note)

    shipment = getattr(order, "shipment", None)
    if shipment:
        if new_status == Order.Status.SHIPPED and not shipment.dispatched_at:
            shipment.dispatched_at = timezone.now()
            shipment.save(update_fields=["dispatched_at"])
        elif new_status == Order.Status.DELIVERED and not shipment.delivered_at:
            shipment.delivered_at = timezone.now()
            shipment.save(update_fields=["delivered_at"])

    return order


@transaction.atomic
def cancel_order(order, *, note="Cancelled by customer."):
    """Cancel an order and return its stock to the catalogue."""
    if not order.is_cancellable:
        raise ValidationError("This order can no longer be cancelled.")

    for item in order.items.select_related("product"):
        product = item.product
        product.stock += item.quantity
        product.save(update_fields=["stock"])

    return advance_order_status(order, Order.Status.CANCELLED, note)
