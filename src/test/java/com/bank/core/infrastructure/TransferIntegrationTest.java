package com.bank.core.infrastructure;

import com.bank.core.application.port.in.AccountUseCase;
import com.bank.core.domain.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransferIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountUseCase accountUseCase;

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("core_banking_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Test
    void shouldExecuteTransferAndCheckIdempotency() {
        // Given
        Account source = accountUseCase.createAccount("SRC-123", "USD");
        // Simulate a deposit hack for the test since we didn't expose a deposit API
        // directly
        executeDirectDeposit(source.getId().toString(), "100.00");
        Account dest = accountUseCase.createAccount("DST-123", "USD");

        String baseUrl = "http://localhost:" + port + "/api/v1/transfers";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "tx-test-id-1");

        Map<String, Object> request = Map.of(
                "sourceAccountId", source.getId(),
                "destinationAccountId", dest.getId(),
                "amount", 25.00,
                "currency", "USD");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // When - First Time
        ResponseEntity<Void> response1 = restTemplate.postForEntity(baseUrl, entity, Void.class);
        assertEquals(HttpStatus.ACCEPTED, response1.getStatusCode());

        // When - Second Time (Idempotency Check)
        ResponseEntity<Void> response2 = restTemplate.postForEntity(baseUrl, entity, Void.class);
        assertEquals(HttpStatus.CONFLICT, response2.getStatusCode());
    }

    // A small helper to artificially bump balance for testing the transfer,
    // real app would have a back-office deposit endpoint or process.
    @Autowired
    private com.bank.core.infrastructure.out.persistence.JpaAccountRepository jpaAccountRepository;

    private void executeDirectDeposit(String accountId, String amount) {
        var entity = jpaAccountRepository.findById(java.util.UUID.fromString(accountId)).get();
        var newBalance = entity.getBalance().add(new BigDecimal(amount));
        var updated = com.bank.core.infrastructure.out.persistence.AccountJpaEntity.builder()
                .id(entity.getId())
                .customerReference(entity.getCustomerReference())
                .balance(newBalance)
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(java.time.OffsetDateTime.now())
                .build();
        jpaAccountRepository.save(updated);
    }
}
