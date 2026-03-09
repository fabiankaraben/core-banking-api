package com.bank.core.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body DTO for the fund transfer endpoint.
 *
 * <p>The caller must also supply an {@code Idempotency-Key} HTTP header. The key may be
 * any string up to 255 characters; a UUID v4 is recommended.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Schema(description = "Payload required to initiate an internal fund transfer")
public record TransferRequest(

        @Schema(description = "UUID of the source (debit) account",
                example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "sourceAccountId is required")
        UUID sourceAccountId,

        @Schema(description = "UUID of the destination (credit) account",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @NotNull(message = "destinationAccountId is required")
        UUID destinationAccountId,

        @Schema(description = "Amount to transfer (must be positive)", example = "250.00")
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "ISO 4217 currency code of the transfer amount", example = "USD")
        @NotBlank(message = "currencyCode is required")
        @Size(min = 3, max = 3, message = "currencyCode must be exactly 3 characters")
        @Pattern(regexp = "[A-Z]{3}", message = "currencyCode must be an uppercase ISO 4217 code")
        String currencyCode
) {}
