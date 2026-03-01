package com.bank.core.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final Money amount;
    private final TransactionStatus status;
    private final OffsetDateTime createdAt;

    public enum TransactionStatus {
        COMPLETED, FAILED
    }

    public Transaction(UUID id, UUID sourceAccountId, UUID destinationAccountId, Money amount, TransactionStatus status,
            OffsetDateTime createdAt) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Transaction create(UUID sourceAccountId, UUID destinationAccountId, Money amount) {
        return new Transaction(UUID.randomUUID(), sourceAccountId, destinationAccountId, amount,
                TransactionStatus.COMPLETED, OffsetDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
