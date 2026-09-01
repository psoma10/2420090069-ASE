# Lab 1 — E-Commerce System (Agile Software Development)

Implementation of **PRD 01 — E-Commerce System using Agile Software Development**.

A Django web application supporting customer authentication, product browsing and
search, cart management, ordering and payment, order tracking, and administration.

---

## 1. Technology Stack

| Layer | Choice |
|---|---|
| Language | Python 3.11 |
| Backend framework | Django 5.0.6 |
| Frontend | HTML templates, CSS, minimal JavaScript |
| Database | PostgreSQL 16 |
| Version control | Git / GitHub |

The PRD allowed MySQL or PostgreSQL; PostgreSQL was chosen.

---

## 2. Project Structure

```
Lab 1/
├── config/                 Project settings, root URLs, WSGI/ASGI
├── accounts/               US-01  Authentication and customer profiles
├── catalogue/              US-02, US-06  Products, categories, search
│   └── management/commands/seed_catalogue.py   Sample data loader
├── cart/                   US-03  Shopping cart
├── orders/                 US-04, US-05  Orders, payment, tracking
├── templates/              HTML templates
├── static/css/style.css    Stylesheet
├── requirements.txt
├── .env.example            Configuration template
└── manage.py
```

Business rules live in `services.py` modules (`cart/services.py`,
`orders/services.py`) rather than in views, so they can be unit tested directly
and reused by any future interface.

---

## 3. Setup

### 3.1 Prerequisites

- Python 3.11+
- A running PostgreSQL 16 server

### 3.2 Install

```bash
cd "Lab 1"
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # macOS / Linux
pip install -r requirements.txt
```

### 3.3 Create the database

```sql
CREATE DATABASE lab1_ecommerce;
CREATE ROLE lab1_user LOGIN PASSWORD 'choose-a-password';
ALTER DATABASE lab1_ecommerce OWNER TO lab1_user;
ALTER ROLE lab1_user CREATEDB;   -- required for the test database
```

### 3.4 Configure

```bash
cp .env.example .env
```

Edit `.env` and set `SECRET_KEY`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
`DB_HOST`, `DB_PORT`. The application refuses to start if `SECRET_KEY` or
`DB_PASSWORD` is missing, rather than falling back to an insecure default.

`.env` is git-ignored and must never be committed.

### 3.5 Migrate, seed and run

```bash
python manage.py migrate
python manage.py seed_catalogue        # 4 categories, 15 products
python manage.py createsuperuser       # for the admin module
python manage.py runserver 8010
```

Open <http://127.0.0.1:8010/>. The admin module is at `/admin/`.

> Port 8000 is used here only as an example; choose any free port.

---

## 4. User Stories and Where They Are Implemented

| Story | Requirement | Implementation |
|---|---|---|
| US-01 | Register / log in securely | `accounts/views.py`, `accounts/forms.py`, Django auth |
| US-02 | Browse and search products | `catalogue/views.py` (`product_list`, `product_detail`) |
| US-03 | Add, update, remove cart items | `cart/services.py`, `cart/views.py` |
| US-04 | Place order and pay | `orders/services.py` (`create_order_from_cart`, `process_payment`) |
| US-05 | Track order status | `orders/views.py` (`order_tracking`), `OrderStatusUpdate` |
| US-06 | Manage products | `catalogue/admin.py`, Django admin |

### Functional requirements

| FR | Requirement | Status |
|---|---|---|
| FR-01 | Register, log in, log out | Implemented |
| FR-02 | Display products with name, price, availability | Implemented |
| FR-03 | Search and browse | Implemented |
| FR-04 | Add / update quantity / remove | Implemented |
| FR-05 | Create order from cart | Implemented |
| FR-06 | Payment flow | Implemented (simulated gateway) |
| FR-07 | Display order status | Implemented |
| FR-08 | Administer products, customers, orders | Implemented |

---

## 5. Data Entities

All nine entities named in PRD section 9 are modelled:

| Entity | Model | Notes |
|---|---|---|
| User | `accounts.User` | Extends `AbstractUser`; administrators flagged by `is_staff` |
| Product | `catalogue.Product` | Name, price, stock, category, active flag |
| Category | `catalogue.Category` | Slug-addressed grouping |
| Cart | `cart.Cart` | One active cart per customer (DB constraint) |
| CartItem | `cart.CartItem` | Unique per (cart, product); quantity ≥ 1 (DB constraint) |
| Order | `orders.Order` | UUID reference, six-state status |
| OrderItem | `orders.OrderItem` | Stores `product_name` and `unit_price` at time of purchase |
| Payment | `orders.Payment` | One per order, generated transaction id |
| Shipment | `orders.Shipment` | Tracking number, dispatch and delivery timestamps |

