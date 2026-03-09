package com.bank.core.application.port.out;

import com.bank.core.domain.model.Transaction;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port defining the persistence contract for {@link Transaction} entities.
 *
 * <p>Transactions are append-only ledger records; no update or delete operations
 * are exposed by this port.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface TransactionRepository {

    /**
     * Persists a new {@link Transaction} record to the store.
     *
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    Transaction save(Transaction transaction);

    /**
     * Finds a transaction by its unique identifier.
     *
     * @param transactionId the transaction UUID to look up
     * @return an {@link Optional} containing the transaction, or empty if not found
     */
    Optional<Transaction> findById(UUID transactionId);

    /**
     * Finds the most recent transaction associated with a given idempotency key.
     *
     * <p>Used to retrieve the cached outcome of a previously processed transfer when
     * an idempotent replay is detected.</p>
     *
     * @param idempotencyKey the idempotency key to search by
     * @return an {@link Optional} containing the matching transaction, or empty if not found
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
