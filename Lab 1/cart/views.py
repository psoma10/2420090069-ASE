"""Shopping cart views (PRD 01, US-03 / FR-04)."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.http import require_POST

from catalogue.models import Product

from .services import add_to_cart, get_active_cart, remove_from_cart, update_quantity


@login_required
def cart_detail(request):
    """Show the current cart with per-line subtotals and a grand total."""
    cart = get_active_cart(request.user)
    items = cart.items.select_related("product").all()
    return render(request, "cart/cart_detail.html", {"cart": cart, "items": items})


@login_required
@require_POST
def cart_add(request, product_id):
    """Add a product to the cart, honouring the requested quantity."""
    product = get_object_or_404(Product, pk=product_id, is_active=True)
    cart = get_active_cart(request.user)

    try:
        quantity = int(request.POST.get("quantity", 1))
    except (TypeError, ValueError):
        messages.error(request, "Please enter a valid quantity.")
        return redirect(product.get_absolute_url())

    try:
        add_to_cart(cart, product, quantity)
        messages.success(request, f"{product.name} was added to your cart.")
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))

    return redirect(request.POST.get("next") or "cart:cart_detail")


@login_required
@require_POST
def cart_update(request, product_id):
    """Set an exact quantity for a line already in the cart."""
    product = get_object_or_404(Product, pk=product_id)
    cart = get_active_cart(request.user)

    try:
        quantity = int(request.POST.get("quantity", 1))
    except (TypeError, ValueError):
        messages.error(request, "Please enter a valid quantity.")
        return redirect("cart:cart_detail")

    try:
        item = update_quantity(cart, product, quantity)
        if item is None:
            messages.success(request, f"{product.name} was removed from your cart.")
        else:
            messages.success(request, "Your cart has been updated.")
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))

    return redirect("cart:cart_detail")


@login_required
@require_POST
def cart_remove(request, product_id):
    """Remove a product line from the cart."""
    product = get_object_or_404(Product, pk=product_id)
    cart = get_active_cart(request.user)

    try:
        remove_from_cart(cart, product)
        messages.success(request, f"{product.name} was removed from your cart.")
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))

    return redirect("cart:cart_detail")
