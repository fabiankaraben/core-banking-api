package com.bank.core.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain entity representing a completed or failed fund transfer between two accounts.
 *
 * <p>A {@code Transaction} is an immutable ledger record created at the moment a transfer
 * is attempted. The {@link TransactionStatus} captures the outcome, and any failure reason
 * is stored in the {@code failureReason} field for audit purposes.</p>
 *
 * <p>Like {@link Account}, this class is a pure domain object with no framework dependencies.
 * All persistence concerns are handled in the infrastructure layer.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class Transaction {

    /** Unique surrogate identifier for this transaction. */
    private final UUID id;

    /**
     * The idempotency key supplied by the caller for this transfer request.
     * Stored to enable duplicate detection and replay responses.
     */
    private final String idempotencyKey;

    /** The source (debit) account. */
    private final UUID sourceAccountId;

    /** The destination (credit) account. */
    private final UUID destinationAccountId;

    /** The amount transferred, expressed in the common currency of both accounts. */
    private final Money amount;

    /** The outcome of this transfer attempt. */
    private final TransactionStatus status;

    /**
     * Human-readable description of why the transfer failed.
     * {@code null} when {@link #status} is {@link TransactionStatus#COMPLETED}.
     */
    private final String failureReason;

    /** The moment at which this transaction was recorded. */
    private final Instant createdAt;

    /**
     * Full constructor for reconstituting a {@code Transaction} from storage.
     *
     * @param id                   the transaction UUID
     * @param idempotencyKey       the caller-supplied idempotency key
     * @param sourceAccountId      the debit account UUID
     * @param destinationAccountId the credit account UUID
     * @param amount               the transfer amount
     * @param status               the transaction outcome
     * @param failureReason        the failure description, or {@code null} on success
     * @param createdAt            the creation timestamp
     */
    public Transaction(UUID id, String idempotencyKey, UUID sourceAccountId,
                       UUID destinationAccountId, Money amount, TransactionStatus status,
                       String failureReason, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    /**
     * Factory method for recording a successful transfer.
     *
     * @param idempotencyKey       the caller-supplied idempotency key
     * @param sourceAccountId      the debit account UUID
     * @param destinationAccountId the credit account UUID
     * @param amount               the transfer amount
     * @return a new {@code Transaction} with status {@link TransactionStatus#COMPLETED}
     */
    public static Transaction completed(String idempotencyKey, UUID sourceAccountId,
                                        UUID destinationAccountId, Money amount) {
        return new Transaction(UUID.randomUUID(), idempotencyKey, sourceAccountId,
                destinationAccountId, amount, TransactionStatus.COMPLETED, null, Instant.now());
    }

    /**
     * Factory method for recording a failed transfer.
     *
     * @param idempotencyKey       the caller-supplied idempotency key
     * @param sourceAccountId      the debit account UUID
     * @param destinationAccountId the credit account UUID
     * @param amount               the transfer amount
     * @param reason               a human-readable failure description
     * @return a new {@code Transaction} with status {@link TransactionStatus#FAILED}
     */
    public static Transaction failed(String idempotencyKey, UUID sourceAccountId,
                                     UUID destinationAccountId, Money amount, String reason) {
        return new Transaction(UUID.randomUUID(), idempotencyKey, sourceAccountId,
                destinationAccountId, amount, TransactionStatus.FAILED, reason, Instant.now());
    }

    /**
     * Returns the transaction identifier.
     *
     * @return the transaction UUID
     */
    public UUID getId() { return id; }

    /**
     * Returns the caller-supplied idempotency key.
     *
     * @return the idempotency key string
     */
    public String getIdempotencyKey() { return idempotencyKey; }

    /**
     * Returns the source (debit) account identifier.
     *
     * @return the source account UUID
     */
    public UUID getSourceAccountId() { return sourceAccountId; }

    /**
     * Returns the destination (credit) account identifier.
     *
     * @return the destination account UUID
     */
    public UUID getDestinationAccountId() { return destinationAccountId; }

    /**
     * Returns the transfer amount.
     *
     * @return the {@link Money} amount
     */
    public Money getAmount() { return amount; }

    /**
     * Returns the transaction outcome.
     *
     * @return the {@link TransactionStatus}
     */
    public TransactionStatus getStatus() { return status; }

    /**
     * Returns the failure reason, if any.
     *
     * @return the failure reason string, or {@code null} for completed transactions
     */
    public String getFailureReason() { return failureReason; }

    /**
     * Returns the transaction creation timestamp.
     *
     * @return the creation {@link Instant}
     */
    public Instant getCreatedAt() { return createdAt; }
}
