"""Administrator customer management (PRD 01, FR-08)."""

from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin

from .models import User


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ["username", "email", "phone", "is_staff", "is_active", "created_at"]
    list_filter = ["is_staff", "is_active", "created_at"]
    search_fields = ["username", "email", "phone"]
    readonly_fields = ["created_at", "last_login", "date_joined"]

    fieldsets = BaseUserAdmin.fieldsets + (
        ("Contact details", {"fields": ("phone", "address")}),
    )
    add_fieldsets = BaseUserAdmin.add_fieldsets + (
        ("Contact details", {"fields": ("email", "phone", "address")}),
    )
