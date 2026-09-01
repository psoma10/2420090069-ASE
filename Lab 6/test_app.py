"""Pytest suite covering the Flask endpoints exposed by ``app.py``."""

import pytest

from app import app as flask_app


@pytest.fixture
def client():
    """Provide a Flask test client bound to the application under test."""
    flask_app.config["TESTING"] = True
    with flask_app.test_client() as test_client:
        yield test_client


def test_root_status_code(client):
    """The root endpoint responds with HTTP 200."""
    response = client.get("/")
    assert response.status_code == 200


def test_root_body(client):
    """The root endpoint returns the expected greeting."""
    response = client.get("/")
    assert response.get_data(as_text=True) == "Hello from Docker!"


def test_health_status_code(client):
    """The health endpoint responds with HTTP 200."""
    response = client.get("/health")
    assert response.status_code == 200


def test_health_payload(client):
    """The health endpoint reports an ``ok`` status as JSON."""
    response = client.get("/health")
    assert response.get_json() == {"status": "ok"}


@pytest.mark.parametrize(
    "path",
    [
        "/missing",
        "/health/extra",
        "/does-not-exist",
    ],
)
def test_unknown_route_returns_404(client, path):
    """Unregistered routes return HTTP 404."""
    response = client.get(path)
    assert response.status_code == 404