`orders.OrderStatusUpdate` is an additional append-only history table backing the
tracking timeline (FR-07).

---

## 6. Agile Increments

Agile was selected per PRD section 7: the system was built as small functional
increments, each usable on its own.

| Increment | Delivered | Usable outcome |
|---|---|---|
| 1 | Project setup, database configuration | Application starts |
| 2 | Authentication (US-01) | Customers can register and log in |
| 3 | Catalogue and search (US-02) | Customers can find products |
| 4 | Shopping cart (US-03) | Customers can assemble a purchase |
| 5 | Order creation (US-04) | Carts become orders; stock is reserved |
| 6 | Payment (US-04) | Orders can be paid for and confirmed |
| 7 | Order tracking (US-05) | Customers can see delivery status |
| 8 | Administration (US-06) | Staff can manage the catalogue and orders |

Each increment was integrated and verified before the next began, matching the
implementation plan in PRD section 10.

---

## 7. Testing

```bash
python manage.py test
```

**88 tests, all passing. 91% statement coverage.**

```bash
pip install coverage
coverage run --source='.' \
  --omit='*/migrations/*,*/.venv/*,manage.py,config/wsgi.py,config/asgi.py,*/tests.py,*/tests_views.py' \
  manage.py test
coverage report
```

| Module | Coverage |
|---|---|
| `orders/services.py` | 100% |
| `orders/forms.py` | 100% |
| `cart/views.py` | 98% |
| `catalogue/views.py` | 97% |
| `catalogue/models.py` | 96% |
| `orders/models.py` | 95% |
| `cart/services.py` | 95% |
| `orders/views.py` | 93% |
| **Total** | **91%** |

Tests cover, among other cases:

- password hashing, duplicate-email rejection, login and logout
- search matching name, description and category; inactive products hidden
- cart quantity validation against stock, including repeated additions
- stock decrement on order, and restoration on cancellation
- prices frozen at time of purchase, unaffected by later catalogue changes
- transactional rollback when stock disappears mid-checkout
- payment success, failure, and double-payment rejection
- one customer being unable to read or pay for another customer's order

The running application was additionally verified end to end over HTTP:
registration through browsing, cart, checkout, payment, and tracking.

---

## 8. Design Notes

**Prices are frozen at purchase.** `OrderItem` copies `product_name` and
`unit_price` when the order is created, so a later price change never alters the
value of a historical order.

**Checkout is transactional.** `create_order_from_cart` runs inside a single
transaction and locks the affected product rows with `select_for_update()`. If
any line cannot be supplied, nothing is written and no stock moves — two
concurrent checkouts cannot oversell the same item.

**Orders are scoped to their owner.** Every order view looks the order up by
reference *and* user, so guessing a reference does not expose another
customer's data. This is covered by tests.

**Payment is simulated.** Per PRD section 6 the requirement is a payment *flow*.
No external gateway is contacted and no card details are collected or stored;
`Payment` records only the method, outcome, amount and a generated transaction
id. Cash on delivery always succeeds; other methods succeed with high
probability so the failure path remains demonstrable. Swapping in a real
gateway means changing `process_payment` in `orders/services.py` and nothing
else.

**Secrets come from the environment.** `SECRET_KEY` and `DB_PASSWORD` are read
from `.env` with no insecure fallback. When `DEBUG` is off, secure cookies,
HSTS, and clickjacking protection are enabled automatically.

---

## 9. Definition of Done

Assessed against PRD section 11:

| Criterion | Status |
|---|---|
| Required functionality implemented | All 8 functional requirements |
| Data stored correctly | Verified against PostgreSQL |
| Relevant validation present | Form, model, and database-level constraints |
| Feature integrates with existing modules | Full journey verified end to end |
| Basic functional testing passes | 88 tests passing, 91% coverage |
| Code committed to version control | Committed to the repository |

---

## 10. Deliverables

All items in PRD section 12 are present: Django source code, database
schema/models with migrations, frontend pages, authentication module, product
catalogue, cart and order modules, payment integration, order tracking, admin
module, and the Git repository.
