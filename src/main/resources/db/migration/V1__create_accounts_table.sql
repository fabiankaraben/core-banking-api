-- =============================================================================
-- Migration V1: Create accounts table
--
-- Stores all bank accounts. The `version` column enables JPA optimistic locking
-- to prevent lost-update anomalies during concurrent transfers.
-- =============================================================================

CREATE TABLE accounts (
    id          UUID            NOT NULL,
    customer_id UUID            NOT NULL,
    balance     NUMERIC(19, 4)  NOT NULL,
    currency    CHAR(3)         NOT NULL,
    status      VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_accounts            PRIMARY KEY (id),
    CONSTRAINT chk_accounts_balance   CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status    CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
