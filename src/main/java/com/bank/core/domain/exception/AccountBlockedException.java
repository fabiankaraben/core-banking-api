package com.bank.core.domain.exception;

import java.util.UUID;

/**
 * Thrown when an operation is attempted on an account whose status is
 * {@link com.bank.core.domain.model.AccountStatus#BLOCKED}.
 *
 * <p>The REST exception handler maps this exception to HTTP 422 Unprocessable Entity.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AccountBlockedException extends RuntimeException {

    /** The identifier of the blocked account. */
    private final UUID accountId;

    /**
     * Constructs an {@code AccountBlockedException} for the given account identifier.
     *
     * @param accountId the UUID of the blocked account
     */
    public AccountBlockedException(UUID accountId) {
        super("Account is blocked and cannot perform operations: " + accountId);
        this.accountId = accountId;
    }

    /**
     * Returns the identifier of the blocked account.
     *
     * @return the blocked account UUID
     */
    public UUID getAccountId() {
        return accountId;
    }
}
