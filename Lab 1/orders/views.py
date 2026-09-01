"""Order, payment and tracking views (PRD 01, US-04, US-05 / FR-05..FR-07)."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.http import require_POST

from cart.services import get_active_cart

from .forms import CheckoutForm, PaymentForm
from .models import Order, Payment
from .services import CheckoutError, cancel_order, create_order_from_cart, process_payment


def _owned_order(request, reference):
    """Fetch an order, ensuring it belongs to the requesting customer.

    Scoping the lookup by user prevents one customer reading another's order
    by guessing a reference.
    """
    return get_object_or_404(Order, reference=reference, user=request.user)


@login_required
def checkout(request):
    """Collect delivery details and create the order (FR-05)."""
    cart = get_active_cart(request.user)

    if cart.is_empty:
        messages.info(request, "Your cart is empty.")
        return redirect("catalogue:product_list")

    if request.method == "POST":
        form = CheckoutForm(request.POST, user=request.user)
        if form.is_valid():
            try:
                order = create_order_from_cart(
                    cart,
                    full_name=form.cleaned_data["full_name"],
                    phone=form.cleaned_data["phone"],
                    shipping_address=form.cleaned_data["shipping_address"],
                )
            except CheckoutError as exc:
                messages.error(request, str(exc))
                return redirect("cart:cart_detail")

            messages.success(request, "Order created. Please complete payment.")
            return redirect("orders:payment", reference=order.reference)
        messages.error(request, "Please correct the errors below.")
    else:
        form = CheckoutForm(user=request.user)

    items = cart.items.select_related("product").all()
    return render(
        request, "orders/checkout.html", {"form": form, "cart": cart, "items": items}
    )


@login_required
def payment(request, reference):
    """Run the simulated payment gateway for an order (FR-06)."""
    order = _owned_order(request, reference)

    if order.is_paid:
        messages.info(request, "This order has already been paid for.")
        return redirect("orders:order_detail", reference=order.reference)

    if request.method == "POST":
        form = PaymentForm(request.POST)
        if form.is_valid():
            try:
                result = process_payment(order, method=form.cleaned_data["method"])
            except ValidationError as exc:
                messages.error(request, "; ".join(exc.messages))
                return redirect("orders:order_detail", reference=order.reference)

            if result.status == Payment.Status.SUCCESS:
                messages.success(
                    request, f"Payment successful. Transaction {result.transaction_id}."
                )
                return redirect("orders:order_detail", reference=order.reference)

            messages.error(request, "Payment failed. Please try another method.")
            return redirect("orders:payment", reference=order.reference)
        messages.error(request, "Please select a payment method.")
    else:
        form = PaymentForm()

    return render(request, "orders/payment.html", {"form": form, "order": order})


@login_required
def order_list(request):
    """List the customer's own orders, most recent first."""
    orders = (
        Order.objects.filter(user=request.user)
        .prefetch_related("items")
        .select_related("payment")
    )
    return render(request, "orders/order_list.html", {"orders": orders})


@login_required
def order_detail(request, reference):
    """Show one order with its lines, payment and delivery status."""
    order = _owned_order(request, reference)
    return render(
        request,
        "orders/order_detail.html",
        {
            "order": order,
            "items": order.items.all(),
            "payment": getattr(order, "payment", None),
            "shipment": getattr(order, "shipment", None),
        },
    )


@login_required
def order_tracking(request, reference):
    """Show the current delivery status and its history (US-05 / FR-07)."""
    order = _owned_order(request, reference)
    return render(
        request,
        "orders/order_tracking.html",
        {
            "order": order,
            "shipment": getattr(order, "shipment", None),
            "updates": order.status_updates.all(),
            "statuses": Order.Status.choices,
        },
    )


@login_required
@require_POST
def order_cancel(request, reference):
    """Cancel an order that has not yet shipped, restoring stock."""
    order = _owned_order(request, reference)

    try:
        cancel_order(order)
        messages.success(request, f"Order {order.short_reference} has been cancelled.")
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))

    return redirect("orders:order_detail", reference=order.reference)
