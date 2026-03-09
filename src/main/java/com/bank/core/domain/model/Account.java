package com.bank.core.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * Core domain entity representing a bank account.
 *
 * <p>This is a pure Java domain object with no framework annotations. All persistence
 * mapping is handled in the infrastructure layer via a dedicated JPA entity adapter,
 * keeping the domain free from infrastructure concerns (Hexagonal Architecture).</p>
 *
 * <p>Optimistic concurrency control is enforced through the {@code version} field,
 * which maps to a {@code @Version}-annotated column in the JPA adapter. This prevents
 * lost-update anomalies when two concurrent transfer requests target the same account.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class Account {

    /** Unique surrogate identifier for this account. */
    private final UUID id;

    /** Reference to the customer who owns this account. */
    private final UUID customerId;

    /** Current available balance; always uses {@link Money#SCALE} decimal places. */
    private Money balance;

    /** ISO 4217 currency in which this account is denominated. */
    private final Currency currency;

    /** Lifecycle state of this account. */
    private AccountStatus status;

    /**
     * Optimistic locking token. Incremented by the persistence layer on each update.
     * A {@code null} value indicates the entity has never been persisted.
     */
    private Long version;

    /** Timestamp at which the account record was first created. */
    private final Instant createdAt;

    /** Timestamp of the most recent modification to this account. */
    private Instant updatedAt;

    /**
     * Constructs an {@code Account} with all fields supplied, typically used
     * when reconstituting an entity from the persistence layer.
     *
     * @param id         the unique account identifier
     * @param customerId the owning customer identifier
     * @param balance    the current balance
     * @param currency   the account currency
     * @param status     the current account status
     * @param version    the optimistic lock version
     * @param createdAt  the creation timestamp
     * @param updatedAt  the last-updated timestamp
     */
    public Account(UUID id, UUID customerId, Money balance, Currency currency,
                   AccountStatus status, Long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method for creating a brand-new account with an initial balance.
     *
     * <p>The resulting account is in {@link AccountStatus#ACTIVE} state with a
     * {@code null} version (not yet persisted) and the current time as both
     * {@code createdAt} and {@code updatedAt}.</p>
     *
     * @param customerId     the owning customer identifier
     * @param initialBalance the opening balance (must be non-negative)
     * @param currencyCode   the ISO 4217 currency code
     * @return a new, unpersisted {@code Account} instance
     */
    public static Account open(UUID customerId, BigDecimal initialBalance, String currencyCode) {
        Instant now = Instant.now();
        Currency currency = Currency.getInstance(currencyCode);
        Money balance = Money.of(initialBalance, currencyCode);
        return new Account(UUID.randomUUID(), customerId, balance, currency,
                AccountStatus.ACTIVE, null, now, now);
    }

    /**
     * Credits this account by adding the specified {@code amount} to the balance.
     *
     * @param amount the positive amount to add
     * @throws IllegalStateException    if the account is not {@link AccountStatus#ACTIVE}
     * @throws IllegalArgumentException if currencies differ or the amount is negative
     */
    public void credit(Money amount) {
        ensureActive();
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Debits this account by subtracting the specified {@code amount} from the balance.
     *
     * @param amount the positive amount to subtract
     * @throws IllegalStateException    if the account is not {@link AccountStatus#ACTIVE}
     * @throws IllegalArgumentException if currencies differ, the amount is negative,
     *                                  or the balance is insufficient
     */
    public void debit(Money amount) {
        ensureActive();
        if (!this.balance.isGreaterThanOrEqualTo(amount)) {
            throw new IllegalArgumentException(
                    "Insufficient funds: available=" + this.balance + ", requested=" + amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Blocks this account, preventing any further debit or credit operations.
     *
     * @throws IllegalStateException if the account is already blocked
     */
    public void block() {
        if (this.status == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Account is already blocked: " + this.id);
        }
        this.status = AccountStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    /**
     * Ensures this account is currently in {@link AccountStatus#ACTIVE} state.
     *
     * @throws IllegalStateException if the account is blocked
     */
    private void ensureActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: " + this.id);
        }
    }

    /**
     * Returns the unique account identifier.
     *
     * @return the account UUID
     */
    public UUID getId() { return id; }

    /**
     * Returns the owning customer identifier.
     *
     * @return the customer UUID
     */
    public UUID getCustomerId() { return customerId; }

    /**
     * Returns the current balance.
     *
     * @return the current {@link Money} balance
     */
    public Money getBalance() { return balance; }

    /**
     * Returns the account currency.
     *
     * @return the ISO 4217 {@link Currency}
     */
    public Currency getCurrency() { return currency; }

    /**
     * Returns the current account status.
     *
     * @return the {@link AccountStatus}
     */
    public AccountStatus getStatus() { return status; }

    /**
     * Returns the optimistic lock version.
     *
     * @return the version token, or {@code null} if not yet persisted
     */
    public Long getVersion() { return version; }

    /**
     * Returns the account creation timestamp.
     *
     * @return the creation {@link Instant}
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Returns the last-updated timestamp.
     *
     * @return the last-updated {@link Instant}
     */
    public Instant getUpdatedAt() { return updatedAt; }
}
