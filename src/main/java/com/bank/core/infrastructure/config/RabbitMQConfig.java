package com.bank.core.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Spring configuration for all RabbitMQ topology and listener infrastructure.
 *
 * <p>Declares the following AMQP topology:</p>
 * <pre>
 *  [banking.events] ──routing─key──▶ [notification.queue]    ──DLX──▶ [notification.queue.dlq]
 *  (DirectExchange)  ──routing─key──▶ [fraud-check.queue]     ──DLX──▶ [fraud-check.queue.dlq]
 * </pre>
 *
 * <p>Each primary queue is backed by a Dead Letter Queue (DLQ). If a listener exhausts
 * its configured retry budget (3 attempts with exponential back-off), the message is
 * automatically forwarded to the corresponding DLQ for manual inspection.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class RabbitMQConfig {

    @Value("${banking.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${banking.rabbitmq.queue.notification}")
    private String notificationQueue;

    @Value("${banking.rabbitmq.queue.fraud-check}")
    private String fraudCheckQueue;

    @Value("${banking.rabbitmq.routing-key.transaction-completed}")
    private String transactionCompletedKey;

    /** DLQ suffix appended to each primary queue name. */
    private static final String DLQ_SUFFIX = ".dlq";

    /** Dead-letter exchange name (reuses the default exchange routing by queue name). */
    private static final String DLX_NAME = "banking.events.dlx";

    // -----------------------------------------------------------------------
    // Exchange
    // -----------------------------------------------------------------------

    /**
     * Declares the primary {@code banking.events} {@link DirectExchange}.
     *
     * @return the configured exchange
     */
    @Bean
    public DirectExchange bankingEventsExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /**
     * Declares the dead-letter exchange used to route failed messages to DLQs.
     *
     * @return the dead-letter exchange
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME, true, false);
    }

    // -----------------------------------------------------------------------
    // Queues
    // -----------------------------------------------------------------------

    /**
     * Declares the {@code notification.queue} with a DLX configuration.
     *
     * @return the notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", notificationQueue + DLQ_SUFFIX)
                .build();
    }

    /**
     * Declares the {@code notification.queue.dlq} for failed notification messages.
     *
     * @return the notification dead-letter queue
     */
    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(notificationQueue + DLQ_SUFFIX).build();
    }

    /**
     * Declares the {@code fraud-check.queue} with a DLX configuration.
     *
     * @return the fraud-check queue
     */
    @Bean
    public Queue fraudCheckQueue() {
        return QueueBuilder.durable(fraudCheckQueue)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", fraudCheckQueue + DLQ_SUFFIX)
                .build();
    }

    /**
     * Declares the {@code fraud-check.queue.dlq} for failed fraud-check messages.
     *
     * @return the fraud-check dead-letter queue
     */
    @Bean
    public Queue fraudCheckDlq() {
        return QueueBuilder.durable(fraudCheckQueue + DLQ_SUFFIX).build();
    }

    // -----------------------------------------------------------------------
    // Bindings
    // -----------------------------------------------------------------------

    /**
     * Binds {@code notification.queue} to the primary exchange using the
     * transaction-completed routing key.
     *
     * @return the binding
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(bankingEventsExchange())
                .with(transactionCompletedKey);
    }

    /**
     * Binds {@code fraud-check.queue} to the primary exchange using the
     * transaction-completed routing key.
     *
     * @return the binding
     */
    @Bean
    public Binding fraudCheckBinding() {
        return BindingBuilder.bind(fraudCheckQueue())
                .to(bankingEventsExchange())
                .with(transactionCompletedKey);
    }

    /**
     * Binds {@code notification.queue.dlq} to the DLX.
     *
     * @return the DLQ binding
     */
    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq())
                .to(deadLetterExchange())
                .with(notificationQueue + DLQ_SUFFIX);
    }

    /**
     * Binds {@code fraud-check.queue.dlq} to the DLX.
     *
     * @return the DLQ binding
     */
    @Bean
    public Binding fraudCheckDlqBinding() {
        return BindingBuilder.bind(fraudCheckDlq())
                .to(deadLetterExchange())
                .with(fraudCheckQueue + DLQ_SUFFIX);
    }

    // -----------------------------------------------------------------------
    // Listener Container Factory with Retry
    // -----------------------------------------------------------------------

    /**
     * Configures a {@link SimpleRabbitListenerContainerFactory} with a stateless
     * retry interceptor (3 attempts, exponential back-off starting at 1 second).
     *
     * <p>After exhausting all retries, the message is nack'd without re-queue,
     * triggering the DLX routing configured on each primary queue.</p>
     *
     * @param connectionFactory the AMQP connection factory
     * @return the retryable listener container factory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory retryableContainerFactory(
            ConnectionFactory connectionFactory) {

        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .build();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // -----------------------------------------------------------------------
    // RabbitTemplate
    // -----------------------------------------------------------------------

    /**
     * Configures the {@link RabbitTemplate} to use JSON message conversion.
     *
     * @param connectionFactory the AMQP connection factory
     * @return the configured template
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
