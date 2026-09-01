"""Administrator order management (PRD 01, FR-08)."""

from django.contrib import admin, messages

from .models import Order, OrderItem, OrderStatusUpdate, Payment, Shipment
from .services import advance_order_status


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    readonly_fields = ["product", "product_name", "unit_price", "quantity"]
    can_delete = False


class StatusUpdateInline(admin.TabularInline):
    model = OrderStatusUpdate
    extra = 0
    readonly_fields = ["status", "note", "created_at"]
    can_delete = False


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = [
        "short_reference",
        "user",
        "status",
        "total_amount",
        "placed_at",
    ]
    list_filter = ["status", "placed_at"]
    search_fields = ["reference", "user__username", "user__email", "full_name"]
    readonly_fields = ["reference", "total_amount", "placed_at", "updated_at"]
    inlines = [OrderItemInline, StatusUpdateInline]
    date_hierarchy = "placed_at"
    actions = ["mark_processing", "mark_shipped", "mark_delivered"]

    def _bulk_advance(self, request, queryset, status, label):
        """Move the selected orders to a status, recording each change."""
        count = 0
        for order in queryset:
            advance_order_status(order, status, note=f"Marked {label} by administrator.")
            count += 1
        self.message_user(
            request, f"{count} order(s) marked {label}.", messages.SUCCESS
        )

    @admin.action(description="Mark selected orders as processing")
    def mark_processing(self, request, queryset):
        self._bulk_advance(request, queryset, Order.Status.PROCESSING, "processing")

    @admin.action(description="Mark selected orders as shipped")
    def mark_shipped(self, request, queryset):
        self._bulk_advance(request, queryset, Order.Status.SHIPPED, "shipped")

    @admin.action(description="Mark selected orders as delivered")
    def mark_delivered(self, request, queryset):
        self._bulk_advance(request, queryset, Order.Status.DELIVERED, "delivered")


@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ["transaction_id", "order", "method", "status", "amount", "created_at"]
    list_filter = ["status", "method", "created_at"]
    search_fields = ["transaction_id", "order__reference"]
    readonly_fields = ["transaction_id", "created_at"]


@admin.register(Shipment)
class ShipmentAdmin(admin.ModelAdmin):
    list_display = [
        "tracking_number",
        "order",
        "carrier",
        "current_location",
        "dispatched_at",
        "delivered_at",
    ]
    list_filter = ["carrier", "dispatched_at"]
    search_fields = ["tracking_number", "order__reference"]
    readonly_fields = ["tracking_number", "created_at"]
