package com.bank.core.infrastructure.in.web.dto;

import com.bank.core.domain.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a bank account returned by the REST API.
 *
 * <p>Maps from the domain {@link Account} object. Using a dedicated response record
 * prevents domain internals from leaking into the API contract.</p>
 *
 * @param id         the unique account identifier
 * @param customerId the owning customer identifier
 * @param balance    the current balance
 * @param currency   the ISO 4217 currency code
 * @param status     the account status string (e.g., {@code "ACTIVE"})
 * @param createdAt  the account creation timestamp
 * @param updatedAt  the last-updated timestamp
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Schema(description = "Bank account details")
public record AccountResponse(

        @Schema(description = "Unique account identifier",
                example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Owning customer identifier",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID customerId,

        @Schema(description = "Current account balance", example = "1500.0000")
        BigDecimal balance,

        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Account lifecycle status", example = "ACTIVE")
        String status,

        @Schema(description = "ISO-8601 creation timestamp")
        Instant createdAt,

        @Schema(description = "ISO-8601 last-updated timestamp")
        Instant updatedAt
) {

    /**
     * Factory method that maps a domain {@link Account} to an {@code AccountResponse}.
     *
     * @param account the domain account to convert
     * @return the corresponding response DTO
     */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getBalance().amount(),
                account.getCurrency().getCurrencyCode(),
                account.getStatus().name(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
