package com.bank.core.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity adapter for the {@code outbox_messages} table.
 *
 * <p>Supports the Transactional Outbox Pattern: records in this table are written
 * atomically with business data and later relayed to RabbitMQ by the
 * {@link com.bank.core.infrastructure.out.messaging.OutboxRelayScheduler}.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    /** Surrogate primary key. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The AMQP exchange name the message should be routed through. */
    @Column(name = "exchange", nullable = false, length = 255)
    private String exchange;

    /** The AMQP routing key. */
    @Column(name = "routing_key", nullable = false, length = 255)
    private String routingKey;

    /** The JSON-serialized message payload stored as {@code TEXT}. */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Whether this message has been delivered to the broker. */
    @Column(name = "published", nullable = false)
    private boolean published;

    /** The timestamp at which this record was inserted. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** The timestamp at which the relay published this record to the broker. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Default no-arg constructor required by the JPA specification.
     */
    protected OutboxMessageJpaEntity() {}

    /**
     * Full constructor used by the persistence adapter.
     *
     * @param id          the outbox message UUID
     * @param exchange    the target AMQP exchange
     * @param routingKey  the AMQP routing key
     * @param payload     the JSON payload
     * @param published   whether the message has been delivered
     * @param createdAt   the creation timestamp
     * @param publishedAt the publish timestamp, or {@code null}
     */
    public OutboxMessageJpaEntity(UUID id, String exchange, String routingKey, String payload,
                                   boolean published, Instant createdAt, Instant publishedAt) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.published = published;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    /** @return the outbox message UUID */
    public UUID getId() { return id; }

    /** @return the target AMQP exchange name */
    public String getExchange() { return exchange; }

    /** @return the AMQP routing key */
    public String getRoutingKey() { return routingKey; }

    /** @return the JSON payload */
    public String getPayload() { return payload; }

    /** @return whether the message has been published */
    public boolean isPublished() { return published; }

    /** @param published the new published flag */
    public void setPublished(boolean published) { this.published = published; }

    /** @return the creation timestamp */
    public Instant getCreatedAt() { return createdAt; }

    /** @return the publish timestamp, or {@code null} */
    public Instant getPublishedAt() { return publishedAt; }

    /** @param publishedAt the publish timestamp to set */
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
