"""Tests for orders, payment and tracking (PRD 01, US-04, US-05 / FR-05..FR-07)."""

from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase
from django.urls import reverse

from cart.services import add_to_cart, get_active_cart
from catalogue.models import Category, Product

from .models import Order, Payment
from .services import (
    CheckoutError,
    advance_order_status,
    cancel_order,
    create_order_from_cart,
    process_payment,
)

User = get_user_model()


class OrderCreationTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="customer", email="c@example.com", password="Str0ng-Pass-2024"
        )
        self.category = Category.objects.create(name="Electronics")
        self.product = Product.objects.create(
            name="Speaker", category=self.category, price=Decimal("1999.00"), stock=10
        )
        self.cart = get_active_cart(self.user)

    def _place_order(self):
        return create_order_from_cart(
            self.cart,
            full_name="Test Customer",
            phone="9876543210",
            shipping_address="1 Test Road",
        )

    def test_order_is_created_from_cart(self):
        add_to_cart(self.cart, self.product, 2)
        order = self._place_order()

        self.assertEqual(order.items.count(), 1)
        self.assertEqual(order.total_amount, Decimal("3998.00"))
        self.assertEqual(order.status, Order.Status.PENDING)

    def test_stock_is_decremented_on_order(self):
        add_to_cart(self.cart, self.product, 3)
        self._place_order()

        self.product.refresh_from_db()
        self.assertEqual(self.product.stock, 7)

    def test_cart_is_retired_after_checkout(self):
        add_to_cart(self.cart, self.product, 1)
        self._place_order()

        self.cart.refresh_from_db()
        self.assertFalse(self.cart.is_active)
        # A fresh cart is issued for the next purchase.
        self.assertNotEqual(get_active_cart(self.user).pk, self.cart.pk)

    def test_empty_cart_cannot_be_checked_out(self):
        with self.assertRaises(CheckoutError):
            self._place_order()

    def test_price_is_frozen_at_time_of_order(self):
        add_to_cart(self.cart, self.product, 1)
        order = self._place_order()

        self.product.price = Decimal("2999.00")
        self.product.save()

        order.refresh_from_db()
        self.assertEqual(order.items.first().unit_price, Decimal("1999.00"))
        self.assertEqual(order.total_amount, Decimal("1999.00"))

    def test_checkout_fails_when_stock_drops_below_cart_quantity(self):
        add_to_cart(self.cart, self.product, 5)
        self.product.stock = 2
        self.product.save()

        with self.assertRaises(CheckoutError):
            self._place_order()

        # Nothing was written: the transaction rolled back.
        self.assertEqual(Order.objects.count(), 0)

    def test_initial_status_update_is_recorded(self):
        add_to_cart(self.cart, self.product, 1)
        order = self._place_order()
        self.assertEqual(order.status_updates.count(), 1)


class PaymentTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="payer", email="p@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Books")
        self.product = Product.objects.create(
            name="Textbook", category=category, price=Decimal("899.00"), stock=5
        )
        cart = get_active_cart(self.user)
        add_to_cart(cart, self.product, 2)
        self.order = create_order_from_cart(
            cart, full_name="Payer", phone="9876543210", shipping_address="2 Test Road"
        )

    def test_successful_payment_confirms_order(self):
        payment = process_payment(
            self.order, method=Payment.Method.CARD, simulate_success=True
        )
        self.order.refresh_from_db()

        self.assertEqual(payment.status, Payment.Status.SUCCESS)
        self.assertEqual(self.order.status, Order.Status.CONFIRMED)
        self.assertTrue(self.order.is_paid)

    def test_successful_payment_creates_shipment(self):
        process_payment(self.order, method=Payment.Method.CARD, simulate_success=True)
        self.order.refresh_from_db()
        self.assertTrue(hasattr(self.order, "shipment"))
        self.assertTrue(self.order.shipment.tracking_number)

    def test_failed_payment_leaves_order_pending(self):
        payment = process_payment(
            self.order, method=Payment.Method.CARD, simulate_success=False
        )
        self.order.refresh_from_db()

        self.assertEqual(payment.status, Payment.Status.FAILED)
        self.assertEqual(self.order.status, Order.Status.PENDING)
        self.assertFalse(self.order.is_paid)

    def test_transaction_id_is_generated(self):
        payment = process_payment(
            self.order, method=Payment.Method.UPI, simulate_success=True
        )
        self.assertTrue(payment.transaction_id.startswith("TXN"))

    def test_cash_on_delivery_always_succeeds(self):
        payment = process_payment(self.order, method=Payment.Method.COD)
        self.assertEqual(payment.status, Payment.Status.SUCCESS)

    def test_paying_twice_is_rejected(self):
        process_payment(self.order, method=Payment.Method.CARD, simulate_success=True)
        self.order.refresh_from_db()
        with self.assertRaises(ValidationError):
            process_payment(self.order, method=Payment.Method.CARD, simulate_success=True)

    def test_amount_matches_order_total(self):
        payment = process_payment(
            self.order, method=Payment.Method.CARD, simulate_success=True
        )
        self.assertEqual(payment.amount, self.order.total_amount)


class TrackingAndCancellationTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="tracker", email="t@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Home")
        self.product = Product.objects.create(
            name="Lamp", category=category, price=Decimal("1200.00"), stock=8
        )
        cart = get_active_cart(self.user)
        add_to_cart(cart, self.product, 2)
        self.order = create_order_from_cart(
            cart, full_name="Tracker", phone="9876543210", shipping_address="3 Test Road"
        )

    def test_status_change_is_recorded_in_history(self):
        before = self.order.status_updates.count()
        advance_order_status(self.order, Order.Status.PROCESSING, "Packing items.")
        self.assertEqual(self.order.status_updates.count(), before + 1)

    def test_shipped_status_stamps_dispatch_time(self):
        process_payment(self.order, method=Payment.Method.COD)
        self.order.refresh_from_db()
        advance_order_status(self.order, Order.Status.SHIPPED)

        self.order.shipment.refresh_from_db()
        self.assertIsNotNone(self.order.shipment.dispatched_at)

    def test_delivered_status_stamps_delivery_time(self):
        process_payment(self.order, method=Payment.Method.COD)
        self.order.refresh_from_db()
        advance_order_status(self.order, Order.Status.SHIPPED)
        advance_order_status(self.order, Order.Status.DELIVERED)

        self.order.shipment.refresh_from_db()
        self.assertIsNotNone(self.order.shipment.delivered_at)

    def test_unknown_status_is_rejected(self):
        with self.assertRaises(ValidationError):
            advance_order_status(self.order, "teleported")

    def test_cancelling_restores_stock(self):
        self.product.refresh_from_db()
        stock_after_order = self.product.stock

        cancel_order(self.order)

        self.product.refresh_from_db()
        self.assertEqual(self.product.stock, stock_after_order + 2)
        self.assertEqual(self.order.status, Order.Status.CANCELLED)

    def test_shipped_order_cannot_be_cancelled(self):
        advance_order_status(self.order, Order.Status.SHIPPED)
        with self.assertRaises(ValidationError):
            cancel_order(self.order)


class OrderViewTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="viewer", email="v@example.com", password="Str0ng-Pass-2024"
        )
        self.other = User.objects.create_user(
            username="intruder", email="i@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Toys")
        self.product = Product.objects.create(
            name="Puzzle", category=category, price=Decimal("650.00"), stock=6
        )
        cart = get_active_cart(self.user)
        add_to_cart(cart, self.product, 1)
        self.order = create_order_from_cart(
            cart, full_name="Viewer", phone="9876543210", shipping_address="4 Test Road"
        )

    def test_checkout_requires_login(self):
        response = self.client.get(reverse("orders:checkout"))
        self.assertEqual(response.status_code, 302)

    def test_checkout_with_empty_cart_redirects(self):
        self.client.force_login(self.user)
        response = self.client.get(reverse("orders:checkout"))
        self.assertRedirects(response, reverse("catalogue:product_list"))

    def test_customer_can_view_own_order(self):
        self.client.force_login(self.user)
        response = self.client.get(
            reverse("orders:order_detail", args=[self.order.reference])
        )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.order.short_reference)

    def test_customer_cannot_view_another_customers_order(self):
        self.client.force_login(self.other)
        response = self.client.get(
            reverse("orders:order_detail", args=[self.order.reference])
        )
        self.assertEqual(response.status_code, 404)

    def test_tracking_page_shows_status_history(self):
        self.client.force_login(self.user)
        response = self.client.get(
            reverse("orders:order_tracking", args=[self.order.reference])
        )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Pending Payment")

    def test_order_list_shows_only_own_orders(self):
        self.client.force_login(self.other)
        response = self.client.get(reverse("orders:order_list"))
        self.assertNotContains(response, self.order.short_reference)

    def test_payment_view_completes_the_flow(self):
        self.client.force_login(self.user)
        self.client.post(
            reverse("orders:payment", args=[self.order.reference]),
            {"method": Payment.Method.COD},
        )
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, Order.Status.CONFIRMED)
