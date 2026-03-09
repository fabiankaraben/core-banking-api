package com.bank.core.infrastructure;

import com.bank.core.application.port.in.CreateAccountUseCase;
import com.bank.core.domain.model.Account;
import com.bank.core.infrastructure.in.web.dto.AccountResponse;
import com.bank.core.infrastructure.in.web.dto.CreateAccountRequest;
import com.bank.core.infrastructure.in.web.dto.TransactionResponse;
import com.bank.core.infrastructure.in.web.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the full HTTP request-response cycle of the transfer endpoint.
 *
 * <p>Starts the full Spring Boot context with PostgreSQL and RabbitMQ containers
 * (inherited from {@link AbstractIntegrationTest}). The
 * {@link com.bank.core.application.port.out.IdempotencyPort} is mocked to avoid
 * requiring a running Redis instance in CI.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("Transfer API integration tests")
class TransferIntegrationIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CreateAccountUseCase createAccountUseCase;

    @MockBean
    private com.bank.core.application.port.out.IdempotencyPort idempotencyPort;

    private UUID sourceAccountId;
    private UUID destinationAccountId;

    @BeforeEach
    void setUp() {
        when(idempotencyPort.get(any())).thenReturn(Optional.empty());
        when(idempotencyPort.tryAcquire(any())).thenReturn(true);

        Account source = createAccountUseCase.createAccount(
                new CreateAccountUseCase.CreateAccountCommand(
                        UUID.randomUUID(), new BigDecimal("2000.00"), "USD"));
        Account destination = createAccountUseCase.createAccount(
                new CreateAccountUseCase.CreateAccountCommand(
                        UUID.randomUUID(), new BigDecimal("500.00"), "USD"));

        sourceAccountId = source.getId();
        destinationAccountId = destination.getId();
    }

    @Nested
    @DisplayName("POST /api/v1/accounts")
    class CreateAccountEndpoint {

        @Test
        @DisplayName("returns HTTP 201 and the created account")
        void createsAccount() {
            CreateAccountRequest body = new CreateAccountRequest(
                    UUID.randomUUID(), new BigDecimal("1000.00"), "USD");

            ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/accounts", body, AccountResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo("ACTIVE");
            assertThat(response.getBody().balance()).isEqualByComparingTo("1000.0000");
        }

        @Test
        @DisplayName("returns HTTP 400 for invalid request")
        void rejectsMissingCustomerId() {
            CreateAccountRequest invalid = new CreateAccountRequest(
                    null, new BigDecimal("100.00"), "USD");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/accounts", invalid, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transfers")
    class TransferEndpoint {

        @Test
        @DisplayName("returns HTTP 201 and COMPLETED transaction for valid transfer")
        void completesValidTransfer() {
            String idempotencyKey = UUID.randomUUID().toString();
            TransferRequest body = new TransferRequest(
                    sourceAccountId, destinationAccountId, new BigDecimal("500.00"), "USD");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", idempotencyKey);
            HttpEntity<TransferRequest> request = new HttpEntity<>(body, headers);

            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/transfers", request, TransactionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
            assertThat(response.getBody().amount()).isEqualByComparingTo("500.0000");
        }

        @Test
        @DisplayName("returns HTTP 404 when source account does not exist")
        void returns404ForMissingAccount() {
            String idempotencyKey = UUID.randomUUID().toString();
            TransferRequest body = new TransferRequest(
                    UUID.randomUUID(), destinationAccountId, new BigDecimal("100.00"), "USD");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", idempotencyKey);
            HttpEntity<TransferRequest> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/transfers", request, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("returns HTTP 400 when Idempotency-Key header is missing")
        void returns400WhenHeaderMissing() {
            TransferRequest body = new TransferRequest(
                    sourceAccountId, destinationAccountId, new BigDecimal("100.00"), "USD");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/transfers", body, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns HTTP 422 for insufficient funds")
        void returns422ForInsufficientFunds() {
            String idempotencyKey = UUID.randomUUID().toString();
            TransferRequest body = new TransferRequest(
                    sourceAccountId, destinationAccountId, new BigDecimal("99999.00"), "USD");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", idempotencyKey);
            HttpEntity<TransferRequest> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/v1/transfers", request, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
