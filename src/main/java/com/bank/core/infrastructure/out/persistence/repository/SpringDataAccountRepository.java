package com.bank.core.infrastructure.out.persistence.repository;

import com.bank.core.infrastructure.out.persistence.entity.AccountJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AccountJpaEntity}.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository} plus
 * a custom query method that acquires a pessimistic write lock for safe concurrent
 * balance mutations during fund transfers.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {

    /**
     * Finds an account by ID and acquires a {@code SELECT FOR UPDATE} database lock.
     *
     * <p>The lock prevents other transactions from modifying the same account row
     * concurrently, acting as a second safety layer alongside the JPA
     * {@code @Version} optimistic locking mechanism.</p>
     *
     * @param id the account UUID to lock and retrieve
     * @return an {@link Optional} containing the locked {@link AccountJpaEntity}, or empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = :id")
    Optional<AccountJpaEntity> findByIdWithLock(@Param("id") UUID id);
}
