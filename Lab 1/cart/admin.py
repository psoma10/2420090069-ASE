"""Cart inspection for administrators (PRD 01, FR-08)."""

from django.contrib import admin

from .models import Cart, CartItem


class CartItemInline(admin.TabularInline):
    model = CartItem
    extra = 0
    readonly_fields = ["added_at"]


@admin.register(Cart)
class CartAdmin(admin.ModelAdmin):
    list_display = ["id", "user", "is_active", "item_count", "total", "updated_at"]
    list_filter = ["is_active", "created_at"]
    search_fields = ["user__username", "user__email"]
    inlines = [CartItemInline]

    @admin.display(description="Items")
    def item_count(self, obj):
        return obj.item_count

    @admin.display(description="Total")
    def total(self, obj):
        return obj.total
