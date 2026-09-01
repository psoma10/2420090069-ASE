# ASE Lab — 2420090069

Advanced Software Engineering laboratory work. Each lab implements one PRD,
covering software process models, project management tooling, version control
workflows, containerization, and orchestration.

**Repository:** <https://github.com/psoma10/2420090069-ASE>

---

## Labs

| # | Lab | Topic | Deliverable |
|---|---|---|---|
| 1 | [Lab 1](Lab%201/) | E-Commerce System — **Agile** model | Django application |
| 2 | [Lab 2](Lab%202/) | College ERP — **Iterative** model | Spring Boot application |
| 3 | Lab 3 | Scrum project planning | [Jira board ↗](https://90069ase.atlassian.net/jira/software/projects/SCRUM/summary?atlOrigin=eyJpIjoiYjVmMDc4ZjQwMmY1NGEwNWE0ZTViZDQ2NzBkZThkODEiLCJwIjoiaiJ9) |
| 4 | Lab 4 | College Event Management — task board | [Trello board ↗](https://trello.com/b/EdGhGuIV/college-event-management-system) |
| 5 | [Lab 5](Lab%205/) | GitFlow workflow and GitHub Actions CI | Branches, PRs, CI pipeline |
| 6 | [Lab 6](Lab%206/) | Docker containerization | Containerized Flask app |
| 7 | [Lab 7](Lab%207/) | Minikube Kubernetes deployment | Nginx pod on Minikube |

Labs 3 and 4 are planning exercises whose deliverables are hosted in external
project-management tools, so their directories hold no source code.

---

## Requirement Documents

| PRD | Lab | Title |
|---|---|---|
| [PRD 01](PRD_01_ECommerce_Agile.md) | 1 | E-Commerce System using Agile Software Development |
| [PRD 02](PRD_02_College_ERP_Iterative.md) | 2 | College ERP System using Iterative Model |
| [PRD 05](PRD_05_GitFlow_GitHub_Actions.md) | 5 | GitFlow Workflow and GitHub Actions CI |
| [PRD 06](PRD_06_Docker_Containerization.md) | 6 | Docker Containerization of a Flask Application |
| [PRD 07](PRD_07_Minikube_Nginx.md) | 7 | Minikube Kubernetes Nginx Deployment |

---

## Lab 1 — E-Commerce System (Agile)

Django e-commerce application covering customer authentication, product
browsing and search, cart management, ordering and payment, order tracking,
and administration.

- **Stack:** Python 3.11, Django 5.0.6, PostgreSQL 16, HTML/CSS
- **Tests:** 88 passing, 91% statement coverage
- **Setup and documentation:** [Lab 1/README.md](Lab%201/README.md)

```bash
cd "Lab 1"
python -m venv .venv && .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env          # then fill in SECRET_KEY and database credentials
python manage.py migrate
python manage.py seed_catalogue
python manage.py runserver 8010
```

---

## Lab 2 — College ERP (Iterative)

Spring Boot implementation of the College ERP system, built with the iterative
model. See [Lab 2](Lab%202/) for setup instructions.

---

## Lab 3 — Scrum Project Planning

Sprint planning, backlog, and issue tracking carried out in Jira.

**Board:** <https://90069ase.atlassian.net/jira/software/projects/SCRUM/summary?atlOrigin=eyJpIjoiYjVmMDc4ZjQwMmY1NGEwNWE0ZTViZDQ2NzBkZThkODEiLCJwIjoiaiJ9>

---

## Lab 4 — College Event Management System

Task board for the College Event Management System, maintained in Trello.

**Board:** <https://trello.com/b/EdGhGuIV/college-event-management-system>

---

## Lab 5 — GitFlow and GitHub Actions

Branching strategy, pull requests, merge-conflict resolution, and a CI pipeline
defined in `.github/workflows/ci.yml`. The branch and PR history in this
repository is itself part of the deliverable.

---

## Lab 6 — Docker Containerization

Flask application packaged as a Docker image (`docker-demo-app`) and run as a
container (`docker-demo-container`) publishing port 5000, so the app answers on
<http://localhost:5000>. Includes a Compose file, a Pytest suite over the Flask
test client, and Flake8 configuration. See [Lab 6](Lab%206/).

---

## Lab 7 — Minikube Kubernetes Deployment

Nginx deployed as a pod on a local Minikube cluster. See [Lab 7](Lab%207/).

---

## Repository Layout

```
ASE-Lab/
├── README.md               This file
├── PRD_*.md                Requirement documents
├── .github/workflows/      CI pipeline (Lab 5)
├── Lab 1/                  Django e-commerce application
├── Lab 2/                  Spring Boot College ERP
├── Lab 3/                  Jira planning (external)
├── Lab 4/                  Trello board (external)
├── Lab 5/                  GitFlow exercise
├── Lab 6/                  Docker containerization
└── Lab 7/                  Minikube deployment
```
