package com.bank.core.application.port.out;

import com.bank.core.domain.model.OutboxMessage;

/**
 * Outbound port defining the contract for publishing messages to the AMQP broker.
 *
 * <p>Used exclusively by the Outbox relay scheduler to deliver persisted
 * {@link OutboxMessage} records to RabbitMQ after the database transaction has committed.
 * Callers in the application layer never publish to the broker directly; they only
 * create {@link OutboxMessage} entries, thereby guaranteeing at-least-once delivery.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface MessagePublisherPort {

    /**
     * Publishes the payload of the given {@link OutboxMessage} to the configured
     * RabbitMQ exchange using the message's routing key.
     *
     * @param message the outbox message to publish; must not have been previously published
     * @throws org.springframework.amqp.AmqpException if the broker is unreachable or rejects
     *         the message (the outbox relay will retry on the next scheduling cycle)
     */
    void publish(OutboxMessage message);
}
