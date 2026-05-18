# Java Bank Management System

A fully-featured, console-based banking application built in **pure Java** with no external frameworks or libraries. Designed to demonstrate deep mastery of Object-Oriented Programming, software design patterns, and core Java — making it a strong portfolio piece for any Java developer role.

---

## Overview

This project simulates a real-world banking system where customers can manage accounts through an ATM interface and administrators can perform bank-wide operations. All data is persisted across sessions using Java Serialization, so the system behaves like a real application — your data survives restarts.

The codebase is intentionally framework-free. Every feature — from file persistence to PIN hashing to concurrent transaction safety — is implemented from scratch using the Java standard library, demonstrating that the developer understands the fundamentals that frameworks abstract away.

---

## Features

### Customer (ATM) Features
- Secure login with PIN hashing (SHA-256) and account lockout after 3 failed attempts
- Check balance across multiple linked accounts
- Deposit and withdraw with real-time balance updates
- Transfer funds between accounts with automatic rollback on failure
- View full transaction statement with history
- Change PIN (with old PIN verification)

### Account Types
| Account | Daily Limit | Interest | Special Feature |
|---|---|---|---|
| Savings | $3,000 | 3.5% p.a. | Minimum balance enforced |
| Checking | $5,000 | None | Overdraft up to configured limit |
| Loan | N/A | Variable rate | EMI calculation, repayment tracking |

### Admin Features
- Create, freeze, unfreeze, and delete accounts
- Apply monthly interest to all eligible accounts in one operation
- Unlock user accounts locked due to failed PIN attempts
- Generate bank-wide reports: total assets, accounts by type, top balances, frozen accounts
- View all users and all accounts with formatted summaries

### System Features
- Full data persistence — all accounts, users, and transaction history survive application restarts
- Thread-safe balance operations using `synchronized` methods
- Real-time event notifications (low balance alerts, large transaction notices, freeze alerts)
- Custom exceptions with contextual data for every business rule violation

---

## Architecture

The project follows a clean **layered architecture** where each layer has a single responsibility and dependencies only flow downward.

```
┌─────────────────────────────────────────┐
│           main (Controllers)            │  ← Handles user input/output only
│     BankApplication · ATMController     │
│          AdminController                │
├─────────────────────────────────────────┤
│              services                   │  ← Business logic only
│  AuthService · TransactionService       │
│   AdminService · ATMService             │
├─────────────────────────────────────────┤
│             repository                  │  ← Data access only
│  Repository<T,ID> · AccountRepository  │
│        UserRepository                   │
├──────────────┬──────────────────────────┤
│    models    │        patterns          │  ← Domain objects + design patterns
│   Account   │   AccountFactory         │
│  (abstract) │   BankEventListener      │
│  Savings /  │   NotificationService    │
│  Checking / │                          │
│    Loan     │                          │
├──────────────┴──────────────────────────┤
│    interfaces · exceptions · enums      │  ← Foundation (no dependencies)
│    utils (FileManager, PinHasher, ...)  │
└─────────────────────────────────────────┘
```

### Package Breakdown

| Package | Responsibility |
|---|---|
| `enums` | Type-safe constants: `AccountType`, `TransactionType`, `UserRole`, `TransactionStatus` |
| `exceptions` | Custom checked exceptions with contextual fields for every business rule |
| `interfaces` | Behavioral contracts: `Auditable`, `InterestBearing`, `Transferable` |
| `models` | Core domain objects: `Account` (abstract), `SavingsAccount`, `CheckingAccount`, `LoanAccount`, `User`, `Transaction` |
| `patterns` | Design patterns: `AccountFactory` (Factory), `BankEventListener` + `NotificationService` (Observer) |
| `repository` | Generic `Repository<T,ID>` interface + file-backed implementations |
| `services` | Business logic: `AuthService`, `TransactionService`, `AdminService`, `ATMService` |
| `utils` | Infrastructure: `FileManager` (serialization I/O), `PinHasher` (SHA-256), `ConsoleHelper` (Scanner wrapper) |

---

## OOP Concepts Demonstrated

### Four Pillars
- **Encapsulation** — `balance` is private and can only be modified through `deposit()` / `withdraw()`, which enforce all business rules. `Transaction` is fully immutable (all fields `final`, no setters).
- **Abstraction** — `Account` is abstract with three abstract methods (`getDailyWithdrawalLimit`, `calculateInterest`, `getAccountSummary`) that each subclass implements according to its own rules.
- **Inheritance** — `SavingsAccount`, `CheckingAccount`, and `LoanAccount` each extend `Account`, inheriting all shared behaviour and overriding only what differs. `LoanAccount` blocks `withdraw()` entirely using `UnsupportedOperationException`.
- **Polymorphism** — `TransactionService` calls `account.withdraw()` on an `Account` reference without knowing the subclass. `AdminService.applyInterest()` calls `ib.applyInterest()` on any `InterestBearing` account — savings accounts earn interest, loan accounts accrue debt, all through the same method call.

### Design Patterns
- **Factory Pattern** — `AccountFactory` centralises account creation. Callers receive an `Account` reference without knowing which subclass was instantiated.
- **Observer Pattern** — `BankEventListener` interface with `NotificationService` as the concrete observer. `TransactionService` fires events after each operation without knowing how they are handled.
- **Repository Pattern** — Generic `Repository<T, ID>` interface decouples business logic from persistence. Services never touch `FileManager` directly.
- **Strategy Pattern** (implicit) — Interest calculation strategy is encapsulated in each account subclass via `calculateInterest()`.

