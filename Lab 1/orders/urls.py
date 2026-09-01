"""Order routes (PRD 01, FR-05, FR-06, FR-07)."""

from django.urls import path

from . import views

app_name = "orders"

urlpatterns = [
    path("checkout/", views.checkout, name="checkout"),
    path("", views.order_list, name="order_list"),
    path("<uuid:reference>/", views.order_detail, name="order_detail"),
    path("<uuid:reference>/payment/", views.payment, name="payment"),
    path("<uuid:reference>/tracking/", views.order_tracking, name="order_tracking"),
    path("<uuid:reference>/cancel/", views.order_cancel, name="order_cancel"),
]
