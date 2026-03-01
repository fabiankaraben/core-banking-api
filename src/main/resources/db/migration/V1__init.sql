CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_reference VARCHAR(255) NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    source_account_id UUID REFERENCES accounts(id),
    destination_account_id UUID REFERENCES accounts(id),
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_accounts_customer_ref ON accounts(customer_reference);
CREATE INDEX idx_transactions_source_acc ON transactions(source_account_id);
CREATE INDEX idx_transactions_dest_acc ON transactions(destination_account_id);
CREATE INDEX idx_outbox_status ON outbox_messages(status) WHERE status = 'PENDING';
