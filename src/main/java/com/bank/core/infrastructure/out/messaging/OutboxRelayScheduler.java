package com.bank.core.infrastructure.out.messaging;

import com.bank.core.application.port.out.OutboxMessageRepository;
import com.bank.core.application.port.out.MessagePublisherPort;
import com.bank.core.domain.model.OutboxMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled background component that implements the relay half of the
 * Transactional Outbox Pattern.
 *
 * <p>On each scheduling cycle (default: every 5 seconds), this component:</p>
 * <ol>
 *   <li>Queries the {@code outbox_messages} table for all unpublished records.</li>
 *   <li>Publishes each record's payload to RabbitMQ via {@link MessagePublisherPort}.</li>
 *   <li>Marks successfully delivered records as published and persists the change.</li>
 * </ol>
 *
 * <p>If RabbitMQ is unavailable or rejects a message, the exception is caught and
 * logged. The record remains {@code published=false} and will be retried on the next
 * cycle, providing at-least-once delivery semantics.</p>
 *
 * <p>Each outbox message is processed in its own independent transaction to prevent
 * a single failing message from blocking all other pending messages.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private static final String METRIC_RELAY_PUBLISHED = "banking.outbox.relay.published";
    private static final String METRIC_RELAY_FAILED = "banking.outbox.relay.failed";

    private final OutboxMessageRepository outboxMessageRepository;
    private final MessagePublisherPort messagePublisherPort;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs an {@code OutboxRelayScheduler} with its required dependencies.
     *
     * @param outboxMessageRepository the repository for querying and updating outbox records
     * @param messagePublisherPort    the adapter that publishes messages to RabbitMQ
     * @param meterRegistry           the Micrometer registry for relay metrics
     */
    public OutboxRelayScheduler(OutboxMessageRepository outboxMessageRepository,
                                MessagePublisherPort messagePublisherPort,
                                MeterRegistry meterRegistry) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.messagePublisherPort = messagePublisherPort;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Polls for unpublished outbox messages and relays them to RabbitMQ.
     *
     * <p>Runs on a fixed delay defined by the
     * {@code banking.outbox.relay.fixed-delay-ms} property (default: 5000 ms).
     * Each message is processed in its own {@link Transactional} boundary via
     * {@link #relayMessage(OutboxMessage)}.</p>
     */
    @Scheduled(fixedDelayString = "${banking.outbox.relay.fixed-delay-ms:5000}")
    public void relay() {
        List<OutboxMessage> pending = outboxMessageRepository.findUnpublished();
        if (!pending.isEmpty()) {
            log.debug("Outbox relay: found {} pending message(s)", pending.size());
            pending.forEach(this::relayMessage);
        }
    }

    /**
     * Publishes a single outbox message and marks it as delivered.
     *
     * <p>This method runs in its own transaction so that a failure on one message
     * does not prevent others from being processed.</p>
     *
     * @param message the outbox message to relay
     */
    @Transactional
    public void relayMessage(OutboxMessage message) {
        try {
            messagePublisherPort.publish(message);
            message.markPublished();
            outboxMessageRepository.save(message);
            meterRegistry.counter(METRIC_RELAY_PUBLISHED).increment();
            log.info("Outbox relay: published message id={}", message.getId());
        } catch (Exception ex) {
            meterRegistry.counter(METRIC_RELAY_FAILED).increment();
            log.error("Outbox relay: failed to publish message id={}: {}", message.getId(), ex.getMessage());
        }
    }
}
