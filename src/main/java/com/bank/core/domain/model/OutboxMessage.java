package com.bank.core.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a pending outbound AMQP message stored in the
 * transactional outbox table.
 *
 * <p>The <strong>Transactional Outbox Pattern</strong> solves the dual-write problem:
 * rather than publishing directly to RabbitMQ inside a database transaction (which risks
 * inconsistency if the broker is unavailable), we atomically persist a serialized message
 * record to the {@code outbox_messages} table in the same Postgres transaction. A
 * background scheduler subsequently polls unpublished rows, publishes them to the broker,
 * and marks them as published.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class OutboxMessage {

    /** Unique identifier for this outbox record. */
    private final UUID id;

    /** The AMQP exchange the message should be routed through. */
    private final String exchange;

    /** The AMQP routing key that determines which queues receive the message. */
    private final String routingKey;

    /** The JSON-serialized message payload. */
    private final String payload;

    /**
     * Indicates whether this message has been successfully delivered to the broker.
     * Starts as {@code false}; set to {@code true} by the outbox relay scheduler.
     */
    private boolean published;

    /** The moment this outbox record was created (same as the enclosing transaction). */
    private final Instant createdAt;

    /** The moment this outbox record was published to the broker, or {@code null} if pending. */
    private Instant publishedAt;

    /**
     * Full constructor used when reconstituting a record from the persistence layer.
     *
     * @param id          the outbox message UUID
     * @param exchange    the target AMQP exchange
     * @param routingKey  the AMQP routing key
     * @param payload     the JSON payload
     * @param published   whether the message has been delivered to the broker
     * @param createdAt   the creation timestamp
     * @param publishedAt the publish timestamp, or {@code null}
     */
    public OutboxMessage(UUID id, String exchange, String routingKey, String payload,
                         boolean published, Instant createdAt, Instant publishedAt) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.published = published;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    /**
     * Factory method that creates a new, unpublished outbox message.
     *
     * @param exchange   the target AMQP exchange
     * @param routingKey the AMQP routing key
     * @param payload    the JSON payload
     * @return a new {@code OutboxMessage} with {@code published=false}
     */
    public static OutboxMessage create(String exchange, String routingKey, String payload) {
        return new OutboxMessage(UUID.randomUUID(), exchange, routingKey,
                payload, false, Instant.now(), null);
    }

    /**
     * Marks this outbox message as having been successfully published to the broker.
     */
    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    /**
     * Returns the outbox message identifier.
     *
     * @return the UUID
     */
    public UUID getId() { return id; }

    /**
     * Returns the target AMQP exchange name.
     *
     * @return the exchange name
     */
    public String getExchange() { return exchange; }

    /**
     * Returns the AMQP routing key.
     *
     * @return the routing key
     */
    public String getRoutingKey() { return routingKey; }

    /**
     * Returns the JSON-serialized message payload.
     *
     * @return the payload string
     */
    public String getPayload() { return payload; }

    /**
     * Returns whether the message has been delivered to the broker.
     *
     * @return {@code true} if published
     */
    public boolean isPublished() { return published; }

    /**
     * Returns the creation timestamp.
     *
     * @return the creation {@link Instant}
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Returns the publish timestamp.
     *
     * @return the publish {@link Instant}, or {@code null} if not yet published
     */
    public Instant getPublishedAt() { return publishedAt; }
}
