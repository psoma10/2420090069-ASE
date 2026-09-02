"""User entity for the E-Commerce System (PRD 01, section 9)."""

from django.contrib.auth.models import AbstractUser
from django.db import models


class User(AbstractUser):
    """Customer or administrator account.

    Extends Django's AbstractUser so the built-in authentication, password
    hashing and permission machinery is reused rather than reimplemented.
    Administrators are identified by the standard ``is_staff`` flag, which
    also grants access to the Django admin used for FR-08.
    """

    email = models.EmailField(unique=True)
    phone = models.CharField(max_length=20, blank=True)
    address = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    REQUIRED_FIELDS = ["email"]

    class Meta:
        db_table = "accounts_user"
        ordering = ["-created_at"]

    def __str__(self):
        return self.username

    @property
    def is_administrator(self):
        """Administrators manage products, customers and orders (FR-08)."""
        return self.is_staff or self.is_superuser
