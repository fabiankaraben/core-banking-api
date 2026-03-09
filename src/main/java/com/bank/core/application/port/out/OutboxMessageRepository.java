package com.bank.core.application.port.out;

import com.bank.core.domain.model.OutboxMessage;
import java.util.List;

/**
 * Outbound port defining the persistence contract for {@link OutboxMessage} entities.
 *
 * <p>Supports the Transactional Outbox Pattern by allowing the application to atomically
 * write outbox records alongside business data, and by providing a query method for the
 * background relay scheduler to retrieve unprocessed messages.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface OutboxMessageRepository {

    /**
     * Persists a new {@link OutboxMessage} record to the store.
     *
     * @param message the outbox message to save
     * @return the saved outbox message
     */
    OutboxMessage save(OutboxMessage message);

    /**
     * Retrieves all outbox messages that have not yet been published to the broker.
     *
     * <p>The relay scheduler calls this method periodically to find pending messages.
     * After successfully delivering each message to RabbitMQ, it calls
     * {@link #save(OutboxMessage)} to persist the {@code published=true} state.</p>
     *
     * @return an unordered list of unpublished {@link OutboxMessage} instances
     */
    List<OutboxMessage> findUnpublished();
}
