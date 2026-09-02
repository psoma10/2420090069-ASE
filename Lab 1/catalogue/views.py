"""Product catalogue views (PRD 01, US-02 / FR-02, FR-03)."""

from django.core.paginator import Paginator
from django.db.models import Q
from django.shortcuts import get_object_or_404, render

from .models import Category, Product

PAGE_SIZE = 12


def _visible_products():
    return Product.objects.filter(is_active=True).select_related("category")


def product_list(request):
    """Browse the catalogue with optional keyword search (FR-03).

    The search term is passed to the ORM as a parameter, never interpolated
    into SQL, so the query is injection-safe.
    """
    query = request.GET.get("q", "").strip()
    category_slug = request.GET.get("category", "").strip()

    products = _visible_products()

    if query:
        products = products.filter(
            Q(name__icontains=query)
            | Q(description__icontains=query)
            | Q(category__name__icontains=query)
        )

    if category_slug:
        products = products.filter(category__slug=category_slug)

    paginator = Paginator(products, PAGE_SIZE)
    page = paginator.get_page(request.GET.get("page"))

    context = {
        "page_obj": page,
        "products": page.object_list,
        "categories": Category.objects.all(),
        "query": query,
        "active_category": category_slug,
        "result_count": paginator.count,
    }
    return render(request, "catalogue/product_list.html", context)


def product_detail(request, slug):
    """Show one product with its price and availability (FR-02)."""
    product = get_object_or_404(_visible_products(), slug=slug)
    related = (
        _visible_products()
        .filter(category=product.category)
        .exclude(pk=product.pk)[:4]
    )
    return render(
        request,
        "catalogue/product_detail.html",
        {"product": product, "related_products": related},
    )


def category_detail(request, slug):
    """List every active product within a category."""
    category = get_object_or_404(Category, slug=slug)
    products = _visible_products().filter(category=category)

    paginator = Paginator(products, PAGE_SIZE)
    page = paginator.get_page(request.GET.get("page"))

    return render(
        request,
        "catalogue/category_detail.html",
        {
            "category": category,
            "page_obj": page,
            "products": page.object_list,
            "result_count": paginator.count,
        },
    )
