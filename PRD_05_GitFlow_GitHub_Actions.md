# PRD 05 — GitFlow Workflow and GitHub Actions CI

## 1. Project Overview
Implement a small Python application using a GitFlow-style branch workflow, feature branches, Pull Requests, merge-conflict resolution, and GitHub Actions for automatic linting and testing on every push.

## 2. Objective
- Create and manage feature branches.
- Push feature branches to GitHub.
- Merge changes through Pull Requests.
- Demonstrate and resolve a merge conflict.
- Run Flake8 and Pytest automatically through GitHub Actions on every push.

## 3. Technology Stack
- Git / Git Bash
- GitHub
- Python 3.12
- Pytest
- Flake8
- GitHub Actions

## 4. Repository Structure
```text
GitHub-Devops/
├── app.py
├── test_app.py
├── requirements.txt
└── .github/
    └── workflows/
        └── ci.yml
```

## 5. Functional Requirements

### FR-01 — Git Repository
Initialize the project with Git and use `main` as the stable branch.

### FR-02 — Feature Branch
Create feature branches using the `feature/<name>` convention.

Example:
```bash
git checkout -b feature/add-subtract
```

### FR-03 — Feature Implementation
The application shall contain:
```python
def add(a, b):
    return a + b

def subtract(a, b):
    return a - b
```

### FR-04 — Pull Request
Push the feature branch and create a Pull Request with `main` as the base branch.

### FR-05 — Merge Conflict
Create two branches that modify the same line differently and resolve the resulting conflict manually.

### FR-06 — Automated Testing
Create tests for `add()` and `subtract()`:
```python
from app import add, subtract

def test_add():
    assert add(2, 3) == 5

def test_subtract():
    assert subtract(5, 2) == 3
```

### FR-07 — Automated Linting
Use Flake8 to check Python code quality.

`requirements.txt`:
```text
pytest
flake8
```

### FR-08 — Continuous Integration
Create `.github/workflows/ci.yml`:
```yaml
name: Python CI

on:
  push:
    branches:
      - "**"

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install dependencies
        run: |
          python -m pip install --upgrade pip
          pip install -r requirements.txt

      - name: Run Linter
        run: flake8 .

      - name: Run Tests
        run: pytest
```

## 6. Git Workflow
```text
Feature Branch
      ↓
Edit Code
      ↓
Commit
      ↓
Push
      ↓
Pull Request
      ↓
Merge
      ↓
Conflict? → Resolve → Commit
      ↓
GitHub Actions
      ↓
Lint + Test
      ↓
PASS / FAIL
```

## 7. Acceptance Criteria
- `main` repository is initialized and pushed to GitHub.
- A feature branch can be created and pushed.
- A Pull Request can be created and merged.
- A merge conflict is demonstrated and successfully resolved.
- `pytest` executes successfully.
- `flake8` executes successfully.
- Every push triggers the GitHub Actions workflow.
- The workflow reports PASS when linting and tests succeed.

## 8. Deliverables
- `app.py`
- `test_app.py`
- `requirements.txt`
- `.github/workflows/ci.yml`
- GitHub repository containing the branch/PR/merge history
