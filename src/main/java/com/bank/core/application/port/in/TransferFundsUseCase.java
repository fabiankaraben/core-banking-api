package com.bank.core.application.port.in;

import com.bank.core.domain.model.Transaction;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound port defining the contract for transferring funds between two accounts.
 *
 * <p>This is the primary write operation in the system. The implementation enforces:</p>
 * <ul>
 *   <li>Idempotency via Redis (24-hour TTL on the {@code Idempotency-Key})</li>
 *   <li>Optimistic locking on both accounts to prevent concurrent modification</li>
 *   <li>Atomic Postgres commit with a corresponding outbox message record</li>
 *   <li>Post-commit message publication to RabbitMQ via the outbox relay</li>
 * </ul>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface TransferFundsUseCase {

    /**
     * Command record encapsulating all data required to initiate a fund transfer.
     *
     * @param idempotencyKey       a caller-generated UUID or token that uniquely identifies
     *                             this transfer request; used for deduplication
     * @param sourceAccountId      the UUID of the account to debit
     * @param destinationAccountId the UUID of the account to credit
     * @param amount               the amount to transfer (must be positive)
     * @param currencyCode         the ISO 4217 currency code of the transfer amount
     */
    record TransferCommand(
            String idempotencyKey,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currencyCode) {}

    /**
     * Executes a fund transfer from the source account to the destination account.
     *
     * <p>If the idempotency key has been seen before within its TTL window, the cached
     * {@link Transaction} result from the original request is returned immediately without
     * re-processing.</p>
     *
     * @param command the transfer command containing all required parameters
     * @return the resulting {@link Transaction} (either newly created or fetched from cache)
     * @throws com.bank.core.domain.exception.AccountNotFoundException  if either account does not exist
     * @throws com.bank.core.domain.exception.InsufficientFundsException if the source account
     *         has insufficient funds
     * @throws com.bank.core.domain.exception.AccountBlockedException    if either account is blocked
     * @throws com.bank.core.domain.exception.DuplicateTransactionException if the idempotency key
     *         maps to an in-flight request (lock contention guard)
     */
    Transaction transfer(TransferCommand command);
}
