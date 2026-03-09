package com.bank.core.infrastructure.out.persistence;

import com.bank.core.application.port.out.OutboxMessageRepository;
import com.bank.core.domain.model.OutboxMessage;
import com.bank.core.infrastructure.out.persistence.entity.OutboxMessageJpaEntity;
import com.bank.core.infrastructure.out.persistence.repository.SpringDataOutboxMessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbound persistence adapter implementing the {@link OutboxMessageRepository} port.
 *
 * <p>Handles persistence of {@link OutboxMessage} domain objects by mapping to/from
 * {@link OutboxMessageJpaEntity}. Used by both the transfer service (to atomically
 * insert outbox records) and the relay scheduler (to query and mark them published).</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class OutboxMessagePersistenceAdapter implements OutboxMessageRepository {

    private final SpringDataOutboxMessageRepository springDataRepo;

    /**
     * Constructs an {@code OutboxMessagePersistenceAdapter}.
     *
     * @param springDataRepo the Spring Data JPA repository for outbox entities
     */
    public OutboxMessagePersistenceAdapter(SpringDataOutboxMessageRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    /**
     * {@inheritDoc}
     *
     * @param message the outbox message to persist
     * @return the saved domain outbox message
     */
    @Override
    public OutboxMessage save(OutboxMessage message) {
        OutboxMessageJpaEntity entity = toEntity(message);
        OutboxMessageJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of unpublished {@link OutboxMessage} domain objects
     */
    @Override
    public List<OutboxMessage> findUnpublished() {
        return springDataRepo.findByPublishedFalse()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Maps a domain {@link OutboxMessage} to a JPA {@link OutboxMessageJpaEntity}.
     *
     * @param m the domain outbox message
     * @return the corresponding JPA entity
     */
    private OutboxMessageJpaEntity toEntity(OutboxMessage m) {
        return new OutboxMessageJpaEntity(
                m.getId(),
                m.getExchange(),
                m.getRoutingKey(),
                m.getPayload(),
                m.isPublished(),
                m.getCreatedAt(),
                m.getPublishedAt()
        );
    }

    /**
     * Maps a JPA {@link OutboxMessageJpaEntity} to a domain {@link OutboxMessage}.
     *
     * @param e the JPA entity
     * @return the corresponding domain outbox message
     */
    private OutboxMessage toDomain(OutboxMessageJpaEntity e) {
        return new OutboxMessage(
                e.getId(),
                e.getExchange(),
                e.getRoutingKey(),
                e.getPayload(),
                e.isPublished(),
                e.getCreatedAt(),
                e.getPublishedAt()
        );
    }
}
