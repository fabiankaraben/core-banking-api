package com.bank.core.infrastructure.in.amqp;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter that consumes messages from the transaction event queues.
 *
 * <p>This component listens on two queues bound to the {@code banking.events} exchange:</p>
 * <ul>
 *   <li>{@code notification.queue} — simulates dispatching a customer notification
 *       (email, SMS, push notification) after a successful transfer.</li>
 *   <li>{@code fraud-check.queue} — simulates submitting the transaction to a
 *       fraud-detection pipeline for asynchronous risk assessment.</li>
 * </ul>
 *
 * <p>Both listeners are intentionally lightweight stubs. In a production system they
 * would delegate to dedicated notification and fraud-detection services. Dead-letter
 * queues (DLQ) are configured in {@link com.bank.core.infrastructure.config.RabbitMQConfig}
 * to capture messages that exhaust their retry budget.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private static final String METRIC_NOTIFICATION_PROCESSED = "banking.notification.processed";
    private static final String METRIC_FRAUD_CHECK_PROCESSED  = "banking.fraud.check.processed";

    private final MeterRegistry meterRegistry;

    /**
     * Constructs a {@code TransactionEventListener} with the required metrics registry.
     *
     * @param meterRegistry the Micrometer registry for consumer metrics
     */
    public TransactionEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Processes a transaction-completed event for the purpose of customer notification.
     *
     * <p>Consumes from {@code notification.queue}. The {@code containerFactory} named
     * {@code "retryableContainerFactory"} applies a back-off retry policy before
     * forwarding a failed message to the Dead Letter Queue.</p>
     *
     * @param payload the JSON payload of the completed transaction event
     */
    @RabbitListener(queues = "${banking.rabbitmq.queue.notification}",
                    containerFactory = "retryableContainerFactory")
    public void onNotification(String payload) {
        log.info("Notification consumer received event: {}", payload);
        meterRegistry.counter(METRIC_NOTIFICATION_PROCESSED).increment();
    }

    /**
     * Processes a transaction-completed event for the purpose of fraud analysis.
     *
     * <p>Consumes from {@code fraud-check.queue}. The {@code containerFactory} named
     * {@code "retryableContainerFactory"} applies a back-off retry policy before
     * forwarding a failed message to the Dead Letter Queue.</p>
     *
     * @param payload the JSON payload of the completed transaction event
     */
    @RabbitListener(queues = "${banking.rabbitmq.queue.fraud-check}",
                    containerFactory = "retryableContainerFactory")
    public void onFraudCheck(String payload) {
        log.info("Fraud-check consumer received event: {}", payload);
        meterRegistry.counter(METRIC_FRAUD_CHECK_PROCESSED).increment();
    }
}
