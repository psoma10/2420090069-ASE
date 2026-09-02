"""Checkout and payment forms (PRD 01, US-04 / FR-05, FR-06)."""

from django import forms

from .models import Payment


class CheckoutForm(forms.Form):
    """Delivery details captured when an order is placed."""

    full_name = forms.CharField(max_length=150)
    phone = forms.CharField(max_length=20)
    shipping_address = forms.CharField(widget=forms.Textarea(attrs={"rows": 4}))

    def __init__(self, *args, **kwargs):
        user = kwargs.pop("user", None)
        super().__init__(*args, **kwargs)
        for field in self.fields.values():
            field.widget.attrs.setdefault("class", "form-input")

        # Pre-fill from the profile so returning customers retype nothing.
        if user is not None and not self.is_bound:
            full_name = f"{user.first_name} {user.last_name}".strip()
            self.fields["full_name"].initial = full_name or user.username
            self.fields["phone"].initial = user.phone
            self.fields["shipping_address"].initial = user.address

    def clean_phone(self):
        phone = self.cleaned_data["phone"].strip()
        digits = phone.replace(" ", "").replace("-", "").lstrip("+")
        if not digits.isdigit() or not (7 <= len(digits) <= 15):
            raise forms.ValidationError("Enter a valid phone number.")
        return phone


class PaymentForm(forms.Form):
    """Payment method selection for the simulated gateway (FR-06).

    Deliberately collects no card data: the gateway is a simulation and
    storing card details would be both unnecessary and unsafe.
    """

    method = forms.ChoiceField(
        choices=Payment.Method.choices,
        widget=forms.RadioSelect,
        initial=Payment.Method.CARD,
    )
