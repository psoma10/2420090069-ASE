"""Shopping cart entities (PRD 01, section 9)."""

from decimal import Decimal

from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models


class Cart(models.Model):
    """A customer's open basket of intended purchases (US-03).

    One active cart per customer. The cart is retained after checkout as a
    historical record but marked inactive, so a new cart is started for the
    next purchase.
    """

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="carts"
    )
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at"]
        constraints = [
            models.UniqueConstraint(
                fields=["user"],
                condition=models.Q(is_active=True),
                name="unique_active_cart_per_user",
            )
        ]

    def __str__(self):
        return f"Cart #{self.pk} for {self.user}"

    @property
    def total(self):
        """Sum of every line in the cart."""
        return sum((item.subtotal for item in self.items.all()), Decimal("0.00"))

    @property
    def item_count(self):
        """Total number of units across all lines, used by the navbar badge."""
        return sum(item.quantity for item in self.items.all())

    @property
    def is_empty(self):
        return not self.items.exists()


class CartItem(models.Model):
    """A single product line within a cart (FR-04)."""

    cart = models.ForeignKey(Cart, on_delete=models.CASCADE, related_name="items")
    product = models.ForeignKey(
        "catalogue.Product", on_delete=models.CASCADE, related_name="cart_items"
    )
    quantity = models.PositiveIntegerField(default=1)
    added_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["added_at"]
        constraints = [
            models.UniqueConstraint(
                fields=["cart", "product"], name="unique_product_per_cart"
            ),
            models.CheckConstraint(
                check=models.Q(quantity__gte=1), name="cart_item_quantity_positive"
            ),
        ]

    def __str__(self):
        return f"{self.quantity} x {self.product.name}"

    def clean(self):
        """Reject quantities the catalogue cannot supply (FR-04 validation)."""
        if self.quantity < 1:
            raise ValidationError({"quantity": "Quantity must be at least 1."})
        if not self.product.can_supply(self.quantity):
            raise ValidationError(
                {"quantity": f"Only {self.product.stock} unit(s) of "
                             f"{self.product.name} are available."}
            )

    @property
    def subtotal(self):
        return self.product.price * self.quantity
