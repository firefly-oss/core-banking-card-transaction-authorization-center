-- Initial schema for the card transaction authorization center

-- Create authorization_requests table
CREATE TABLE authorization_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    masked_pan VARCHAR(255) NOT NULL,
    pan_hash VARCHAR(255),
    token VARCHAR(255),
    expiry_date VARCHAR(10) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    merchant_name VARCHAR(255),
    channel VARCHAR(50) NOT NULL,
    mcc VARCHAR(10),
    country_code VARCHAR(3),
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    cryptogram TEXT,
    pin_data TEXT,
    three_ds_data TEXT,
    additional_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP
);

-- Create authorization_decisions table
CREATE TABLE authorization_decisions (
    id BIGSERIAL PRIMARY KEY,
    decision_id VARCHAR(255) NOT NULL UNIQUE,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    decision VARCHAR(50) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_message TEXT,
    approved_amount DECIMAL(19, 4),
    currency VARCHAR(3) NOT NULL,
    authorization_code VARCHAR(50),
    risk_score INTEGER,
    hold_id VARCHAR(255),
    ledger_entry_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    decision_path TEXT,
    network_response_data TEXT,
    additional_data TEXT,
    challenge_data TEXT,
    daily_limit DECIMAL(19, 4),
    daily_spent DECIMAL(19, 4),
    daily_remaining DECIMAL(19, 4),
    monthly_limit DECIMAL(19, 4),
    monthly_spent DECIMAL(19, 4),
    monthly_remaining DECIMAL(19, 4),
    account_id VARCHAR(255),
    available_balance_before DECIMAL(19, 4),
    available_balance_after DECIMAL(19, 4),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (request_id) REFERENCES authorization_requests(request_id)
);

-- Create authorization_holds table
CREATE TABLE authorization_holds (
    id BIGSERIAL PRIMARY KEY,
    hold_id VARCHAR(255) NOT NULL UNIQUE,
    request_id VARCHAR(255) NOT NULL,
    decision_id VARCHAR(255) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    card_id VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    merchant_name VARCHAR(255),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    original_amount DECIMAL(19, 4),
    original_currency VARCHAR(3),
    exchange_rate DECIMAL(19, 6),
    ledger_entry_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    capture_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    captured_amount DECIMAL(19, 4),
    captured_at TIMESTAMP,
    authorization_code VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (request_id) REFERENCES authorization_requests(request_id),
    FOREIGN KEY (decision_id) REFERENCES authorization_decisions(decision_id)
);

-- Create spending_windows table
CREATE TABLE spending_windows (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(255) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    window_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50),
    country_code VARCHAR(3),
    mcc VARCHAR(10),
    window_date DATE,
    window_month INTEGER,
    window_year INTEGER,
    limit_amount DECIMAL(19, 4) NOT NULL,
    spent_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    remaining_amount DECIMAL(19, 4) NOT NULL,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    last_transaction_id VARCHAR(255),
    last_transaction_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (card_id, window_type, channel, window_date),
    UNIQUE (card_id, window_type, window_month, window_year)
);

-- Create indexes for performance
CREATE INDEX idx_auth_req_masked_pan ON authorization_requests(masked_pan);
CREATE INDEX idx_auth_req_pan_hash ON authorization_requests(pan_hash);
CREATE INDEX idx_auth_req_token ON authorization_requests(token);
CREATE INDEX idx_auth_req_merchant_id ON authorization_requests(merchant_id);
CREATE INDEX idx_auth_req_timestamp ON authorization_requests(timestamp);

CREATE INDEX idx_auth_dec_request_id ON authorization_decisions(request_id);
CREATE INDEX idx_auth_dec_timestamp ON authorization_decisions(timestamp);
CREATE INDEX idx_auth_dec_expires_at ON authorization_decisions(expires_at);

CREATE INDEX idx_auth_hold_request_id ON authorization_holds(request_id);
CREATE INDEX idx_auth_hold_decision_id ON authorization_holds(decision_id);
CREATE INDEX idx_auth_hold_account_id ON authorization_holds(account_id);
CREATE INDEX idx_auth_hold_card_id ON authorization_holds(card_id);
CREATE INDEX idx_auth_hold_expires_at ON authorization_holds(expires_at);
CREATE INDEX idx_auth_hold_capture_status ON authorization_holds(capture_status);

CREATE INDEX idx_spending_card_id ON spending_windows(card_id);
CREATE INDEX idx_spending_account_id ON spending_windows(account_id);
CREATE INDEX idx_spending_window_date ON spending_windows(window_date);
CREATE INDEX idx_spending_window_month_year ON spending_windows(window_month, window_year);
