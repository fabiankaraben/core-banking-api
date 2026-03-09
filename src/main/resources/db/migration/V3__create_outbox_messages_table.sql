-- =============================================================================
-- Migration V3: Create outbox_messages table
--
-- Implements the Transactional Outbox Pattern. Rows are inserted atomically
-- with the corresponding business data (account balance updates + transaction
-- record). The OutboxRelayScheduler polls `published = false` rows and
-- delivers them to RabbitMQ, then sets `published = true`.
-- =============================================================================

CREATE TABLE outbox_messages (
    id           UUID         NOT NULL,
    exchange     VARCHAR(255) NOT NULL,
    routing_key  VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,

    CONSTRAINT pk_outbox_messages PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_messages_unpublished
    ON outbox_messages (published)
    WHERE published = FALSE;
