"""Tests for authentication (PRD 01, US-01 / FR-01)."""

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse

User = get_user_model()


class RegistrationTests(TestCase):
    def test_customer_can_register_and_is_logged_in(self):
        response = self.client.post(
            reverse("accounts:register"),
            {
                "username": "asha",
                "email": "asha@example.com",
                "phone": "9876543210",
                "password1": "Str0ng-Pass-2024",
                "password2": "Str0ng-Pass-2024",
            },
        )
        self.assertRedirects(response, reverse("catalogue:product_list"))
        self.assertTrue(User.objects.filter(username="asha").exists())
        self.assertIn("_auth_user_id", self.client.session)

    def test_duplicate_email_is_rejected(self):
        User.objects.create_user(
            username="existing", email="taken@example.com", password="Str0ng-Pass-2024"
        )
        response = self.client.post(
            reverse("accounts:register"),
            {
                "username": "newcomer",
                "email": "taken@example.com",
                "phone": "",
                "password1": "Str0ng-Pass-2024",
                "password2": "Str0ng-Pass-2024",
            },
        )
        self.assertEqual(response.status_code, 200)
        self.assertFalse(User.objects.filter(username="newcomer").exists())

    def test_password_is_stored_hashed(self):
        user = User.objects.create_user(
            username="hashcheck", email="h@example.com", password="Str0ng-Pass-2024"
        )
        self.assertNotEqual(user.password, "Str0ng-Pass-2024")
        self.assertTrue(user.check_password("Str0ng-Pass-2024"))


class LoginLogoutTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="ravi", email="ravi@example.com", password="Str0ng-Pass-2024"
        )

    def test_login_with_valid_credentials(self):
        response = self.client.post(
            reverse("accounts:login"),
            {"username": "ravi", "password": "Str0ng-Pass-2024"},
        )
        self.assertRedirects(response, reverse("catalogue:product_list"))

    def test_login_with_wrong_password_fails(self):
        response = self.client.post(
            reverse("accounts:login"), {"username": "ravi", "password": "wrong"}
        )
        self.assertEqual(response.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)

    def test_logout_clears_session(self):
        self.client.force_login(self.user)
        self.client.post(reverse("accounts:logout"))
        self.assertNotIn("_auth_user_id", self.client.session)


class ProfileTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="meera", email="meera@example.com", password="Str0ng-Pass-2024"
        )

    def test_profile_requires_login(self):
        response = self.client.get(reverse("accounts:profile"))
        self.assertEqual(response.status_code, 302)
        self.assertIn(reverse("accounts:login"), response.url)

    def test_customer_can_update_profile(self):
        self.client.force_login(self.user)
        self.client.post(
            reverse("accounts:profile"),
            {
                "first_name": "Meera",
                "last_name": "Nair",
                "email": "meera@example.com",
                "phone": "9000000000",
                "address": "12 Palm Street",
            },
        )
        self.user.refresh_from_db()
        self.assertEqual(self.user.first_name, "Meera")
        self.assertEqual(self.user.address, "12 Palm Street")

    def test_is_administrator_reflects_staff_flag(self):
        self.assertFalse(self.user.is_administrator)
        self.user.is_staff = True
        self.assertTrue(self.user.is_administrator)
