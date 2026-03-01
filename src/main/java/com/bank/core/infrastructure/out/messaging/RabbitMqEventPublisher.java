package com.bank.core.infrastructure.out.messaging;

import com.bank.core.application.port.out.EventPublisherPort;
import com.bank.core.domain.Transaction;
import com.bank.core.infrastructure.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishTransactionCompletedEvent(Transaction transaction) {
        // Ideally this handles Outbox Pattern, to keep it concise, we publish directly.
        // The Transactional Outbox would be polled by a scheduler to publish.
        log.info("Publishing TransactionCompletedEvent for Transaction ID: {}", transaction.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, "transaction.completed", transaction);
    }
}
