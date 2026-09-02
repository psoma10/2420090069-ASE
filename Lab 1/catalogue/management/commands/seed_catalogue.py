"""Populate the catalogue with sample data for demonstration.

Usage:  python manage.py seed_catalogue
"""

from decimal import Decimal

from django.core.management.base import BaseCommand

from catalogue.models import Category, Product

CATALOGUE = {
    "Electronics": [
        ("Wireless Mouse", "Compact 2.4GHz wireless mouse with silent clicks.", "799.00", 40),
        ("Mechanical Keyboard", "Tactile switches with white backlighting.", "3499.00", 18),
        ("Noise Cancelling Headphones", "Over-ear headphones with 30 hour battery.", "6999.00", 12),
        ("27-inch Monitor", "QHD IPS display with adjustable stand.", "18999.00", 7),
        ("USB-C Hub", "Seven-in-one hub with HDMI and card reader.", "2199.00", 25),
    ],
    "Books": [
        ("Clean Code", "A handbook of agile software craftsmanship.", "1450.00", 30),
        ("The Pragmatic Programmer", "Your journey to mastery, 20th anniversary edition.", "1699.00", 22),
        ("Design Patterns", "Elements of reusable object-oriented software.", "1899.00", 15),
        ("Introduction to Algorithms", "Comprehensive reference on algorithms.", "3200.00", 9),
    ],
    "Home & Kitchen": [
        ("Ceramic Mug Set", "Set of four glazed stoneware mugs.", "1099.00", 35),
        ("Electric Kettle", "1.7 litre stainless steel kettle with auto shut-off.", "1899.00", 20),
        ("Desk Lamp", "Dimmable LED lamp with three colour temperatures.", "1299.00", 28),
    ],
    "Sports & Fitness": [
        ("Yoga Mat", "Six millimetre non-slip exercise mat.", "899.00", 45),
        ("Adjustable Dumbbells", "Pair of dumbbells adjustable from 2 to 20 kg.", "7499.00", 6),
        ("Running Shoes", "Lightweight cushioned shoes for daily running.", "4299.00", 16),
    ],
}


class Command(BaseCommand):
    help = "Create sample categories and products for demonstration."

    def handle(self, *args, **options):
        created_categories = 0
        created_products = 0

        for category_name, products in CATALOGUE.items():
            category, made = Category.objects.get_or_create(name=category_name)
            created_categories += int(made)

            for name, description, price, stock in products:
                _, made = Product.objects.get_or_create(
                    name=name,
                    defaults={
                        "category": category,
                        "description": description,
                        "price": Decimal(price),
                        "stock": stock,
                    },
                )
                created_products += int(made)

        self.stdout.write(
            self.style.SUCCESS(
                f"Seeded {created_categories} new category(ies) and "
                f"{created_products} new product(s)."
            )
        )
