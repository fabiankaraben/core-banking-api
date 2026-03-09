package com.bank.core.infrastructure.out.messaging;

import com.bank.core.application.port.out.MessagePublisherPort;
import com.bank.core.domain.model.OutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbound messaging adapter implementing the {@link MessagePublisherPort} using
 * Spring AMQP's {@link RabbitTemplate}.
 *
 * <p>Publishes the JSON payload of an {@link OutboxMessage} to the configured
 * RabbitMQ exchange with the message's routing key. This adapter is invoked
 * exclusively by the {@link OutboxRelayScheduler} and never directly by application
 * services, preserving the at-least-once delivery guarantee of the Transactional
 * Outbox Pattern.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class RabbitMQPublisherAdapter implements MessagePublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructs a {@code RabbitMQPublisherAdapter} with the provided {@link RabbitTemplate}.
     *
     * @param rabbitTemplate the Spring AMQP template used for message publication
     */
    public RabbitMQPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code exchange} and {@code routingKey} are taken directly from the
     * {@link OutboxMessage}, allowing different message types to be routed to
     * different queues without changing this adapter.</p>
     *
     * @param message the outbox message to publish
     */
    @Override
    public void publish(OutboxMessage message) {
        log.debug("Publishing outbox message id={} to exchange={} routingKey={}",
                message.getId(), message.getExchange(), message.getRoutingKey());
        rabbitTemplate.convertAndSend(
                message.getExchange(),
                message.getRoutingKey(),
                message.getPayload()
        );
        log.info("Published outbox message id={}", message.getId());
    }
}
