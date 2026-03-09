package com.bank.core.infrastructure.out.persistence.repository;

import com.bank.core.infrastructure.out.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TransactionJpaEntity}.
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    /**
     * Finds the transaction associated with the given idempotency key.
     *
     * <p>Used to retrieve and return the original transaction result during an
     * idempotent replay request.</p>
     *
     * @param idempotencyKey the idempotency key to search by
     * @return an {@link Optional} containing the matching entity, or empty if not found
     */
    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
