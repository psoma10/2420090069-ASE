"""Minimal Flask application for the Docker containerization lab.

Serves a greeting on the root route and a lightweight JSON health check that
container orchestrators can poll. The app binds to ``0.0.0.0`` so it is
reachable from outside the container when port 5000 is published.
"""

from flask import Flask, jsonify

app = Flask(__name__)


@app.route("/")
def home():
    """Return the greeting shown when the container is reachable.

    Returns:
        The plain-text message ``Hello from Docker!``.
    """
    return "Hello from Docker!"


@app.route("/health")
def health():
    """Report application liveness for container health checks.

    Returns:
        A Flask JSON response with the body ``{"status": "ok"}``.
    """
    return jsonify(status="ok")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
