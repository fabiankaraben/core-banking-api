package com.bank.core.infrastructure.in.web.dto;

import com.bank.core.domain.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing the result of a fund transfer returned by the REST API.
 *
 * @param transactionId        the unique transaction identifier
 * @param sourceAccountId      the debited account identifier
 * @param destinationAccountId the credited account identifier
 * @param amount               the transferred amount
 * @param currency             the ISO 4217 currency code
 * @param status               the transaction outcome (e.g., {@code "COMPLETED"})
 * @param failureReason        the failure description, or {@code null} on success
 * @param createdAt            the ISO-8601 transaction creation timestamp
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Schema(description = "Result of a fund transfer operation")
public record TransactionResponse(

        @Schema(description = "Unique transaction identifier")
        UUID transactionId,

        @Schema(description = "Source (debit) account identifier")
        UUID sourceAccountId,

        @Schema(description = "Destination (credit) account identifier")
        UUID destinationAccountId,

        @Schema(description = "Transferred amount", example = "250.0000")
        BigDecimal amount,

        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Transaction outcome", example = "COMPLETED")
        String status,

        @Schema(description = "Failure reason, present only when status is FAILED")
        String failureReason,

        @Schema(description = "ISO-8601 transaction creation timestamp")
        Instant createdAt
) {

    /**
     * Factory method that maps a domain {@link Transaction} to a {@code TransactionResponse}.
     *
     * @param tx the domain transaction to convert
     * @return the corresponding response DTO
     */
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(),
                tx.getDestinationAccountId(),
                tx.getAmount().amount(),
                tx.getAmount().currency().getCurrencyCode(),
                tx.getStatus().name(),
                tx.getFailureReason(),
                tx.getCreatedAt()
        );
    }
}
