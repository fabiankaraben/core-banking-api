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
 * Request body DTO for the account creation endpoint.
 *
 * <p>All fields are validated by Spring's Bean Validation (JSR-380) before the request
 * reaches the application layer. Validation errors are translated to HTTP 400 responses
 * by the global exception handler.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Schema(description = "Payload required to open a new bank account")
public record CreateAccountRequest(

        @Schema(description = "UUID of the customer who will own the account",
                example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "customerId is required")
        UUID customerId,

        @Schema(description = "Opening balance (non-negative)", example = "1000.00")
        @NotNull(message = "initialBalance is required")
        @DecimalMin(value = "0.00", message = "initialBalance must be non-negative")
        BigDecimal initialBalance,

        @Schema(description = "ISO 4217 currency code", example = "USD")
        @NotBlank(message = "currencyCode is required")
        @Size(min = 3, max = 3, message = "currencyCode must be exactly 3 characters")
        @Pattern(regexp = "[A-Z]{3}", message = "currencyCode must be an uppercase ISO 4217 code")
        String currencyCode
) {}
