# System Specification: Core Banking API (Monolithic Edition)

## 1. Project Overview
The Core Banking API is a robust, monolithic backend designed to handle financial transactions with high concurrency and strict data integrity. It demonstrates advanced enterprise patterns, asynchronous message routing, and precise financial calculations without relying on external Cloud Service Providers.

## 2. Technology Stack

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | Java 21 | Core development utilizing Virtual Threads, Records, and Pattern Matching. |
| **Framework** | Spring Boot 3.x | Application framework, Dependency Injection, and REST API routing. |
| **Build Tool** | Maven | Dependency management and build lifecycle. |
| **Database** | PostgreSQL | Single source of truth for ACID-compliant relational data. |
| **Migrations** | Flyway | Database schema versioning and management. |
| **Caching/KV** | Redis | Distributed caching and transaction idempotency management. |
| **Messaging** | RabbitMQ | AMQP Message Broker for reliable, asynchronous background task distribution. |
| **Testing** | JUnit 5, Mockito, Testcontainers | Unit testing and ephemeral container-based integration testing. |
| **Observability**| Micrometer, Prometheus, Grafana | Metrics collection, time-series data storage, and visual dashboards. |
| **Documentation**| OpenAPI (Swagger) | Automated API contract generation and interactive UI. |
| **Deployment** | Docker & Docker Compose | Containerization and local orchestration. |

## 3. Architectural Pattern
The system strictly adheres to **Hexagonal Architecture (Ports and Adapters)** to decouple the core business logic from infrastructure concerns.

* **Domain Layer:** Contains pure Java 21 business entities (`Account`, `Transaction`) and value objects (`Money`). Free from framework annotations.
* **Application Layer:** Contains Use Cases (Commands/Queries) and Ports (interfaces for repositories and message brokers).
* **Infrastructure Layer:** Contains Adapters. Inbound adapters (REST Controllers, `@RabbitListener` components) and Outbound adapters (Postgres Repositories, Redis Clients, `RabbitTemplate` Publishers).

## 4. Core Domain & Features

* **Account Management:** Endpoints to create bank accounts, retrieve account details, and check current balances.
* **Internal Transfers:** Secure money movement between two accounts within the same banking system.
* **Financial Precision:** Strict use of `java.math.BigDecimal`. Floating-point types are strictly prohibited.
* **Banker's Rounding:** All fractional calculations use `RoundingMode.HALF_EVEN` to prevent statistical bias.
* **Concurrency Handling:** Optimistic locking (`@Version`) on the Account entity to prevent race conditions during simultaneous transfer requests.

## 5. Advanced Infrastructure Capabilities



* **Idempotency (Redis):** Transfer endpoints require an `Idempotency-Key` header. The system checks Redis to ensure a specific transaction request is processed exactly once within a 24-hour TTL, preventing accidental double-charges.
* **Asynchronous Routing (RabbitMQ):** Following a successful Postgres database commit, a `TransactionCompletedMessage` is published to a RabbitMQ Exchange. 
* **Queues and Consumers:** The Exchange routes the message to specific Queues (e.g., `notification.queue`, `fraud-check.queue`). Internal `@RabbitListener` components consume these messages to handle side-effects asynchronously without blocking the main HTTP request thread.
* **Dead Letter Queues (DLQ):** Configured for message processing failures. If a notification fails after multiple retries, it is routed to a DLQ for manual inspection or automated alerts.
* **Transactional Outbox Pattern:** Implemented to guarantee at-least-once delivery to RabbitMQ, ensuring the Postgres transaction commit and the message publication intent are atomically tied.

## 6. Database Schema Design

The PostgreSQL database will utilize standard relational patterns for financial ledgers.

* **Table `accounts`:** Stores account UUID, customer reference, current balance (`NUMERIC(19,4)`), currency, status (ACTIVE, BLOCKED), and optimistic locking version.
* **Table `transactions`:** Stores transaction UUID, source account ID, destination account ID, amount (`NUMERIC(19,4)`), timestamp, and status (COMPLETED, FAILED).
* **Table `outbox_messages`:** Stores serialized AMQP messages pending publication to RabbitMQ to prevent the dual-write problem.

## 7. Directory Structure Convention

```
src/main/java/com/bank/core/
├── domain/ (Entities, Value Objects, Exceptions)
├── application/ (Use Cases, Ports: Inbound/Outbound)
└── infrastructure/ (Adapters)
    ├── in/ (REST Web, AMQP Listeners)
    ├── out/ (Persistence, Messaging, Caching)
    └── config/ (Spring Beans, Security, Swagger, RabbitMQ bindings)
```

## 8. Run Instructions
The entire ecosystem must be executable via a single command:
`docker compose up -d`
This will provision PostgreSQL, Redis, RabbitMQ (with Management Plugin UI), Prometheus, Grafana, and the Spring Boot application itself.