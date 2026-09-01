"""Pytest suite covering the arithmetic helpers in ``app.py``."""

import pytest

from app import add, subtract


def test_add():
    assert add(2, 3) == 5


def test_subtract():
    assert subtract(5, 2) == 3


@pytest.mark.parametrize(
    "a, b, expected",
    [
        (0, 0, 0),
        (0, 7, 7),
        (-4, -6, -10),
        (-5, 5, 0),
        (1000000, 2000000, 3000000),
    ],
)
def test_add_parametrized(a, b, expected):
    """``add`` handles zeros, negatives and large integers."""
    assert add(a, b) == expected


@pytest.mark.parametrize(
    "a, b, expected",
    [
        (0, 0, 0),
        (0, 7, -7),
        (-4, -6, 2),
        (-5, 5, -10),
        (3000000, 1000000, 2000000),
    ],
)
def test_subtract_parametrized(a, b, expected):
    """``subtract`` handles zeros, negatives and large integers."""
    assert subtract(a, b) == expected


@pytest.mark.parametrize(
    "a, b, expected",
    [
        (0.5, 0.25, 0.75),
        (-1.5, 0.5, -1.0),
        (2.5, 2.5, 5.0),
    ],
)
def test_add_floats(a, b, expected):
    """``add`` works with floating point operands."""
    assert add(a, b) == pytest.approx(expected)


@pytest.mark.parametrize(
    "a, b, expected",
    [
        (0.5, 0.25, 0.25),
        (-1.5, 0.5, -2.0),
        (2.5, 2.5, 0.0),
    ],
)
def test_subtract_floats(a, b, expected):
    """``subtract`` works with floating point operands."""
    assert subtract(a, b) == pytest.approx(expected)


def test_add_is_commutative():
    """Addition yields the same result regardless of operand order."""
    assert add(8, 3) == add(3, 8)


def test_subtract_is_inverse_of_add():
    """Subtracting an addend returns the original value."""
    assert subtract(add(9, 4), 4) == 9
