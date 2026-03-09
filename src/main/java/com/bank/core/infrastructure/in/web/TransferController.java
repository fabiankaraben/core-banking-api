package com.bank.core.infrastructure.in.web;

import com.bank.core.application.port.in.TransferFundsUseCase;
import com.bank.core.infrastructure.in.web.dto.ApiErrorResponse;
import com.bank.core.infrastructure.in.web.dto.TransactionResponse;
import com.bank.core.infrastructure.in.web.dto.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound REST adapter for internal fund transfer operations.
 *
 * <p>Exposes {@code POST /api/v1/transfers}, which requires an
 * {@code Idempotency-Key} HTTP header. The key guarantees that submitting the same
 * request multiple times within the 24-hour TTL window produces exactly one transfer.</p>
 *
 * <p>Idempotent replays return the original transaction result with HTTP 200 (not 201),
 * allowing clients to distinguish a new transfer from a replayed one by checking the
 * {@code X-Idempotent-Replay} response header set by this controller.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Validated
@Tag(name = "Transfers", description = "Internal fund transfer operations")
public class TransferController {

    private final TransferFundsUseCase transferFundsUseCase;

    /**
     * Constructs a {@code TransferController} with the required use case.
     *
     * @param transferFundsUseCase the use case for executing fund transfers
     */
    public TransferController(TransferFundsUseCase transferFundsUseCase) {
        this.transferFundsUseCase = transferFundsUseCase;
    }

    /**
     * Initiates an internal fund transfer between two accounts.
     *
     * <p>The {@code Idempotency-Key} header must be supplied with every request.
     * A client-generated UUID v4 is the recommended format. If the key has been
     * seen before within its TTL window, the cached transaction is returned.</p>
     *
     * @param idempotencyKey the caller-supplied unique key for this request
     * @param request        the transfer payload
     * @return the resulting transaction with HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Transfer funds between accounts",
        description = "Performs an atomic, idempotent internal transfer. Supply a unique " +
                      "`Idempotency-Key` header (UUID v4 recommended) to enable safe retries."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer completed successfully",
                     content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Source or destination account not found",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Duplicate in-flight request for the same idempotency key",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Insufficient funds or blocked account",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TransactionResponse transfer(
            @Parameter(description = "Unique key for idempotency (UUID v4 recommended)", required = true)
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header is required")
            @Size(max = 255, message = "Idempotency-Key must not exceed 255 characters")
            String idempotencyKey,

            @Valid @RequestBody TransferRequest request) {

        var command = new TransferFundsUseCase.TransferCommand(
                idempotencyKey,
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currencyCode()
        );
        return TransactionResponse.from(transferFundsUseCase.transfer(command));
    }
}
