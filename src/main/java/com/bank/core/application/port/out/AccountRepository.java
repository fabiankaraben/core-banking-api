package com.bank.core.application.port.out;

import com.bank.core.domain.model.Account;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port defining the persistence contract for {@link Account} entities.
 *
 * <p>This interface is the boundary between the application layer and the infrastructure
 * persistence adapter. The application layer depends only on this abstraction; the actual
 * JPA/Postgres implementation lives in
 * {@code com.bank.core.infrastructure.out.persistence}.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AccountRepository {

    /**
     * Persists a new or updated {@link Account} to the store.
     *
     * <p>If the account already exists (matched by {@link Account#getId()}), the record
     * is updated; otherwise, a new row is inserted. Optimistic lock version increments
     * are handled transparently by the JPA adapter.</p>
     *
     * @param account the account to save
     * @return the saved account (may contain updated version or timestamps)
     */
    Account save(Account account);

    /**
     * Finds an account by its unique identifier.
     *
     * @param accountId the account UUID to look up
     * @return an {@link Optional} containing the account, or empty if not found
     */
    Optional<Account> findById(UUID accountId);

    /**
     * Acquires a pessimistic write lock on the account record and returns it.
     *
     * <p>Used in transfer operations to prevent concurrent balance mutations on the
     * same account row. The lock is released when the surrounding transaction commits
     * or rolls back.</p>
     *
     * @param accountId the account UUID to lock and retrieve
     * @return an {@link Optional} containing the locked account, or empty if not found
     */
    Optional<Account> findByIdWithLock(UUID accountId);
}
