# Lab 6 — Docker Containerization of a Flask Application

A minimal Python Flask application packaged as a Docker image and executed
inside a Docker container, reachable from the host at `http://localhost:5000`.

**Repository:** <https://github.com/psoma10/2420090069-ASE.git>

---

## 1. Objective

- Create a Flask application exposing a root endpoint.
- Declare its dependency in `requirements.txt`.
- Write a `Dockerfile` that builds a runnable image.
- Build the image `docker-demo-app`.
- Run the container `docker-demo-container` with port `5000` published.
- Verify the response in a browser, read the logs, then stop and remove the
  container.

---

## 2. Tech Stack

| Component | Technology |
|---|---|
| Language | Python 3.12 (`python:3.12-slim` base image) |
| Web framework | Flask |
| Container runtime | Docker Desktop / Docker Engine |
| Tooling | Docker CLI, Docker Compose v2 |
| Testing | Pytest (Flask test client) |
| Linting | Flake8 |
| CI | GitHub Actions |

---

## 3. Project Structure

This lab lives inside the monorepo that holds every lab. The application code
sits in `Lab 6/`; the CI workflow stays at the **repository root** because that
is the only place GitHub Actions discovers workflows.

```text
2420090069-ASE/
├── .github/
│   └── workflows/
│       └── ci.yml               # Lint + test matrix, plus the Docker build job
├── Lab 6/
│   ├── app.py                   # Flask app: GET / and GET /health
│   ├── requirements.txt         # Flask, pytest, flake8
│   ├── Dockerfile               # Image definition
│   ├── .dockerignore            # Files excluded from the build context
│   ├── docker-compose.yml       # One-command build + run
│   ├── test_app.py              # Pytest suite against the Flask test client
│   ├── setup.cfg                # Flake8 configuration
│   └── README.md                # This document
└── ...                          # Lab 1 … Lab 7
```

---

## 4. Docker Workflow

```text
Source Code
    ↓
Dockerfile
    ↓
docker build
    ↓
Docker Image   (docker-demo-app)
    ↓
docker run
    ↓
Docker Container   (docker-demo-container)
    ↓
localhost:5000
```

### Key concepts

| Term | Meaning in this lab |
|---|---|
| **Image** | Immutable, layered filesystem + metadata built from the `Dockerfile`. Named `docker-demo-app`. |
| **Container** | A running instance of that image with its own process namespace. Named `docker-demo-container`. |
| **Layer** | One `Dockerfile` instruction. Cached and reused when its inputs are unchanged. |
| **Build context** | The directory sent to the daemon (`Lab 6/`), filtered by `.dockerignore`. |
| **Port publishing** | `-p 5000:5000` maps host port 5000 → container port 5000. |

### Why `requirements.txt` is copied before `app.py`

Docker caches each layer. Dependencies change far less often than application
code, so installing them in an earlier layer means an edit to `app.py`
invalidates only the final layers — `pip install` is not re-run.

### Why `host="0.0.0.0"`

Binding to `127.0.0.1` would only accept connections from inside the
container's own network namespace, so the published port would reach nothing.
`0.0.0.0` binds every interface, which is what makes `-p 5000:5000` work.

---

## 5. Build and Run

All commands are run from inside the `Lab 6/` directory.

```bash
cd "Lab 6"
```

### Step 1 — Build the image

```bash
docker build -t docker-demo-app .
```

Expected tail of the output:

```text
=> naming to docker.io/library/docker-demo-app                    done
```

Confirm the image exists:

```bash
docker images docker-demo-app
```

### Step 2 — Run the container

```bash
docker run -d -p 5000:5000 --name docker-demo-container docker-demo-app
```

`-d` detaches (runs in the background) and prints the container ID.

### Step 3 — Verify the container is running

```bash
docker ps
```

`docker-demo-container` appears with status `Up` and ports
`0.0.0.0:5000->5000/tcp`.

### Step 4 — Verify the application

Open <http://localhost:5000> in a browser, or from the terminal:

```bash
curl http://localhost:5000
```

Expected response:

```text
Hello from Docker!
```

The health endpoint returns JSON:

```bash
curl http://localhost:5000/health
```

```json
{"status": "ok"}
```

### Step 5 — Inspect the logs

```bash
docker logs docker-demo-container
```

### Step 6 — Stop and remove the container

```bash
docker stop docker-demo-container
docker rm docker-demo-container
```

Optionally remove the image as well:

```bash
docker rmi docker-demo-app
```

---

## 6. Docker Compose (equivalent one-command flow)

`docker-compose.yml` wraps the build and run steps above:

```bash
docker compose up -d --build   # build image + start container
docker compose logs -f         # follow logs
docker compose down            # stop and remove the container
```

It produces the same image name and container name as the manual commands, so
the two workflows are interchangeable — do not run both at once, since the
container name `docker-demo-container` must be unique.

---

## 7. Running the Tests Locally (without Docker)

```bash
cd "Lab 6"
python -m venv .venv
source .venv/Scripts/activate      # Git Bash on Windows
pip install -r requirements.txt
flake8 .
pytest
```

The tests exercise the Flask **test client**, so no server and no container are
needed to run them.

---

## 8. Continuous Integration

`.github/workflows/ci.yml` runs on every push and pull request:

| Job | What it does |
|---|---|
| `lint-and-test` | Matrix over `Lab 5` and `Lab 6`: installs dependencies, runs Flake8, runs Pytest. |
| `docker-build` | Builds `docker-demo-app` from `Lab 6/`, starts the container, polls `localhost:5000`, asserts the body is `Hello from Docker!`, verifies `/health` returns `{"status": "ok"}`, prints logs, then stops and removes the container. |

This means the acceptance criteria below are re-verified automatically on
every commit, not only on the machine where the lab was first performed.

---

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `error during connect ... dockerDesktopLinuxEngine` | Docker daemon is not running. | Start Docker Desktop and wait for the whale icon to settle. |
| `port is already allocated` | Something else holds host port 5000. | `docker rm -f docker-demo-container`, or publish another port: `-p 5001:5000`. |
| `The container name "/docker-demo-container" is already in use` | A stopped container of that name still exists. | `docker rm docker-demo-container`. |
| Browser shows `ERR_EMPTY_RESPONSE` | App bound to `127.0.0.1` inside the container. | Keep `host="0.0.0.0"` in `app.py`. |
| Code changes do not appear | The image was not rebuilt. | Re-run `docker build -t docker-demo-app .`. |

---

## 10. Acceptance Criteria

- [x] Flask application runs correctly.
- [x] Docker image `docker-demo-app` is created successfully.
- [x] Container `docker-demo-container` starts successfully.
- [x] Port `5000` is mapped from host to container.
- [x] Browser displays `Hello from Docker!`.
- [x] Container logs can be viewed.
- [x] Container can be stopped and removed successfully.

---

## 11. Deliverables

| Deliverable | Location |
|---|---|
| Flask application | `Lab 6/app.py` |
| Dependencies | `Lab 6/requirements.txt` |
| Dockerfile | `Lab 6/Dockerfile` |
| Build context filter | `Lab 6/.dockerignore` |
| Compose definition | `Lab 6/docker-compose.yml` |
| Unit tests | `Lab 6/test_app.py` |
| Built image | `docker-demo-app` |
| Running container | `docker-demo-container` |
