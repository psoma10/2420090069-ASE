"""Order, payment and shipment entities (PRD 01, section 9)."""

import uuid
from decimal import Decimal

from django.conf import settings
from django.db import models


class Order(models.Model):
    """A confirmed purchase created from a customer's cart (FR-05).

    Line prices are copied onto OrderItem at creation time so that later
    catalogue price changes never alter the value of a historical order.
    """

    class Status(models.TextChoices):
        PENDING = "pending", "Pending Payment"
        CONFIRMED = "confirmed", "Confirmed"
        PROCESSING = "processing", "Processing"
        SHIPPED = "shipped", "Shipped"
        DELIVERED = "delivered", "Delivered"
        CANCELLED = "cancelled", "Cancelled"

    reference = models.UUIDField(default=uuid.uuid4, editable=False, unique=True)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="orders"
    )
    status = models.CharField(
        max_length=20, choices=Status.choices, default=Status.PENDING, db_index=True
    )
    full_name = models.CharField(max_length=150)
    phone = models.CharField(max_length=20)
    shipping_address = models.TextField()
    total_amount = models.DecimalField(max_digits=12, decimal_places=2)
    placed_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-placed_at"]
        indexes = [models.Index(fields=["user", "-placed_at"])]

    def __str__(self):
        return f"Order {self.short_reference}"

    @property
    def short_reference(self):
        """Human-friendly order number shown to the customer."""
        return str(self.reference).split("-")[0].upper()

    @property
    def is_cancellable(self):
        """Customers may cancel only before the order has shipped."""
        return self.status in {self.Status.PENDING, self.Status.CONFIRMED}

    @property
    def is_paid(self):
        return hasattr(self, "payment") and self.payment.status == Payment.Status.SUCCESS


class OrderItem(models.Model):
    """A product line frozen at the price paid (FR-05)."""

    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name="items")
    product = models.ForeignKey(
        "catalogue.Product", on_delete=models.PROTECT, related_name="order_items"
    )
    product_name = models.CharField(max_length=200)
    unit_price = models.DecimalField(max_digits=10, decimal_places=2)
    quantity = models.PositiveIntegerField()

    class Meta:
        ordering = ["id"]

    def __str__(self):
        return f"{self.quantity} x {self.product_name}"

    @property
    def subtotal(self):
        return self.unit_price * self.quantity


class Payment(models.Model):
    """Payment attempt against an order (FR-06).

    This is a simulated gateway: no external service is contacted and no card
    data is stored. Only the outcome and a generated transaction id are kept.
    """

    class Status(models.TextChoices):
        PENDING = "pending", "Pending"
        SUCCESS = "success", "Successful"
        FAILED = "failed", "Failed"

    class Method(models.TextChoices):
        CARD = "card", "Credit/Debit Card"
        UPI = "upi", "UPI"
        NETBANKING = "netbanking", "Net Banking"
        COD = "cod", "Cash on Delivery"

    order = models.OneToOneField(
        Order, on_delete=models.CASCADE, related_name="payment"
    )
    method = models.CharField(max_length=20, choices=Method.choices)
    status = models.CharField(
        max_length=20, choices=Status.choices, default=Status.PENDING
    )
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    transaction_id = models.CharField(max_length=64, unique=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Payment for {self.order} ({self.get_status_display()})"

    def save(self, *args, **kwargs):
        if not self.transaction_id:
            self.transaction_id = f"TXN{uuid.uuid4().hex[:16].upper()}"
        super().save(*args, **kwargs)


class Shipment(models.Model):
    """Delivery tracking record for an order (FR-07)."""

    order = models.OneToOneField(
        Order, on_delete=models.CASCADE, related_name="shipment"
    )
    tracking_number = models.CharField(max_length=64, unique=True, blank=True)
    carrier = models.CharField(max_length=100, default="Standard Delivery")
    dispatched_at = models.DateTimeField(null=True, blank=True)
    delivered_at = models.DateTimeField(null=True, blank=True)
    current_location = models.CharField(max_length=200, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Shipment {self.tracking_number}"

    def save(self, *args, **kwargs):
        if not self.tracking_number:
            self.tracking_number = f"TRK{uuid.uuid4().hex[:12].upper()}"
        super().save(*args, **kwargs)


class OrderStatusUpdate(models.Model):
    """Append-only history of status changes, shown on the tracking page (FR-07)."""

    order = models.ForeignKey(
        Order, on_delete=models.CASCADE, related_name="status_updates"
    )
    status = models.CharField(max_length=20, choices=Order.Status.choices)
    note = models.CharField(max_length=255, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.order} -> {self.get_status_display()}"
