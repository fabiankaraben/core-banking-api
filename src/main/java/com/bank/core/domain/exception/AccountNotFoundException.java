package com.bank.core.domain.exception;

import java.util.UUID;

/**
 * Thrown when a requested {@link com.bank.core.domain.model.Account} cannot be
 * located in the persistence store.
 *
 * <p>This is an unchecked exception because a missing account represents a
 * programming error or a stale reference, not a recoverable condition from the
 * caller's perspective. The REST exception handler maps it to HTTP 404.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AccountNotFoundException extends RuntimeException {

    /** The identifier of the account that was not found. */
    private final UUID accountId;

    /**
     * Constructs an {@code AccountNotFoundException} for the given account identifier.
     *
     * @param accountId the UUID of the account that could not be found
     */
    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }

    /**
     * Returns the identifier of the account that was not found.
     *
     * @return the missing account UUID
     */
    public UUID getAccountId() {
        return accountId;
    }
}
