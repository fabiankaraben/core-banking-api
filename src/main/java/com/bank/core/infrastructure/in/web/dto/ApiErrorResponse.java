package com.bank.core.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error envelope returned by the global exception handler for all
 * non-2xx HTTP responses.
 *
 * <p>All error responses share this structure, making it easy for API consumers to
 * parse failures uniformly regardless of the error category.</p>
 *
 * @param timestamp the UTC instant at which the error occurred
 * @param status    the HTTP status code
 * @param error     a short description of the HTTP status (e.g., {@code "Not Found"})
 * @param message   a developer-friendly description of the error cause
 * @param path      the request URI that triggered the error
 * @param errors    a list of field-level validation errors (non-empty only for HTTP 400)
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Schema(description = "Standardised error envelope for all non-2xx responses")
public record ApiErrorResponse(

        @Schema(description = "UTC timestamp of the error")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "HTTP status description", example = "Not Found")
        String error,

        @Schema(description = "Developer-friendly error message")
        String message,

        @Schema(description = "Request path that caused the error", example = "/api/v1/accounts/abc")
        String path,

        @Schema(description = "Field-level validation errors (populated for HTTP 400 only)")
        List<FieldError> errors
) {

    /**
     * Represents a single field-level validation violation.
     *
     * @param field   the name of the request field that failed validation
     * @param message the validation constraint message
     */
    @Schema(description = "Individual field validation error")
    public record FieldError(
            @Schema(description = "Field name", example = "amount") String field,
            @Schema(description = "Violation message", example = "must be greater than zero") String message
    ) {}
}
