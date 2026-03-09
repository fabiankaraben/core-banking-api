package com.bank.core.domain.model;

/**
 * Enumeration of the possible outcomes of a financial {@link Transaction}.
 *
 * <ul>
 *   <li>{@link #COMPLETED} – the transfer was processed and both account balances
 *       have been updated atomically.</li>
 *   <li>{@link #FAILED}    – the transfer could not be completed (e.g., insufficient
 *       funds, blocked account); no balance changes were persisted.</li>
 * </ul>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum TransactionStatus {

    /**
     * The transaction completed successfully and the ledger has been updated.
     */
    COMPLETED,

    /**
     * The transaction failed. The reason is recorded on the associated
     * {@link Transaction} entity.
     */
    FAILED
}
