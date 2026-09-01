# PRD 01 — E-Commerce System using Agile Software Development

## 1. Project Overview
Design and implement a real-world E-Commerce System using the Agile software development model. The system shall support customer authentication, product browsing/search, cart management, ordering/payment, order tracking, and administrative management.

## 2. Objective
- Identify major requirements using user stories.
- Apply the Agile process model.
- Develop the system incrementally.
- Use a suitable programming language and web framework.
- Produce a maintainable and scalable web application.

## 3. Case Study
The E-Commerce System allows customers to browse products, search for products, add products to a cart, place orders, make payments, and track orders. Administrators manage products, orders and customers.

## 4. Technology Stack
- Programming Language: Python
- Backend Framework: Django
- Frontend: HTML, CSS, JavaScript
- Database: MySQL or PostgreSQL
- Version Control: Git/GitHub

## 5. Users
### Customer
- Register/login
- Browse and search products
- Manage shopping cart
- Place orders and make payments
- Track orders

### Administrator
- Manage products
- Manage customers
- Manage orders
- Monitor system activity

## 6. User Stories

### US-01 — Customer Registration/Login
**As a customer, I want to register and log in securely, so that I can access my account and place orders.**

### US-02 — Product Browse/Search
**As a customer, I want to browse and search products, so that I can find products I want to purchase.**

### US-03 — Shopping Cart
**As a customer, I want to add, update and remove products from my cart, so that I can manage my intended purchases.**

### US-04 — Order and Payment
**As a customer, I want to place an order and complete payment, so that I can purchase selected products.**

### US-05 — Order Tracking
**As a customer, I want to track my order, so that I can know its current delivery status.**

### US-06 — Product Management
**As an administrator, I want to add, update and remove products, so that the product catalogue remains accurate.**

## 7. Agile Approach
Agile is selected because it combines iterative and incremental development and emphasizes adaptability and customer satisfaction.

The system is divided into small functional increments. Requirements can be refined as development progresses, and each increment should provide a usable part of the application.

## 8. Functional Requirements

### FR-01 — Authentication
The system shall allow customers to register, log in and log out.

### FR-02 — Product Catalogue
The system shall display products with relevant information such as name, price and availability.

### FR-03 — Search
The system shall allow customers to search and browse products.

### FR-04 — Cart
The system shall allow customers to add, update quantity and remove products.

### FR-05 — Orders
The system shall create an order from the customer's cart and maintain order information.

### FR-06 — Payment
The system shall provide a payment flow for completing an order.

### FR-07 — Tracking
The system shall display the current status of an order.

### FR-08 — Administration
The administrator shall be able to manage products, customers and orders.

## 9. Suggested Data Entities
- User
- Product
- Category
- Cart
- CartItem
- Order
- OrderItem
- Payment
- Shipment/Tracking

## 10. Implementation Plan
1. Create Django project and database configuration.
2. Implement authentication.
3. Implement product catalogue and search.
4. Implement shopping cart.
5. Implement order creation.
6. Integrate the selected payment mechanism.
7. Implement order tracking.
8. Implement administrator functions.
9. Connect frontend templates with backend functionality.
10. Integrate and verify all modules.

## 11. Definition of Done
A feature is considered complete when:
- Required functionality is implemented.
- Data is stored correctly.
- Relevant validation is present.
- The feature integrates with existing modules.
- Basic functional testing passes.
- Code is committed to version control.

## 12. Deliverables
- Django source code
- Database schema/models
- Frontend pages
- Authentication module
- Product catalogue
- Cart and order modules
- Payment integration
- Order tracking
- Admin module
- Git repository
