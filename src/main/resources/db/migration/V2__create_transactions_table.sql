-- =============================================================================
-- Migration V2: Create transactions table
--
-- Append-only ledger of all fund transfer attempts. Records are never updated
-- or deleted, providing a complete and auditable history.
-- The `idempotency_key` column has a unique constraint to enable efficient
-- duplicate-transfer detection at the database level as a secondary guard.
-- =============================================================================

CREATE TABLE transactions (
    id                     UUID            NOT NULL,
    idempotency_key        VARCHAR(255)    NOT NULL,
    source_account_id      UUID            NOT NULL,
    destination_account_id UUID            NOT NULL,
    amount                 NUMERIC(19, 4)  NOT NULL,
    currency               VARCHAR(3)      NOT NULL,
    status                 VARCHAR(10)     NOT NULL,
    failure_reason         VARCHAR(500),
    created_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transactions              PRIMARY KEY (id),
    CONSTRAINT uq_transactions_idempotency  UNIQUE (idempotency_key),
    CONSTRAINT chk_transactions_amount      CHECK (amount > 0),
    CONSTRAINT chk_transactions_status      CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT fk_transactions_source       FOREIGN KEY (source_account_id)
                                                REFERENCES accounts (id),
    CONSTRAINT fk_transactions_destination  FOREIGN KEY (destination_account_id)
                                                REFERENCES accounts (id)
);

CREATE INDEX idx_transactions_source      ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination ON transactions (destination_account_id);
CREATE INDEX idx_transactions_created_at  ON transactions (created_at DESC);
