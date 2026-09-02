"""Tests for the shopping cart (PRD 01, US-03 / FR-04)."""

from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase
from django.urls import reverse

from catalogue.models import Category, Product

from .services import add_to_cart, get_active_cart, remove_from_cart, update_quantity

User = get_user_model()


class CartServiceTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="buyer", email="buyer@example.com", password="Str0ng-Pass-2024"
        )
        self.category = Category.objects.create(name="Electronics")
        self.product = Product.objects.create(
            name="Headphones", category=self.category, price=Decimal("2499.00"), stock=5
        )
        self.cart = get_active_cart(self.user)

    def test_get_active_cart_is_idempotent(self):
        self.assertEqual(get_active_cart(self.user).pk, self.cart.pk)

    def test_add_product_to_cart(self):
        item = add_to_cart(self.cart, self.product, 2)
        self.assertEqual(item.quantity, 2)
        self.assertEqual(self.cart.items.count(), 1)

    def test_adding_same_product_twice_increases_quantity(self):
        add_to_cart(self.cart, self.product, 2)
        add_to_cart(self.cart, self.product, 1)
        self.assertEqual(self.cart.items.count(), 1)
        self.assertEqual(self.cart.items.first().quantity, 3)

    def test_cannot_add_more_than_stock(self):
        with self.assertRaises(ValidationError):
            add_to_cart(self.cart, self.product, 6)

    def test_repeated_adds_cannot_exceed_stock(self):
        add_to_cart(self.cart, self.product, 4)
        with self.assertRaises(ValidationError):
            add_to_cart(self.cart, self.product, 2)

    def test_zero_quantity_is_rejected(self):
        with self.assertRaises(ValidationError):
            add_to_cart(self.cart, self.product, 0)

    def test_update_quantity_sets_exact_value(self):
        add_to_cart(self.cart, self.product, 1)
        update_quantity(self.cart, self.product, 4)
        self.assertEqual(self.cart.items.first().quantity, 4)

    def test_update_to_zero_removes_the_line(self):
        add_to_cart(self.cart, self.product, 2)
        self.assertIsNone(update_quantity(self.cart, self.product, 0))
        self.assertEqual(self.cart.items.count(), 0)

    def test_remove_product(self):
        add_to_cart(self.cart, self.product, 1)
        remove_from_cart(self.cart, self.product)
        self.assertEqual(self.cart.items.count(), 0)

    def test_removing_absent_product_raises(self):
        with self.assertRaises(ValidationError):
            remove_from_cart(self.cart, self.product)

    def test_totals_are_calculated_correctly(self):
        second = Product.objects.create(
            name="Cable", category=self.category, price=Decimal("299.00"), stock=10
        )
        add_to_cart(self.cart, self.product, 2)
        add_to_cart(self.cart, second, 3)

        self.assertEqual(self.cart.total, Decimal("5895.00"))
        self.assertEqual(self.cart.item_count, 5)


class CartViewTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="shopper", email="shopper@example.com", password="Str0ng-Pass-2024"
        )
        self.category = Category.objects.create(name="Books")
        self.product = Product.objects.create(
            name="Novel", category=self.category, price=Decimal("499.00"), stock=3
        )

    def test_cart_requires_login(self):
        response = self.client.get(reverse("cart:cart_detail"))
        self.assertEqual(response.status_code, 302)

    def test_add_to_cart_via_view(self):
        self.client.force_login(self.user)
        self.client.post(
            reverse("cart:cart_add", args=[self.product.id]), {"quantity": 2}
        )
        cart = get_active_cart(self.user)
        self.assertEqual(cart.item_count, 2)

    def test_add_beyond_stock_shows_error_and_adds_nothing(self):
        self.client.force_login(self.user)
        self.client.post(
            reverse("cart:cart_add", args=[self.product.id]), {"quantity": 99}
        )
        self.assertEqual(get_active_cart(self.user).item_count, 0)

    def test_one_customer_cannot_see_another_cart(self):
        other = User.objects.create_user(
            username="other", email="other@example.com", password="Str0ng-Pass-2024"
        )
        add_to_cart(get_active_cart(other), self.product, 2)

        self.client.force_login(self.user)
        response = self.client.get(reverse("cart:cart_detail"))
        self.assertNotContains(response, "Novel")


class CartViewErrorPathTests(TestCase):
    """Covers the validation and error branches of the cart views."""

    def setUp(self):
        self.user = User.objects.create_user(
            username="edge", email="edge@example.com", password="Str0ng-Pass-2024"
        )
        self.category = Category.objects.create(name="Garden")
        self.product = Product.objects.create(
            name="Trowel", category=self.category, price=Decimal("249.00"), stock=4
        )
        self.client.force_login(self.user)

    def test_non_numeric_quantity_on_add_is_rejected(self):
        response = self.client.post(
            reverse("cart:cart_add", args=[self.product.id]), {"quantity": "many"}
        )
        self.assertEqual(response.status_code, 302)
        self.assertEqual(get_active_cart(self.user).item_count, 0)

    def test_non_numeric_quantity_on_update_is_rejected(self):
        add_to_cart(get_active_cart(self.user), self.product, 2)
        response = self.client.post(
            reverse("cart:cart_update", args=[self.product.id]), {"quantity": "lots"}
        )
        self.assertRedirects(response, reverse("cart:cart_detail"))
        self.assertEqual(get_active_cart(self.user).item_count, 2)

    def test_update_beyond_stock_is_rejected(self):
        add_to_cart(get_active_cart(self.user), self.product, 1)
        self.client.post(
            reverse("cart:cart_update", args=[self.product.id]), {"quantity": 99}
        )
        self.assertEqual(get_active_cart(self.user).items.first().quantity, 1)

    def test_update_to_zero_removes_line_via_view(self):
        add_to_cart(get_active_cart(self.user), self.product, 2)
        self.client.post(
            reverse("cart:cart_update", args=[self.product.id]), {"quantity": 0}
        )
        self.assertEqual(get_active_cart(self.user).items.count(), 0)

    def test_remove_absent_product_shows_error(self):
        response = self.client.post(
            reverse("cart:cart_remove", args=[self.product.id])
        )
        self.assertRedirects(response, reverse("cart:cart_detail"))

    def test_remove_product_via_view(self):
        add_to_cart(get_active_cart(self.user), self.product, 1)
        self.client.post(reverse("cart:cart_remove", args=[self.product.id]))
        self.assertEqual(get_active_cart(self.user).items.count(), 0)

    def test_add_respects_next_parameter(self):
        target = self.product.get_absolute_url()
        response = self.client.post(
            reverse("cart:cart_add", args=[self.product.id]),
            {"quantity": 1, "next": target},
        )
        self.assertRedirects(response, target)

    def test_adding_inactive_product_returns_404(self):
        self.product.is_active = False
        self.product.save()
        response = self.client.post(
            reverse("cart:cart_add", args=[self.product.id]), {"quantity": 1}
        )
        self.assertEqual(response.status_code, 404)

    def test_cart_detail_renders_with_items(self):
        add_to_cart(get_active_cart(self.user), self.product, 2)
        response = self.client.get(reverse("cart:cart_detail"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Trowel")
