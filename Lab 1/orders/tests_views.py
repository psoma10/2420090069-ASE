"""View-layer and form tests for orders (PRD 01, US-04, US-05).

Kept in a separate module from tests.py so the service-level tests and the
HTTP-level tests stay easy to run and read independently.
"""

from decimal import Decimal

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse

from cart.services import add_to_cart, get_active_cart
from catalogue.models import Category, Product

from .forms import CheckoutForm
from .models import Order, Payment
from .services import advance_order_status, create_order_from_cart, process_payment

User = get_user_model()


class CheckoutFormTests(TestCase):
    """Covers CheckoutForm validation and profile pre-fill."""

    def setUp(self):
        self.user = User.objects.create_user(
            username="former",
            email="f@example.com",
            password="Str0ng-Pass-2024",
            first_name="Asha",
            last_name="Rao",
            phone="9876543210",
            address="9 Garden Lane",
        )

    def _form(self, **overrides):
        data = {
            "full_name": "Asha Rao",
            "phone": "9876543210",
            "shipping_address": "9 Garden Lane",
        }
        data.update(overrides)
        return CheckoutForm(data)

    def test_valid_details_are_accepted(self):
        self.assertTrue(self._form().is_valid())

    def test_phone_with_letters_is_rejected(self):
        form = self._form(phone="not-a-number")
        self.assertFalse(form.is_valid())
        self.assertIn("phone", form.errors)

    def test_too_short_phone_is_rejected(self):
        self.assertFalse(self._form(phone="12345").is_valid())

    def test_phone_with_spaces_and_plus_is_accepted(self):
        self.assertTrue(self._form(phone="+91 98765 43210").is_valid())

    def test_missing_address_is_rejected(self):
        self.assertFalse(self._form(shipping_address="").is_valid())

    def test_form_prefills_from_profile(self):
        form = CheckoutForm(user=self.user)
        self.assertEqual(form.fields["full_name"].initial, "Asha Rao")
        self.assertEqual(form.fields["phone"].initial, "9876543210")
        self.assertEqual(form.fields["shipping_address"].initial, "9 Garden Lane")

    def test_prefill_falls_back_to_username(self):
        plain = User.objects.create_user(
            username="plain", email="plain@example.com", password="Str0ng-Pass-2024"
        )
        form = CheckoutForm(user=plain)
        self.assertEqual(form.fields["full_name"].initial, "plain")


class CheckoutViewTests(TestCase):
    """Covers the checkout view end to end, including its failure branches."""

    def setUp(self):
        self.user = User.objects.create_user(
            username="checkout", email="co@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Stationery")
        self.product = Product.objects.create(
            name="Notebook", category=category, price=Decimal("120.00"), stock=10
        )
        self.client.force_login(self.user)

    def test_checkout_page_renders_with_items(self):
        add_to_cart(get_active_cart(self.user), self.product, 2)
        response = self.client.get(reverse("orders:checkout"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Notebook")

    def test_valid_checkout_creates_order_and_redirects_to_payment(self):
        add_to_cart(get_active_cart(self.user), self.product, 2)
        response = self.client.post(
            reverse("orders:checkout"),
            {
                "full_name": "Checkout Customer",
                "phone": "9876543210",
                "shipping_address": "5 Test Street",
            },
        )
        order = Order.objects.get(user=self.user)
        self.assertRedirects(response, reverse("orders:payment", args=[order.reference]))
        self.assertEqual(order.items.count(), 1)

    def test_invalid_details_do_not_create_an_order(self):
        add_to_cart(get_active_cart(self.user), self.product, 1)
        response = self.client.post(
            reverse("orders:checkout"),
            {"full_name": "", "phone": "bad", "shipping_address": ""},
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(Order.objects.count(), 0)

    def test_checkout_fails_gracefully_when_stock_disappears(self):
        add_to_cart(get_active_cart(self.user), self.product, 5)
        self.product.stock = 1
        self.product.save()

        response = self.client.post(
            reverse("orders:checkout"),
            {
                "full_name": "Checkout Customer",
                "phone": "9876543210",
                "shipping_address": "5 Test Street",
            },
        )
        self.assertRedirects(response, reverse("cart:cart_detail"))
        self.assertEqual(Order.objects.count(), 0)


class PaymentViewTests(TestCase):
    """Covers the payment view's branches."""

    def setUp(self):
        self.user = User.objects.create_user(
            username="payview", email="pv@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Music")
        self.product = Product.objects.create(
            name="Guitar Strings", category=category, price=Decimal("450.00"), stock=5
        )
        cart = get_active_cart(self.user)
        add_to_cart(cart, self.product, 1)
        self.order = create_order_from_cart(
            cart, full_name="Pay View", phone="9876543210", shipping_address="6 Road"
        )
        self.client.force_login(self.user)

    def test_payment_page_renders(self):
        response = self.client.get(reverse("orders:payment", args=[self.order.reference]))
        self.assertEqual(response.status_code, 200)

    def test_missing_method_is_rejected(self):
        response = self.client.post(
            reverse("orders:payment", args=[self.order.reference]), {}
        )
        self.assertEqual(response.status_code, 200)
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, Order.Status.PENDING)

    def test_already_paid_order_redirects_to_detail(self):
        process_payment(self.order, method=Payment.Method.COD)
        response = self.client.get(reverse("orders:payment", args=[self.order.reference]))
        self.assertRedirects(
            response, reverse("orders:order_detail", args=[self.order.reference])
        )

    def test_another_customer_cannot_pay_for_this_order(self):
        intruder = User.objects.create_user(
            username="thief", email="th@example.com", password="Str0ng-Pass-2024"
        )
        self.client.force_login(intruder)
        response = self.client.post(
            reverse("orders:payment", args=[self.order.reference]),
            {"method": Payment.Method.COD},
        )
        self.assertEqual(response.status_code, 404)


class OrderCancelViewTests(TestCase):
    """Covers cancellation through the view layer."""

    def setUp(self):
        self.user = User.objects.create_user(
            username="canceller", email="cx@example.com", password="Str0ng-Pass-2024"
        )
        category = Category.objects.create(name="Outdoors")
        self.product = Product.objects.create(
            name="Tent", category=category, price=Decimal("5500.00"), stock=4
        )
        cart = get_active_cart(self.user)
        add_to_cart(cart, self.product, 1)
        self.order = create_order_from_cart(
            cart, full_name="Canceller", phone="9876543210", shipping_address="7 Road"
        )
        self.client.force_login(self.user)

    def test_cancel_via_view_restores_stock(self):
        self.product.refresh_from_db()
        stock_after_order = self.product.stock

        self.client.post(reverse("orders:order_cancel", args=[self.order.reference]))

        self.order.refresh_from_db()
        self.product.refresh_from_db()
        self.assertEqual(self.order.status, Order.Status.CANCELLED)
        self.assertEqual(self.product.stock, stock_after_order + 1)

    def test_cancelling_shipped_order_via_view_shows_error(self):
        advance_order_status(self.order, Order.Status.SHIPPED)
        self.client.post(reverse("orders:order_cancel", args=[self.order.reference]))

        self.order.refresh_from_db()
        self.assertEqual(self.order.status, Order.Status.SHIPPED)

    def test_order_list_renders_for_owner(self):
        response = self.client.get(reverse("orders:order_list"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.order.short_reference)
