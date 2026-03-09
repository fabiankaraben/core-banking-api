package com.bank.core.infrastructure.out.persistence.entity;

import com.bank.core.domain.model.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity adapter for the {@code accounts} table.
 *
 * <p>This class is an infrastructure concern and is never exposed to the application
 * or domain layers. The {@link AccountPersistenceAdapter} translates between this
 * entity and the {@link com.bank.core.domain.model.Account} domain object.</p>
 *
 * <p>The {@code version} field participates in optimistic locking: JPA automatically
 * appends a {@code WHERE version = ?} clause to every {@code UPDATE} statement, causing
 * a {@code OptimisticLockException} if a concurrent modification has already incremented
 * the version.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    /** Surrogate primary key; maps to the domain {@code id}. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Foreign key reference to the owning customer. */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Current balance stored as {@code NUMERIC(19,4)}. */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** ISO 4217 currency code stored as a fixed-length string. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Account lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccountStatus status;

    /** Optimistic locking version counter; managed by JPA. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Record creation timestamp. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp of the last modification. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default no-arg constructor required by the JPA specification.
     */
    protected AccountJpaEntity() {}

    /**
     * Full constructor used by the persistence adapter when mapping from the domain object.
     *
     * @param id         the account UUID
     * @param customerId the owning customer UUID
     * @param balance    the current balance
     * @param currency   the ISO 4217 currency code
     * @param status     the account status
     * @param version    the optimistic lock version
     * @param createdAt  the creation timestamp
     * @param updatedAt  the last-updated timestamp
     */
    public AccountJpaEntity(UUID id, UUID customerId, BigDecimal balance, String currency,
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

    /** @return the account UUID */
    public UUID getId() { return id; }

    /** @return the customer UUID */
    public UUID getCustomerId() { return customerId; }

    /** @return the current balance */
    public BigDecimal getBalance() { return balance; }

    /** @param balance the new balance to set */
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    /** @return the ISO 4217 currency code */
    public String getCurrency() { return currency; }

    /** @return the account status */
    public AccountStatus getStatus() { return status; }

    /** @param status the new account status */
    public void setStatus(AccountStatus status) { this.status = status; }

    /** @return the optimistic lock version */
    public Long getVersion() { return version; }

    /** @return the creation timestamp */
    public Instant getCreatedAt() { return createdAt; }

    /** @return the last-updated timestamp */
    public Instant getUpdatedAt() { return updatedAt; }

    /** @param updatedAt the new last-updated timestamp */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
