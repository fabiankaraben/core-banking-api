<div align="center">

# Core Banking API

**A production-grade, monolithic banking backend built with Java 21 & Spring Boot 3**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running the Full Stack](#running-the-full-stack)
  - [Running Locally (IDE)](#running-locally-ide)
- [API Reference](#api-reference)
- [Running the Tests](#running-the-tests)
- [Observability](#observability)
- [Generating Javadoc](#generating-javadoc)
- [Design Decisions](#design-decisions)

---

## Overview

The **Core Banking API** is a robust, monolithic backend designed to handle financial transactions with high concurrency and strict data integrity. It demonstrates advanced enterprise patterns without relying on any external cloud services — the entire ecosystem runs locally via Docker Compose.

This project serves as a portfolio reference implementation that mirrors the quality and design rigour expected in a production financial system.

---

## Architecture

The system strictly adheres to **Hexagonal Architecture (Ports and Adapters)**, cleanly separating business logic from infrastructure:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Application                             │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Domain Layer                        │   │
│  │   Account · Transaction · Money · OutboxMessage          │   │
│  │   (pure Java 21 — zero framework dependencies)           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Application Layer                       │   │
│  │   Use Cases (Commands/Queries)  ·  Ports (Interfaces)    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                Infrastructure Layer                       │   │
│  │  Inbound: REST Controllers · @RabbitListener             │   │
│  │  Outbound: JPA Adapters · Redis Adapter · AMQP Publisher │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

The domain and application layers have **zero awareness** of Spring, JPA, or any other framework. All technology-specific code lives exclusively in the infrastructure layer.

---

## Technology Stack

| Component | Technology | Purpose |
|:---|:---|:---|
| **Language** | Java 21 | Virtual Threads, Records, Pattern Matching |
| **Framework** | Spring Boot 3.2 | DI, REST routing, Scheduling |
| **Build Tool** | Maven 3.9 | Dependency management, build lifecycle |
| **Database** | PostgreSQL 16 | ACID-compliant relational data store |
| **Migrations** | Flyway | Schema versioning |
| **Cache / KV** | Redis 7 | Idempotency key store (24 h TTL) |
| **Messaging** | RabbitMQ 3.13 | Asynchronous event routing |
| **Testing** | JUnit 5 · Mockito · Testcontainers | Unit & integration tests |
| **Observability** | Micrometer · Prometheus · Grafana | Metrics & dashboards |
| **API Docs** | OpenAPI 3 / Swagger UI | Auto-generated interactive documentation |
| **Deployment** | Docker & Docker Compose | Full local orchestration |

---

## Features

### Core Banking
- **Account Management** — create accounts with an initial balance; retrieve account details and current balance.
- **Internal Transfers** — atomic fund transfers between two accounts within the same system.
- **Financial Precision** — all monetary values use `java.math.BigDecimal` with `NUMERIC(19,4)` columns. Floating-point types are strictly prohibited.
- **Banker's Rounding** — `RoundingMode.HALF_EVEN` on all fractional calculations to prevent statistical bias.

### Concurrency & Reliability
- **Optimistic Locking** — `@Version` on the `Account` entity prevents lost-update anomalies during simultaneous transfers.
- **Idempotency (Redis)** — every transfer requires an `Idempotency-Key` header. Duplicate requests within the 24-hour TTL window return the cached result without re-processing.
- **Transactional Outbox Pattern** — the Postgres transaction commit and the RabbitMQ message publication intent are atomically bound, guaranteeing at-least-once delivery.

### Asynchronous Messaging
- **Exchange → Queues** — a `banking.events` Direct Exchange routes `TransactionCompletedMessage` events to `notification.queue` and `fraud-check.queue`.
- **Dead Letter Queues** — each primary queue has a configured DLQ. Messages that exhaust their retry budget are automatically forwarded for manual inspection.
- **Outbox Relay Scheduler** — a background component polls the `outbox_messages` table every 5 seconds and publishes pending messages to RabbitMQ.

### Observability
- **Micrometer metrics** exposed at `/actuator/prometheus` — includes custom counters for completed/failed transfers and outbox relay events.
- **Prometheus** scrapes the application on a 10-second interval.
- **Grafana** dashboards pre-provisioned with the Prometheus data source.

---

## Project Structure

```
src/main/java/com/bank/core/
├── CoreBankingApplication.java
│
├── domain/                          # Pure Java — no framework annotations
│   ├── model/
│   │   ├── Account.java
│   │   ├── AccountStatus.java
│   │   ├── Money.java               # Immutable value object (BigDecimal + Currency)
│   │   ├── OutboxMessage.java
│   │   ├── Transaction.java
│   │   └── TransactionStatus.java
│   └── exception/
│       ├── AccountBlockedException.java
│       ├── AccountNotFoundException.java
│       ├── DuplicateTransactionException.java
│       └── InsufficientFundsException.java
│
├── application/                     # Use cases + port interfaces
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateAccountUseCase.java
│   │   │   ├── GetAccountUseCase.java
│   │   │   └── TransferFundsUseCase.java
│   │   └── out/
│   │       ├── AccountRepository.java
│   │       ├── IdempotencyPort.java
│   │       ├── MessagePublisherPort.java
│   │       ├── OutboxMessageRepository.java
│   │       └── TransactionRepository.java
│   └── service/
│       ├── CreateAccountService.java
│       ├── GetAccountService.java
│       └── TransferFundsService.java
│
└── infrastructure/                  # Spring / JPA / Redis / RabbitMQ
    ├── in/
    │   ├── amqp/
    │   │   └── TransactionEventListener.java
    │   └── web/
    │       ├── AccountController.java
    │       ├── GlobalExceptionHandler.java
    │       ├── TransferController.java
    │       └── dto/
    ├── out/
    │   ├── cache/
    │   │   └── RedisIdempotencyAdapter.java
    │   ├── messaging/
    │   │   ├── OutboxRelayScheduler.java
    │   │   └── RabbitMQPublisherAdapter.java
    │   └── persistence/
    │       ├── AccountPersistenceAdapter.java
    │       ├── TransactionPersistenceAdapter.java
    │       ├── OutboxMessagePersistenceAdapter.java
    │       ├── entity/
    │       └── repository/
    └── config/
        ├── OpenApiConfig.java
        ├── RabbitMQConfig.java
        └── VirtualThreadsConfig.java
```

---

## Getting Started

### Prerequisites

| Tool | Minimum Version |
|:---|:---|
| Docker | 24+ |
| Docker Compose | 2.24+ |
| Java (for local dev) | 21 |
| Maven (for local dev) | 3.9 |

### Running the Full Stack

The entire ecosystem — PostgreSQL, Redis, RabbitMQ, the Spring Boot application, Prometheus, and Grafana — starts with a single command:

```bash
docker compose up -d
```

Wait ~30 seconds for all services to pass their health checks, then access:

| Service | URL | Credentials |
|:---|:---|:---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |
| **Actuator / Health** | http://localhost:8080/actuator/health | — |
| **RabbitMQ Management** | http://localhost:15672 | `guest` / `guest` |
| **Prometheus** | http://localhost:9090 | — |
| **Grafana** | http://localhost:3000 | `admin` / `admin` |

To stop and remove containers:

```bash
docker compose down
```

To stop and also remove all persistent volumes:

```bash
docker compose down -v
```

### Running Locally (IDE)

> Requires a running PostgreSQL instance, Redis, and RabbitMQ. The easiest approach is to start only the infrastructure services via Docker Compose:

```bash
docker compose up -d postgres redis rabbitmq
```

Then run the application:

```bash
./mvnw spring-boot:run
```

---

## API Reference

Full interactive documentation is available at **http://localhost:8080/swagger-ui.html** when the application is running.

### Create Account

```http
POST /api/v1/accounts
Content-Type: application/json

{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "initialBalance": 1000.00,
  "currencyCode": "USD"
}
```

### Get Account

```http
GET /api/v1/accounts/{accountId}
```

### Transfer Funds

```http
POST /api/v1/transfers
Content-Type: application/json
Idempotency-Key: <unique-key-per-request>

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationAccountId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "amount": 250.00,
  "currencyCode": "USD"
}
```

> **Idempotency-Key** — a client-generated UUID (or any unique string ≤ 255 chars). Submitting the same key within 24 hours returns the original result without re-executing the transfer.

### Error Responses

All errors use a consistent envelope:

```json
{
  "timestamp": "2024-01-15T12:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient funds in account ...",
  "path": "/api/v1/transfers",
  "errors": []
}
```

| HTTP Status | Cause |
|:---|:---|
| `400 Bad Request` | Invalid request payload or missing required header |
| `404 Not Found` | Account does not exist |
| `409 Conflict` | Concurrent in-flight request for the same idempotency key, or optimistic lock failure |
| `422 Unprocessable Entity` | Insufficient funds or blocked account |
| `500 Internal Server Error` | Unexpected server-side error |

---

## Running the Tests

### Unit Tests only

```bash
./mvnw test
```

### Integration Tests (requires Docker for Testcontainers)

Integration tests spin up ephemeral PostgreSQL, RabbitMQ, and Redis containers automatically via [Testcontainers](https://testcontainers.com/). Docker must be running.

```bash
./mvnw verify
```

> **macOS + Docker Desktop** — Testcontainers requires the Docker socket to accept versioned API calls. Enable this once in Docker Desktop: **Settings → Advanced → Allow the default Docker socket to be used**. Without it the integration tests are automatically skipped (not failed), so `./mvnw verify` still exits 0.
>
> On Linux/CI the standard `/var/run/docker.sock` works without any additional configuration.

### Full test suite with coverage report

```bash
./mvnw verify -Pcoverage
```

### Test structure

| Test class | Type | What it covers |
|:---|:---|:---|
| `MoneyTest` | Unit | `Money` arithmetic, rounding, currency validation |
| `AccountTest` | Unit | Account lifecycle, debit/credit, blocking |
| `TransferFundsServiceTest` | Unit (Mockito) | Transfer orchestration, idempotency, error paths, outbox message metadata |
| `TransferIntegrationTest` | Integration (Testcontainers) | Full HTTP round-trip via `TestRestTemplate` against a live PostgreSQL + RabbitMQ stack |

---

## Observability

### Metrics

Business metrics are exposed at `/actuator/prometheus` and scraped by Prometheus:

| Metric | Description |
|:---|:---|
| `banking.transfer.completed` | Count of successfully completed transfers |
| `banking.transfer.failed` | Count of failed transfer attempts |
| `banking.transfer.idempotent` | Count of idempotent replay responses |
| `banking.outbox.relay.published` | Count of messages successfully relayed to RabbitMQ |
| `banking.outbox.relay.failed` | Count of relay attempts that failed (will retry) |
| `banking.notification.processed` | Count of notification events consumed |
| `banking.fraud.check.processed` | Count of fraud-check events consumed |

Standard JVM, Hikari connection pool, and Spring MVC metrics are also collected automatically by Micrometer.

### Grafana

The Prometheus data source is auto-provisioned. Navigate to **http://localhost:3000** and import community dashboards (e.g. JVM Micrometer dashboard ID **4701**) to visualise heap, GC, thread pool, and HTTP metrics.

---

## Generating Javadoc

```bash
./mvnw javadoc:javadoc
```

The HTML documentation is generated in `target/site/apidocs/index.html`.

To generate and attach the Javadoc JAR alongside the application JAR:

```bash
./mvnw package
```

---

## Design Decisions

**Why Hexagonal Architecture?**
The domain and application layers are completely free of framework annotations. This makes business logic testable in pure unit tests with no Spring context, minimises coupling, and lets the infrastructure be swapped (e.g. replacing Redis with Memcached) without touching business code.

**Why the Transactional Outbox Pattern instead of direct RabbitMQ publishing?**
Publishing a message inside a database transaction creates a dual-write problem: if the broker is unavailable at commit time, the database record exists but the message is lost. The outbox table is written atomically with the business data, and the relay scheduler handles delivery with automatic retry — guaranteeing at-least-once delivery.

**Why Optimistic Locking + Pessimistic Write Lock?**
The `@Version` optimistic lock is the primary guard; it ensures stale reads in the JPA session are rejected. The `SELECT FOR UPDATE` pessimistic lock is applied when loading accounts for a transfer to prevent two concurrent sessions from reading the same balance and both attempting a debit.

**Why Java 21 Virtual Threads?**
Banking APIs are I/O-bound: every request involves multiple database round-trips, Redis lookups, and potential AMQP operations. Virtual Threads allow each request to block on I/O without consuming a platform thread, achieving high throughput without reactive programming complexity.

---

<div align="center">
  <sub>Built with Java 21 · Spring Boot 3 · PostgreSQL · Redis · RabbitMQ</sub>
</div>
