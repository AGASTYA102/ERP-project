# 🏭 Manufacturing ERP System

A production-ready workflow-based ERP system designed for small manufacturing businesses to track orders from creation to delivery.

---

## 🚀 Features

* 🔐 Role-based access (GM, Designer, Accounts, Production, Purchase)
* 📦 Order lifecycle tracking (Design → Purchase → Production → Accounts)
* 🧾 Invoice generation
* 📊 Audit logs for every status transition
* 🔄 Concurrency-safe operations (Optimistic + Pessimistic locking)
* 📁 Secure file uploads (validated, sanitized, size-limited)
* ⚙️ Environment-based configuration (Dev + Production)

---

## 🏗️ Tech Stack

* Java 17
* Spring Boot
* Spring Security
* JPA / Hibernate
* PostgreSQL / MySQL
* Thymeleaf

---

## 🔐 Security Highlights

* BCrypt password hashing
* CSRF protection enabled
* Role-based authorization using `@PreAuthorize`
* File upload hardening (MIME validation, UUID naming, path sanitization)

---

## ⚙️ Configuration

Uses environment variables:

DB_URL=...
DB_USER=...
DB_PASS=...
UPLOAD_DIR=...

Refer `.env.example` for setup.

---

## ▶️ Running Locally

```bash
mvn clean package
java -jar target/*.jar
```

---

## 🌐 Deployment

The application is deployment-ready and can be hosted on:

* Render
* Railway
* AWS

---

## 📌 Workflow

1. Create Order
2. Submit Design
3. Process Purchase
4. Production
5. Accounts / Invoice

---

## 🧪 Testing

* Integration tests for full workflow
* Security access tests
* File upload security tests
* Concurrency validation

---

## 📄 Documentation

* walkthrough.md → Full system audit & verification report

---

## 💼 Use Case

Designed for small manufacturing businesses to replace Excel/WhatsApp-based order tracking with a structured workflow system.

---

## 🚀 Status

✅ Production Ready
