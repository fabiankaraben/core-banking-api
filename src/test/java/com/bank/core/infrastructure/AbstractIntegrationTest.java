package com.bank.core.infrastructure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all Spring Boot integration tests.
 *
 * <p>Starts PostgreSQL, RabbitMQ, and Redis containers via Testcontainers before any
 * test in the extending class runs. Container lifecycle is managed at the class level
 * ({@code @Container} on static fields), so a single container instance is shared
 * across all tests in a given subclass — dramatically reducing test-suite start-up time.</p>
 *
 * <p>Dynamic Spring properties (datasource URL, credentials, broker host/port, Redis host/port)
 * are injected via {@link DynamicPropertySource}, ensuring each test run uses the ephemeral
 * ports assigned by Testcontainers rather than a fixed local configuration.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Redis exposed port inside the container. */
    private static final int REDIS_PORT = 6379;

    /** PostgreSQL 16 container reused across all tests in a subclass. */
    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("banking_test")
                    .withUsername("banking")
                    .withPassword("banking");

    /** RabbitMQ 3.13 container with the management plugin enabled. */
    @Container
    protected static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    /**
     * Redis 7 container. Spring's Redis auto-configuration requires a reachable
     * broker on startup even when {@link com.bank.core.application.port.out.IdempotencyPort}
     * is mocked at the application layer.
     */
    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(REDIS_PORT);

    /**
     * Injects Testcontainers-allocated connection properties into the Spring
     * {@code Environment} before the application context is refreshed.
     *
     * @param registry the dynamic property registry provided by the test framework
     */
    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
    }
}
