# Meridian — Bank Management System (multi-bank banking + UPI-style payments)

Java 17 · Spring Boot 3 · MySQL 8 · React 18 + TypeScript · Java Swing admin console

## Folder map (what goes where)
```
meridian/
├── backend/            Java Spring Boot project  → open in IntelliJ / Eclipse / VS Code
│   └── src/main/resources/application.properties   ← put your MySQL password here
├── frontend/           React + TypeScript web app
├── database/
│   ├── 01_schema.sql           full DDL (optional – backend also creates tables)
│   ├── 02_advanced_objects.sql triggers, procedures, function (import AFTER first backend start)
│   ├── 03_demo_queries.sql     queries to run live in Workbench
│   └── DATABASE_DESIGN.md      ER diagram + data dictionary  ← for Review-1
└── desktop-admin/      Java Swing admin console (optional)
```

## Install once
JDK 17+ · Maven 3.9+ · Node.js 18+ · MySQL 8 (running on 3306)

## Run
1. **Backend** — edit the MySQL password in `backend/src/main/resources/application.properties`, then
   `cd backend` → `mvn spring-boot:run` (creates DB `meridian`, tables, views, banks and staff logins).
   IntelliJ: install the Lombok plugin and enable annotation processing.
2. **Frontend** — `cd frontend` → `npm install` → `npm run dev` → open **http://localhost:5173**
3. **Database import (optional, for the demo):** MySQL Workbench → open `database/02_advanced_objects.sql` → Run.
4. **Desktop admin (optional):** `cd desktop-admin` → `mvn compile exec:java`

## Logins (staff — password for all: `Meridian@123`)
| Role | Email | Can do |
|---|---|---|
| Admin | admin@meridian.bank | users, staff, unlock, DB explorer, everything |
| Manager | manager@meridian.bank | approve KYC, freeze/close accounts, bank position |
| Teller | teller@meridian.bank | cash deposit / withdrawal at counter |
| Auditor | auditor@meridian.bank | audit trail, all transactions, DB explorer |

**There is no fake data.** Customers, balances and transactions are created only through the real flows below.

## Demo flow (≈6 minutes)
1. **Register** two customers (e.g. 9876500001 and 9876500002). An OTP is "sent": click **Open simulated inbox** and type the code.
2. Each customer: **Profile → KYC** → submit (Aadhaar = any 12 digits, PAN like `ABCDE1234F`).
3. Sign in as **manager** → *KYC queue* → Approve both.
4. Each customer: **Banks → Add bank account** → choose a bank (use *different banks*), branch, set a PIN (not 1234/0000).
5. Sign in as **teller** → find the account number (shown in the customer's Banks page) → **Cash deposit** ₹50,000.
6. Customer 1: **Pay** → type customer 2's *mobile number* → the real name + bank appears → amount → PIN → receipt.
   Also try: **Receive** tab (QR), **Scan QR** (upload a screenshot of the QR), Bank-account + IFSC transfer, **Requests**.
7. Wrong PIN 3 times → PIN locks for 30 min → **Forgot PIN** with OTP. Wrong password 5 times → account lock → admin **Unlock**.
8. Sign in as **auditor/admin** → **Database**: tables with live rows, ER diagram, and the SQL console (sample queries built in).

## About "random numbers like PhonePe"
Inside Meridian, searching any mobile number or UPI ID finds the real registered user and shows their actual name and bank (no fake lookup). A random number that is **not registered in your database** correctly returns "not found" — the national UPI directory (NPCI) is not accessible to a college project. Payments to real FamPay/GPay/PhonePe users are not possible without a licensed bank/PSP.

## Security features
BCrypt password hashing · JWT · OTP (5-min expiry, 5 attempts) for sign-up, login, password reset and PIN reset · per-bank-account PIN with 3-attempt lockout · weak-PIN rejection · account lock after 5 failed sign-ins · KYC gate · role-based access (5 roles) · daily (₹2,00,000) and per-transaction (₹1,00,000) limits · row-level locking on money movement · full audit trail.

## Troubleshooting
* *Access denied for user 'root'* → fix password in `application.properties`.
* *Port 8080 busy* → change `server.port` and the proxy in `frontend/vite.config.ts`.
* *Camera blocked* → works on `localhost`/HTTPS only; use **Upload QR image**.
* *OTP not arriving* → there is no real SMS gateway; open `/inbox` (the simulated inbox) or watch the backend console.
