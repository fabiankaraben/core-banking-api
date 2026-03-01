package com.bank.core.infrastructure.in.messaging;

import com.bank.core.domain.Transaction;
import com.bank.core.infrastructure.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void processTransactionCompleted(Transaction transaction) {
        log.info("Received notification for completed transfer. ID: {}, Amount: {} {}",
                transaction.getId(), transaction.getAmount().amount(), transaction.getAmount().currency());

        // Simulating some notification logic like email, SMS, or pushing to another
        // system.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Notification processed successfully for transaction {}", transaction.getId());
    }
}
