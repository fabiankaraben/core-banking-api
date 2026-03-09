package com.bank.core.domain.exception;

import com.bank.core.domain.model.Money;
import java.util.UUID;

/**
 * Thrown when a debit operation cannot be completed because the source account
 * does not hold sufficient funds to cover the requested transfer amount.
 *
 * <p>The REST exception handler maps this exception to HTTP 422 Unprocessable Entity.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class InsufficientFundsException extends RuntimeException {

    /** The source account that lacked sufficient funds. */
    private final UUID accountId;

    /** The balance available at the time of the failed debit. */
    private final Money availableBalance;

    /** The amount that was requested but could not be fulfilled. */
    private final Money requestedAmount;

    /**
     * Constructs an {@code InsufficientFundsException} with detailed balance information.
     *
     * @param accountId        the UUID of the source account
     * @param availableBalance the current balance of the account
     * @param requestedAmount  the amount that was requested
     */
    public InsufficientFundsException(UUID accountId, Money availableBalance, Money requestedAmount) {
        super(String.format(
                "Insufficient funds in account %s: available=%s, requested=%s",
                accountId, availableBalance, requestedAmount));
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    /**
     * Returns the source account identifier.
     *
     * @return the account UUID
     */
    public UUID getAccountId() { return accountId; }

    /**
     * Returns the available balance at the time of the failed debit.
     *
     * @return the available {@link Money} balance
     */
    public Money getAvailableBalance() { return availableBalance; }

    /**
     * Returns the amount that was requested but could not be fulfilled.
     *
     * @return the requested {@link Money} amount
     */
    public Money getRequestedAmount() { return requestedAmount; }
}
