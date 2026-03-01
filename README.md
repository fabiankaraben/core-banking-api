# Core Banking API (Monolithic Edition)

This is a monolithic Core Banking API built with Java 21, Spring Boot 3, and Hexagonal Architecture. It demonstrates advanced enterprise patterns such as asynchronous message routing, idempotency, strict financial calculations, and containerized integration testing.

## Features
- **Account Management**: Create and view bank accounts.
- **Internal Transfers**: Secure money movement between accounts using strict `BigDecimal` precision and Banker's Rounding (`HALF_EVEN`).
- **Idempotency**: Redis-backed idempotency keys to prevent accidental double-charges during transfers.
- **Asynchronous Routing**: RabbitMQ integration for asynchronous domain event processing (e.g., notifications after a successful transfer).
- **Concurrency Control**: Optimistic locking (`@Version`) on Account entities to prevent race conditions.
- **Observability**: Prometheus metrics and Actuator endpoints exposed.

## Technology Stack
- Java 21 (Virtual Threads enabled)
- Spring Boot 3.2.x
- PostgreSQL 15+ (with Flyway Migrations)
- Redis 7
- RabbitMQ 3
- Testcontainers (Integration Testing)

## Prerequisites
- Docker & Docker Compose
- Java 21 (Optional, if you wish to run/build outside of Docker)
- Maven (Optional)

## Quick Start (Run using Docker)

The entire ecosystem (Postgres, Redis, RabbitMQ, Prometheus, Grafana, and the Spring Boot App) can be started using a single command:

```bash
docker compose up -d --build
```

The services will be available at:
- **API (Swagger UI)**: http://localhost:8080/swagger-ui.html
- **API Health Check**: http://localhost:8080/actuator/health
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## How to use the API

You can use the Swagger UI linked above, or use `curl`:

**1. Create Accounts**
```bash
# Create Source Account
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerReference": "CUST-A", "currency": "USD"}'

# Create Destination Account
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerReference": "CUST-B", "currency": "USD"}'
```
*(Take note of the returned UUIDs to use in the transfer)*

**2. Note:** Currently, new accounts start with `0.00`. To test transfers, you will need to manually adjust the source account balance in the database, as the system strictly prevents overdrafts.

**3. Execute Transfer**
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: my-unique-tx-key-1' \
  -d '{
    "sourceAccountId": "<SOURCE_UUID>",
    "destinationAccountId": "<DEST_UUID>",
    "amount": 50.00,
    "currency": "USD"
  }'
```
If you send the exact same `curl` request again with the same `Idempotency-Key`, the system will intercept it in Redis and return a `409 Conflict`.

## Running the Tests

The project includes Unit Tests (JUnit 5 + Mockito) and comprehensive Integration Tests (Testcontainers). 

To execute the tests, ensure your local Docker engine is running (required for Testcontainers), and execute:

```bash
mvn clean verify
```

*(Note: Ensure your local environment uses JDK 21. If you encounter Lombok compilation issues due to using newer JDK versions like 25, you can run the tests via a Maven Docker container: `docker run --rm -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven -v /var/run/docker.sock:/var/run/docker.sock maven:3.9.6-eclipse-temurin-21 mvn clean verify`)*

This will run all tests, spinning up ephemeral containers for Postgres, Redis, and RabbitMQ to test the application context in a production-like environment.
