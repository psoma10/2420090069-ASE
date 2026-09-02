"""Tests for the product catalogue (PRD 01, US-02 / FR-02, FR-03)."""

from decimal import Decimal

from django.test import TestCase
from django.urls import reverse

from .models import Category, Product


class ProductModelTests(TestCase):
    def setUp(self):
        self.category = Category.objects.create(name="Electronics")

    def test_slug_is_generated_from_name(self):
        product = Product.objects.create(
            name="Wireless Mouse", category=self.category, price=Decimal("799.00"), stock=5
        )
        self.assertEqual(product.slug, "wireless-mouse")

    def test_in_stock_reflects_quantity(self):
        product = Product.objects.create(
            name="Keyboard", category=self.category, price=Decimal("1499.00"), stock=0
        )
        self.assertFalse(product.in_stock)
        product.stock = 3
        self.assertTrue(product.in_stock)

    def test_can_supply_respects_stock_and_active_flag(self):
        product = Product.objects.create(
            name="Monitor", category=self.category, price=Decimal("8999.00"), stock=2
        )
        self.assertTrue(product.can_supply(2))
        self.assertFalse(product.can_supply(3))

        product.is_active = False
        self.assertFalse(product.can_supply(1))


class CatalogueViewTests(TestCase):
    def setUp(self):
        self.electronics = Category.objects.create(name="Electronics")
        self.books = Category.objects.create(name="Books")

        self.laptop = Product.objects.create(
            name="Gaming Laptop",
            category=self.electronics,
            description="A fast portable computer",
            price=Decimal("74999.00"),
            stock=4,
        )
        self.novel = Product.objects.create(
            name="Mystery Novel",
            category=self.books,
            description="A gripping story",
            price=Decimal("399.00"),
            stock=10,
        )
        self.hidden = Product.objects.create(
            name="Discontinued Tablet",
            category=self.electronics,
            price=Decimal("9999.00"),
            stock=1,
            is_active=False,
        )

    def test_product_list_shows_only_active_products(self):
        response = self.client.get(reverse("catalogue:product_list"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Gaming Laptop")
        self.assertNotContains(response, "Discontinued Tablet")

    def test_search_matches_product_name(self):
        response = self.client.get(reverse("catalogue:product_list"), {"q": "laptop"})
        self.assertContains(response, "Gaming Laptop")
        self.assertNotContains(response, "Mystery Novel")

    def test_search_matches_description(self):
        response = self.client.get(reverse("catalogue:product_list"), {"q": "gripping"})
        self.assertContains(response, "Mystery Novel")

    def test_search_with_no_match_reports_zero_results(self):
        response = self.client.get(reverse("catalogue:product_list"), {"q": "zzzznothing"})
        self.assertEqual(response.context["result_count"], 0)

    def test_product_detail_shows_price_and_availability(self):
        response = self.client.get(self.laptop.get_absolute_url())
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "74999.00")
        self.assertContains(response, "In stock")

    def test_inactive_product_detail_returns_404(self):
        response = self.client.get(self.hidden.get_absolute_url())
        self.assertEqual(response.status_code, 404)

    def test_category_detail_lists_its_products(self):
        response = self.client.get(self.books.get_absolute_url())
        self.assertContains(response, "Mystery Novel")
        self.assertNotContains(response, "Gaming Laptop")