### SOLID Principles
- **S** — Every class has one job. `AuthService` handles only authentication. `FileManager` handles only file I/O.
- **O** — Adding a new account type requires zero changes to existing services — just subclass `Account`.
- **L** — Any `Account` subclass can be used wherever an `Account` is expected.
- **I** — `InterestBearing`, `Auditable`, and `Transferable` are focused single-purpose interfaces.
- **D** — Services depend on interfaces (`Repository`, `BankEventListener`), not concrete classes.

---

## Technical Highlights

- **Serialization persistence** — `FileManager` uses `ObjectOutputStream` / `ObjectInputStream` with buffering to persist the entire object graph to `.dat` files. No database required.
- **Thread-safe transactions** — `deposit()` and `withdraw()` are `synchronized`, preventing race conditions when concurrent requests target the same account.
- **Transfer rollback** — `TransactionService.transfer()` reverses a withdrawal if the subsequent deposit fails, maintaining data consistency without a database transaction.
- **SHA-256 PIN hashing** — Raw PINs are never stored. `PinHasher` uses `java.security.MessageDigest` to hash before storage and comparison.
- **Java Streams** — Used throughout `AdminService` and repositories for filtering, grouping (`Collectors.groupingBy`), sorting, and aggregating collections without imperative loops.
- **Custom checked exceptions** — Five domain-specific exceptions each carry contextual data (e.g. `InsufficientFundsException` holds both the requested and available amounts).

---

## Project Structure

```
bank-system/
└── src/
    ├── enums/
    │   ├── AccountType.java
    │   ├── TransactionType.java
    │   ├── TransactionStatus.java
    │   └── UserRole.java
    ├── exceptions/
    │   ├── InsufficientFundsException.java
    │   ├── AccountFrozenException.java
    │   ├── AccountNotFoundException.java
    │   ├── DailyLimitExceededException.java
    │   └── InvalidPinException.java
    ├── interfaces/
    │   ├── Auditable.java
    │   ├── InterestBearing.java
    │   └── Transferable.java
    ├── models/
    │   ├── Account.java          (abstract base)
    │   ├── SavingsAccount.java
    │   ├── CheckingAccount.java
    │   ├── LoanAccount.java
    │   ├── Transaction.java      (immutable)
    │   └── User.java
    ├── patterns/
    │   ├── AccountFactory.java
    │   ├── BankEventListener.java
    │   └── NotificationService.java
    ├── repository/
    │   ├── Repository.java       (generic interface)
    │   ├── AccountRepository.java
    │   └── UserRepository.java
    ├── services/
    │   ├── AuthService.java
    │   ├── TransactionService.java
    │   ├── AdminService.java
    │   └── ATMService.java
    └── utils/
        ├── FileManager.java
        ├── PinHasher.java
        └── ConsoleHelper.java
```

---

## How to Run

```bash
# Compile all source files from the src directory
javac -cp src -d out $(find src -name "*.java")

# Run the application
java -cp out BankApplication
```

On first run the application creates a `bank_data/` directory and initialises empty data files. All data persists automatically after every operation.

---

## Sample Interaction

```
============================================================
=                  JAVA BANK SYSTEM                       =
============================================================

[1] Login
[2] Register
[3] Exit

> 2

Name: Ahmed Hassan
Email: ahmed@example.com
PIN: ****

  ✓ User registered successfully. ID: USR-4A2F91B3
  ✓ Account created: SAV-B7C3D12E (owner: Ahmed Hassan)

> 1  (Login)

Email: ahmed@example.com
PIN: ****
  ✓ Welcome back, Ahmed!

[1] Check Balance    [2] Deposit    [3] Withdraw
[4] Transfer         [5] Statement  [6] Change PIN
[7] My Accounts      [8] Logout

> 2  (Deposit)

Account: SAV-B7C3D12E
Amount: 5000
  ✓ Deposited $5000.00. New balance: $5000.00

> 3  (Withdraw)

Account: SAV-B7C3D12E
Amount: 4000
  ✗ Daily withdrawal limit of $3000.00 exceeded.
```

---

## Why No Framework?

This project deliberately avoids Spring Boot, Hibernate, or any other framework to demonstrate understanding of the fundamentals — the things frameworks do for you. Every feature here maps to a concept you would explain in a technical interview:

| "How would you..." | This project does it with... |
|---|---|
| Persist data | Java Serialization (`ObjectOutputStream`) |
| Hash passwords | `java.security.MessageDigest` SHA-256 |
| Handle concurrency | `synchronized` methods |
| Decouple components | Dependency Injection via constructors |
| Prevent invalid states | Custom checked exceptions |
| Add new features safely | Abstract classes + interfaces (Open/Closed) |
| Notify on events | Observer Pattern |
| Create objects cleanly | Factory Pattern |

---

## Technologies

- **Language** — Java 17+
- **Persistence** — Java Serialization (`java.io`)
- **Security** — `java.security.MessageDigest` (SHA-256)
- **Concurrency** — `java.util.concurrent` concepts via `synchronized`
- **Collections** — `HashMap`, `ArrayList`, `Collections.unmodifiableList`
- **Streams** — `java.util.stream` (filter, map, collect, groupingBy, sorted, limit)
- **Build** — `javac` (no build tool required)
- **IDE** — Any Java IDE (IntelliJ IDEA recommended)
