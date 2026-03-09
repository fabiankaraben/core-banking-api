package com.bank.core.infrastructure.out.persistence.repository;

import com.bank.core.infrastructure.out.persistence.entity.OutboxMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OutboxMessageJpaEntity}.
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SpringDataOutboxMessageRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    /**
     * Retrieves all outbox message records that have not yet been relayed to the broker.
     *
     * <p>Polled by the {@link com.bank.core.infrastructure.out.messaging.OutboxRelayScheduler}
     * on a fixed schedule to find and publish pending messages.</p>
     *
     * @return a list of unpublished {@link OutboxMessageJpaEntity} records
     */
    List<OutboxMessageJpaEntity> findByPublishedFalse();
}
