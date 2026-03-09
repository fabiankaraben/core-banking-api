package com.bank.core.infrastructure.out.persistence.entity;

import com.bank.core.domain.model.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity adapter for the {@code transactions} table.
 *
 * <p>Transactions are append-only ledger records. Once committed to the database,
 * they are never modified or deleted, providing a complete and auditable history
 * of all fund movements in the system.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    /** Surrogate primary key; maps to the domain transaction {@code id}. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Caller-supplied idempotency key. Indexed in the database for efficient
     * duplicate-detection lookups.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    /** The account that was debited. */
    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    /** The account that was credited. */
    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    /** The transfer amount stored as {@code NUMERIC(19,4)}. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO 4217 currency code of the transfer. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** The transaction outcome. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TransactionStatus status;

    /** Human-readable failure description; {@code null} for successful transactions. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** The moment at which this transaction was recorded. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Default no-arg constructor required by the JPA specification.
     */
    protected TransactionJpaEntity() {}

    /**
     * Full constructor used by the persistence adapter when mapping from the domain object.
     *
     * @param id                   the transaction UUID
     * @param idempotencyKey       the caller-supplied idempotency key
     * @param sourceAccountId      the debit account UUID
     * @param destinationAccountId the credit account UUID
     * @param amount               the transfer amount
     * @param currency             the ISO 4217 currency code
     * @param status               the transaction outcome
     * @param failureReason        the failure description, or {@code null}
     * @param createdAt            the creation timestamp
     */
    public TransactionJpaEntity(UUID id, String idempotencyKey, UUID sourceAccountId,
                                 UUID destinationAccountId, BigDecimal amount, String currency,
                                 TransactionStatus status, String failureReason, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    /** @return the transaction UUID */
    public UUID getId() { return id; }

    /** @return the idempotency key */
    public String getIdempotencyKey() { return idempotencyKey; }

    /** @return the source account UUID */
    public UUID getSourceAccountId() { return sourceAccountId; }

    /** @return the destination account UUID */
    public UUID getDestinationAccountId() { return destinationAccountId; }

    /** @return the transfer amount */
    public BigDecimal getAmount() { return amount; }

    /** @return the ISO 4217 currency code */
    public String getCurrency() { return currency; }

    /** @return the transaction status */
    public TransactionStatus getStatus() { return status; }

    /** @return the failure reason, or {@code null} */
    public String getFailureReason() { return failureReason; }

    /** @return the creation timestamp */
    public Instant getCreatedAt() { return createdAt; }
}
