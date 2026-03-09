package com.bank.core.infrastructure.in.web;

import com.bank.core.application.port.in.CreateAccountUseCase;
import com.bank.core.application.port.in.GetAccountUseCase;
import com.bank.core.infrastructure.in.web.dto.AccountResponse;
import com.bank.core.infrastructure.in.web.dto.ApiErrorResponse;
import com.bank.core.infrastructure.in.web.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Inbound REST adapter for account management operations.
 *
 * <p>Exposes two endpoints under {@code /api/v1/accounts}:</p>
 * <ul>
 *   <li>{@code POST /} — opens a new bank account</li>
 *   <li>{@code GET /{accountId}} — retrieves account details and current balance</li>
 * </ul>
 *
 * <p>This controller is a thin adapter: it translates HTTP requests into use-case
 * commands and maps domain objects back to JSON response DTOs. No business logic
 * lives here.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    /**
     * Constructs an {@code AccountController} with the required use cases.
     *
     * @param createAccountUseCase the use case for creating new accounts
     * @param getAccountUseCase    the use case for retrieving account details
     */
    public AccountController(CreateAccountUseCase createAccountUseCase,
                              GetAccountUseCase getAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
    }

    /**
     * Opens a new bank account for the specified customer.
     *
     * @param request the account creation payload
     * @return the created account with HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new bank account",
               description = "Creates an ACTIVE bank account for the specified customer with an initial balance.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully",
                     content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        var command = new CreateAccountUseCase.CreateAccountCommand(
                request.customerId(),
                request.initialBalance(),
                request.currencyCode()
        );
        return AccountResponse.from(createAccountUseCase.createAccount(command));
    }

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param accountId the UUID of the account to retrieve
     * @return the account details with HTTP 200
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID",
               description = "Returns the details and current balance of the specified account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found",
                     content = @Content(schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
                     content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return AccountResponse.from(getAccountUseCase.getAccount(accountId));
    }
}
