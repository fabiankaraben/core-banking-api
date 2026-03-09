# =============================================================================
# Multi-stage Dockerfile for Core Banking API
#
# Stage 1 (builder): Compiles and packages the application with Maven.
# Stage 2 (runtime): Runs the packaged JAR on a minimal JRE image.
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1 — Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN ./mvnw -f pom.xml -B -q -DskipTests package 2>/dev/null || \
    (apk add --no-cache maven && mvn -B -q -DskipTests package)

# ---------------------------------------------------------------------------
# Stage 2 — Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Core Banking Team"
LABEL description="Core Banking API — Java 21 / Spring Boot 3"

WORKDIR /app

# Create a non-root user for the application process
RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder /workspace/target/core-banking-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
