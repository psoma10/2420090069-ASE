# Lab 5 — GitFlow Workflow and GitHub Actions CI

A small Python application built and delivered through a GitFlow-style branch
workflow, with automatic linting and testing on every push via GitHub Actions.

**Repository:** <https://github.com/psoma10/2420090069-ASE.git>

---

## 1. Objective

- Create and manage feature branches.
- Push feature branches to GitHub.
- Merge changes through Pull Requests.
- Demonstrate and resolve a merge conflict.
- Run Flake8 and Pytest automatically through GitHub Actions on every push.

---

## 2. Tech Stack

| Component | Technology |
|---|---|
| Language | Python 3.12 |
| Version control | Git / Git Bash |
| Hosting & PRs | GitHub (`gh` CLI) |
| Testing | Pytest |
| Linting | Flake8 |
| CI | GitHub Actions |

---

## 3. Repository Structure

This lab lives inside a monorepo that holds every lab. The application code sits
in `Lab 5/`, while the CI workflow must live at the **repository root** so that
GitHub Actions can discover it.

```text
2420090069-ASE/
├── .github/
│   └── workflows/
│       └── ci.yml           # CI workflow (repo root — required location)
├── Lab 5/
│   ├── app.py               # add() and subtract()
│   ├── test_app.py          # Pytest unit tests
│   ├── requirements.txt     # pytest, flake8
│   ├── setup.cfg            # Flake8 configuration
│   └── README.md            # This document
└── ...                      # Lab 1 … Lab 7
```

---

## 4. GitFlow Workflow Used

### Branch model

| Branch | Role |
|---|---|
| `main` | Stable branch. Always green. Never committed to directly. |
| `feature/<name>` | Short-lived branch for one unit of work. Merged into `main` via PR. |

```text
main ──────●───────────────────●──────────────►
            \                 /
             ●──●──●─────────●   feature/add-subtract
             commit  push    Pull Request → merge
```

### Flow

```text
Feature Branch → Edit Code → Commit → Push → Pull Request → Merge
      → Conflict? → Resolve → Commit → GitHub Actions → Lint + Test → PASS / FAIL
```

### Commands used

**1. Create the feature branch off `main`.**

```bash
git checkout main
git pull origin main
git checkout -b feature/add-subtract
```

**2. Implement `add()` / `subtract()`, then commit.**

```bash
git add "Lab 5/app.py" "Lab 5/test_app.py"
git commit -m "feat: add add() and subtract() with unit tests"
```

**3. Push the branch and set upstream tracking.**

```bash
git push -u origin feature/add-subtract
```

Pushing immediately triggers the CI workflow, because it listens on `"**"`
(all branches).

**4. Open a Pull Request into `main`.**

```bash
gh pr create \
  --base main \
  --head feature/add-subtract \
  --title "feat: add and subtract operations" \
  --body "Implements add()/subtract() with Pytest coverage and Flake8 clean."
```

**5. Merge the PR once CI reports PASS.**

```bash
gh pr merge --merge --delete-branch
```

**6. Sync the local `main`.**

```bash
git checkout main
git pull origin main
```

---

## 5. Merge Conflict Demonstration

A merge conflict is produced deliberately by having **two feature branches edit
the same line** of `Lab 5/app.py` — the module docstring's first line — in two
different ways.

### Step 1 — Branch A edits the line

```bash
git checkout main
git checkout -b feature/conflict-a
```

Change the first line of the docstring in `Lab 5/app.py` to:

```python
"""Simple arithmetic utilities for the GitFlow lab (branch A edit)."""
```

```bash
git commit -am "docs: reword module docstring (branch A)"
git push -u origin feature/conflict-a
```

### Step 2 — Branch B edits the *same* line, differently

Branch B is cut from `main` **before** A is merged, so both start from the same
base commit:

```bash
git checkout main
git checkout -b feature/conflict-b
```

Change the exact same line to something else:

```python
"""Arithmetic helper module for CI demonstration (branch B edit)."""
```

```bash
git commit -am "docs: reword module docstring (branch B)"
git push -u origin feature/conflict-b
```

### Step 3 — First merge succeeds, second one conflicts

```bash
git checkout main
git merge feature/conflict-a        # fast-forward / clean merge — OK
git merge feature/conflict-b        # CONFLICT
```

Git reports:

```text
Auto-merging Lab 5/app.py
CONFLICT (content): Merge conflict in Lab 5/app.py
Automatic merge failed; fix conflicts and then commit the result.
```

