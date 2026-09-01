"""Calculator utilities demonstrating branch conflicts in the GitFlow lab.

Exposes two pure functions, ``add`` and ``subtract``, which are exercised by
``test_app.py`` and linted by Flake8 in the CI workflow.
"""

from typing import Union

Number = Union[int, float]


def add(a: Number, b: Number) -> Number:
    """Return the sum of two numbers.

    Args:
        a: The first operand.
        b: The second operand.

    Returns:
        The result of ``a + b``.
    """
    return a + b


def subtract(a: Number, b: Number) -> Number:
    """Return the difference of two numbers.

    Args:
        a: The number to subtract from.
        b: The number to subtract.

    Returns:
        The result of ``a - b``.
    """
    return a - b
