package com.bank.core.domain.model;

/**
 * Enumeration of the possible lifecycle states of a bank {@link Account}.
 *
 * <ul>
 *   <li>{@link #ACTIVE}  – the account is in good standing and can send and receive funds.</li>
 *   <li>{@link #BLOCKED} – the account has been administratively frozen; all debit and credit
 *       operations will be rejected until the account is re-activated.</li>
 * </ul>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AccountStatus {

    /**
     * The account is operational and may participate in fund transfers.
     */
    ACTIVE,

    /**
     * The account is frozen. No debit or credit operations are permitted while
     * the account holds this status.
     */
    BLOCKED
}
