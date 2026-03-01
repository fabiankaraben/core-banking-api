package com.bank.core.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String customerReference;
    private Money balance;
    private AccountStatus status;
    private Long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE, BLOCKED
    }

    public Account(UUID id, String customerReference, Money balance, AccountStatus status, Long version,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.customerReference = customerReference;
        this.balance = balance;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Account create(String customerReference, String currency) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Account(UUID.randomUUID(), customerReference, Money.zero(currency), AccountStatus.ACTIVE, 0L, now,
                now);
    }

    public void deposit(Money amount) {
        checkActive();
        this.balance = this.balance.add(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    public void withdraw(Money amount) {
        checkActive();
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new AccountException.InsufficientFundsException();
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    private void checkActive() {
        if (this.status == AccountStatus.BLOCKED) {
            throw new AccountException.AccountBlockedException(this.id.toString());
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
