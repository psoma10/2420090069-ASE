# PRD 06 — Docker Containerization of a Flask Application

## 1. Project Overview
Create a small Python Flask application and containerize it using Docker. Build a Docker image and run the application inside a Docker container accessible through `localhost:5000`.

## 2. Objective
- Create a Flask application.
- Define its dependency in `requirements.txt`.
- Create a Dockerfile.
- Build a Docker image.
- Run the application inside a Docker container.
- Verify the application through a browser.

## 3. Technology Stack
- Python 3.12
- Flask
- Docker Desktop
- Docker CLI

## 4. Project Structure
```text
docker-demo/
├── app.py
├── requirements.txt
└── Dockerfile
```

## 5. Functional Requirements

### FR-01 — Flask Application
`app.py` shall provide a root endpoint:
```python
from flask import Flask

app = Flask(__name__)

@app.route("/")
def home():
    return "Hello from Docker!"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
```

### FR-02 — Dependencies
`requirements.txt`:
```text
Flask
```

### FR-03 — Dockerfile
```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5000

CMD ["python", "app.py"]
```

### FR-04 — Build Image
```bash
docker build -t docker-demo-app .
```

### FR-05 — Run Container
```bash
docker run -d -p 5000:5000 --name docker-demo-container docker-demo-app
```

### FR-06 — Verify Container
```bash
docker ps
```

The container `docker-demo-container` shall appear as running.

### FR-07 — Verify Application
Open:
```text
http://localhost:5000
```

Expected response:
```text
Hello from Docker!
```

### FR-08 — Logs and Cleanup
```bash
docker logs docker-demo-container
docker stop docker-demo-container
docker rm docker-demo-container
```

## 6. Docker Workflow
```text
Source Code
    ↓
Dockerfile
    ↓
docker build
    ↓
Docker Image
    ↓
docker run
    ↓
Docker Container
    ↓
localhost:5000
```

## 7. Acceptance Criteria
- Flask application runs correctly.
- Docker image `docker-demo-app` is created successfully.
- Container `docker-demo-container` starts successfully.
- Port `5000` is mapped from host to container.
- Browser displays `Hello from Docker!`.
- Container logs can be viewed.
- Container can be stopped and removed successfully.

## 8. Deliverables
- `app.py`
- `requirements.txt`
- `Dockerfile`
- Built Docker image
- Running Docker container
