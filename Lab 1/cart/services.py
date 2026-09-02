"""Cart operations (PRD 01, FR-04)."""

from django.core.exceptions import ValidationError
from django.db import transaction

from .models import Cart, CartItem


def get_active_cart(user):
    """Return the customer's open cart, creating one on first use."""
    cart, _ = Cart.objects.get_or_create(user=user, is_active=True)
    return cart


@transaction.atomic
def add_to_cart(cart, product, quantity=1):
    """Add a product, or increase its quantity if already present.

    Validates the combined quantity against stock so repeated adds cannot
    exceed availability.
    """
    if quantity < 1:
        raise ValidationError("Quantity must be at least 1.")

    item = cart.items.filter(product=product).first()
    new_quantity = (item.quantity if item else 0) + quantity

    if not product.can_supply(new_quantity):
        raise ValidationError(
            f"Only {product.stock} unit(s) of {product.name} are available."
        )

    if item:
        item.quantity = new_quantity
        item.save(update_fields=["quantity"])
    else:
        item = CartItem.objects.create(cart=cart, product=product, quantity=quantity)

    return item


@transaction.atomic
def update_quantity(cart, product, quantity):
    """Set an exact quantity; a quantity of zero removes the line."""
    item = cart.items.filter(product=product).first()
    if item is None:
        raise ValidationError("That product is not in your cart.")

    if quantity < 1:
        item.delete()
        return None

    if not product.can_supply(quantity):
        raise ValidationError(
            f"Only {product.stock} unit(s) of {product.name} are available."
        )

    item.quantity = quantity
    item.save(update_fields=["quantity"])
    return item


def remove_from_cart(cart, product):
    """Remove a product line from the cart entirely."""
    deleted, _ = cart.items.filter(product=product).delete()
    if not deleted:
        raise ValidationError("That product is not in your cart.")


def clear_cart(cart):
    """Empty the cart without retiring it."""
    cart.items.all().delete()