### Step 4 — Inspect the conflict markers

`Lab 5/app.py` now contains both versions, separated by conflict markers:

```python
<<<<<<< HEAD
"""Simple arithmetic utilities for the GitFlow lab (branch A edit).
=======
"""Arithmetic helper module for CI demonstration (branch B edit).
>>>>>>> feature/conflict-b

Exposes two pure functions, ``add`` and ``subtract``, which are exercised by
``test_app.py`` and linted by Flake8 in the CI workflow.
"""
```

Reading the markers:

| Marker | Meaning |
|---|---|
| `<<<<<<< HEAD` | Start of the version already on `main` (from branch A). |
| `=======` | Divider between the two competing versions. |
| `>>>>>>> feature/conflict-b` | End of the incoming version from branch B. |

Confirm which files are conflicted:

```bash
git status
# Unmerged paths:
#   both modified:   Lab 5/app.py
```

### Step 5 — Resolve manually

Open the file, delete **all three** marker lines, and keep a single agreed
wording that merges the intent of both branches:

```python
"""Simple arithmetic application for the GitFlow + GitHub Actions lab.

Exposes two pure functions, ``add`` and ``subtract``, which are exercised by
``test_app.py`` and linted by Flake8 in the CI workflow.
"""
```

### Step 6 — Stage the resolution and commit the merge

```bash
git add "Lab 5/app.py"
git commit -m "merge: resolve docstring conflict between conflict-a and conflict-b"
git push origin main
```

Verify locally before pushing:

```bash
cd "Lab 5"
flake8 .
pytest -q
```

> No conflict markers may survive in the committed file — a stray `<<<<<<<`
> is a syntax error in Python and would fail both Flake8 and Pytest in CI.

---

## 6. Running Locally

```bash
cd "Lab 5"
pip install -r requirements.txt
flake8 .
pytest -q
```

Expected output:

```text
$ flake8 .
$ pytest -q
..                                                          [100%]
2 passed in 0.02s
```

Flake8 printing nothing means zero style violations. Configuration lives in
`setup.cfg`.

---

## 7. Continuous Integration (GitHub Actions)

The workflow is defined at the repository root in
`.github/workflows/ci.yml`.

### Triggers

| Event | Filter | Effect |
|---|---|---|
| `push` | `branches: "**"` | Runs on **every push to every branch**, including all `feature/*` branches. |
| `pull_request` | `branches: main` | Runs on every PR targeting `main`, gating the merge. |

### Job — `lint-and-test`

Runs on `ubuntu-latest` with `working-directory: "Lab 5"`, so every step
executes inside this lab's folder within the monorepo.

| Step | Action / command |
|---|---|
| Checkout code | `actions/checkout@v4` |
| Set up Python | `actions/setup-python@v5` with `python-version: "3.12"` |
| Install dependencies | `python -m pip install --upgrade pip` then `pip install -r requirements.txt` |
| Run Linter | `flake8 .` |
| Run Tests | `pytest` |

```yaml
name: Python CI

on:
  push:
    branches:
      - "**"
  pull_request:
    branches:
      - main

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: "Lab 5"
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

### PASS / FAIL reporting

Steps run in order and the job stops at the first non-zero exit code:

- **PASS** — a green check on the commit and on the PR, shown when both
  `flake8 .` and `pytest` exit `0`.
- **FAIL** — a red cross, when Flake8 reports a style violation or any test
  fails. The PR is blocked from merging cleanly until it goes green.

Results are visible on the repository's **Actions** tab and inline in the
Pull Request's checks section.

---

## 8. Acceptance Criteria

- [x] `main` repository is initialized and pushed to GitHub.
- [x] A feature branch can be created and pushed.
- [x] A Pull Request can be created and merged.
- [x] A merge conflict is demonstrated and successfully resolved.
- [x] `pytest` executes successfully.
- [x] `flake8` executes successfully.
- [x] Every push triggers the GitHub Actions workflow.
- [x] The workflow reports PASS when linting and tests succeed.

---

## 9. Deliverables

| Deliverable | Path |
|---|---|
| Application | `Lab 5/app.py` |
| Tests | `Lab 5/test_app.py` |
| Dependencies | `Lab 5/requirements.txt` |
| Lint config | `Lab 5/setup.cfg` |
| CI workflow | `.github/workflows/ci.yml` |
| Branch / PR / merge history | <https://github.com/psoma10/2420090069-ASE.git> |
