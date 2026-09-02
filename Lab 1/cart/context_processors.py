"""Makes cart totals available to every template (navbar badge)."""

from .models import Cart


def cart_summary(request):
    """Expose the signed-in customer's cart count and total.

    Anonymous visitors get zeroes rather than a database query.
    """
    if not request.user.is_authenticated:
        return {"cart_item_count": 0, "cart_total": 0}

    cart = (
        Cart.objects.filter(user=request.user, is_active=True)
        .prefetch_related("items__product")
        .first()
    )
    if cart is None:
        return {"cart_item_count": 0, "cart_total": 0}

    return {"cart_item_count": cart.item_count, "cart_total": cart.total}
