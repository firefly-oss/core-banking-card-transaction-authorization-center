# Database Schema

This document details the database schema used in the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Tables](#tables)
  - [Authorization Request](#authorization-request)
  - [Authorization Decision](#authorization-decision)
  - [Authorization Hold](#authorization-hold)
  - [Spending Window](#spending-window)
  - [Limit Override](#limit-override)
  - [Risk Rule](#risk-rule)
  - [Risk Assessment](#risk-assessment)
- [Indexes](#indexes)
- [Database Configuration](#database-configuration)
- [Migration Strategy](#migration-strategy)
- [Performance Considerations](#performance-considerations)

## Overview

The Core Banking Card Transaction Authorization Center uses a PostgreSQL database with R2DBC for reactive database access. The schema is designed to efficiently store and retrieve transaction data while supporting the various processes involved in card authorization.

## Entity Relationship Diagram

```
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│ AuthorizationRequest│       │AuthorizationDecision│       │  AuthorizationHold  │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ requestId (PK)      │───┐   │ decisionId (PK)     │       │ holdId (PK)         │
│ maskedPan           │   │   │ requestId (FK)      │───┐   │ requestId (FK)      │
│ panHash             │   └──>│ cardId              │   │   │ cardId              │
│ expiryDate          │       │ accountId           │   │   │ accountId           │
│ merchantId          │       │ accountSpaceId      │   │   │ accountSpaceId      │
│ merchantName        │       │ decision            │   │   │ amount              │
│ channel             │       │ reasonCode          │   │   │ capturedAmount      │
│ mcc                 │       │ reasonMessage       │   │   │ currency            │
│ countryCode         │       │ approvedAmount      │   │   │ merchantId          │
│ transactionType     │       │ currency            │   │   │ merchantName        │
│ amount              │       │ authorizationCode   │   │   │ status              │
│ currency            │       │ riskScore           │   │   │ createdAt           │
│ timestamp           │       │ holdId (FK)         │───┘──>│ updatedAt           │
│ processed           │       │ timestamp           │       │ expiresAt           │
│ processedAt         │       └─────────────────────┘       └─────────────────────┘
└─────────────────────┘                                              │
                                                                     │
                                                                     │
┌─────────────────────┐       ┌─────────────────────┐                │
│   SpendingWindow    │       │    LimitOverride    │                │
├─────────────────────┤       ├─────────────────────┤                │
│ id (PK)             │       │ id (PK)             │                │
│ cardId              │<──────│ cardId              │                │
│ type                │       │ transactionLimit    │                │
│ date                │       │ dailyLimit          │                │
│ month               │       │ monthlyLimit        │                │
│ year                │       │ reason              │                │
│ totalAmount         │       │ createdBy           │                │
│ transactionCount    │       │ createdAt           │                │
│ lastUpdated         │       │ expiresAt           │                │
└─────────────────────┘       └─────────────────────┘                │
                                                                     │
                                                                     │
┌─────────────────────┐       ┌─────────────────────┐                │
│      RiskRule       │       │   RiskAssessment    │                │
├─────────────────────┤       ├─────────────────────┤                │
│ id (PK)             │       │ id (PK)             │                │
│ code                │       │ requestId (FK)      │────────────────┘
│ name                │       │ riskScore           │
│ description         │       │ riskLevel           │
│ category            │       │ recommendation      │
│ score               │       │ triggeredRules      │
│ enabled             │       │ timestamp           │
└─────────────────────┘       └─────────────────────┘
```

## Tables

### Authorization Request

Stores the details of each authorization request received by the system.

```sql
CREATE TABLE authorization_request (
    request_id BIGSERIAL PRIMARY KEY,
    masked_pan VARCHAR(19) NOT NULL,
    pan_hash VARCHAR(64) NOT NULL,
    expiry_date VARCHAR(5) NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    mcc VARCHAR(4) NOT NULL,
    country_code VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP
);
```

### Authorization Decision

Stores the authorization decisions made by the system.

```sql
CREATE TABLE authorization_decision (
    decision_id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    account_space_id BIGINT,
    decision VARCHAR(20) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_message VARCHAR(200) NOT NULL,
    approved_amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    authorization_code VARCHAR(10),
    risk_score INTEGER,
    hold_id BIGINT,
    timestamp TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_decision_request FOREIGN KEY (request_id) 
        REFERENCES authorization_request (request_id)
);
```

### Authorization Hold

Stores the authorization holds created for approved transactions.

```sql
CREATE TABLE authorization_hold (
    hold_id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    account_space_id BIGINT,
    amount DECIMAL(19, 4) NOT NULL,
    captured_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_hold_request FOREIGN KEY (request_id) 
        REFERENCES authorization_request (request_id)
);
```

### Spending Window

Stores the spending windows used for tracking daily and monthly limits.

```sql
CREATE TABLE spending_window (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL, -- DAILY, MONTHLY
    date DATE,
    month INTEGER,
    year INTEGER,
    total_amount DECIMAL(19, 4) NOT NULL,
    transaction_count INTEGER NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    
    CONSTRAINT unique_daily_window UNIQUE (card_id, type, date),
    CONSTRAINT unique_monthly_window UNIQUE (card_id, type, month, year)
);
```

### Limit Override

Stores temporary limit overrides for cards.

```sql
CREATE TABLE limit_override (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL,
    transaction_limit DECIMAL(19, 4),
    daily_limit DECIMAL(19, 4),
    monthly_limit DECIMAL(19, 4),
    reason VARCHAR(100) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
```

### Risk Rule

Stores the risk rules used for risk assessment.

```sql
CREATE TABLE risk_rule (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    score INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

### Risk Assessment

Stores the risk assessments performed for each transaction.

```sql
CREATE TABLE risk_assessment (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    recommendation VARCHAR(20) NOT NULL,
    triggered_rules JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_assessment_request FOREIGN KEY (request_id) 
        REFERENCES authorization_request (request_id)
);
```

## Indexes

The following indexes are created to optimize query performance:

```sql
-- Authorization Request indexes
CREATE INDEX idx_request_pan_hash ON authorization_request (pan_hash);
CREATE INDEX idx_request_timestamp ON authorization_request (timestamp);
CREATE INDEX idx_request_processed ON authorization_request (processed);

-- Authorization Decision indexes
CREATE INDEX idx_decision_request_id ON authorization_decision (request_id);
CREATE INDEX idx_decision_card_id ON authorization_decision (card_id);
CREATE INDEX idx_decision_account_id ON authorization_decision (account_id);
CREATE INDEX idx_decision_timestamp ON authorization_decision (timestamp);

-- Authorization Hold indexes
CREATE INDEX idx_hold_request_id ON authorization_hold (request_id);
CREATE INDEX idx_hold_card_id ON authorization_hold (card_id);
CREATE INDEX idx_hold_account_id ON authorization_hold (account_id);
CREATE INDEX idx_hold_status ON authorization_hold (status);
CREATE INDEX idx_hold_expires_at ON authorization_hold (expires_at);

-- Spending Window indexes
CREATE INDEX idx_spending_window_card_id ON spending_window (card_id);
CREATE INDEX idx_spending_window_date ON spending_window (date);
CREATE INDEX idx_spending_window_month_year ON spending_window (month, year);

-- Limit Override indexes
CREATE INDEX idx_limit_override_card_id ON limit_override (card_id);
CREATE INDEX idx_limit_override_expires_at ON limit_override (expires_at);

-- Risk Assessment indexes
CREATE INDEX idx_risk_assessment_request_id ON risk_assessment (request_id);
CREATE INDEX idx_risk_assessment_timestamp ON risk_assessment (timestamp);
```

## Database Configuration

The database connection is configured in the `application.yml` file:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/card_authorization
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 30
      max-idle-time: 30m
      validation-query: SELECT 1
```

For production environments, it's recommended to use connection pooling with appropriate settings:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 20
      max-size: 50
      max-idle-time: 30m
      max-life-time: 2h
      validation-query: SELECT 1
```

## Migration Strategy

The database schema is managed using Flyway migrations. Migration scripts are stored in the `src/main/resources/db/migration` directory and are executed automatically when the application starts.

Example migration script:

```sql
-- V1__initial_schema.sql
CREATE TABLE authorization_request (
    request_id BIGSERIAL PRIMARY KEY,
    masked_pan VARCHAR(19) NOT NULL,
    pan_hash VARCHAR(64) NOT NULL,
    -- other fields...
);

-- V2__add_account_space_id.sql
ALTER TABLE authorization_decision ADD COLUMN account_space_id BIGINT;
ALTER TABLE authorization_hold ADD COLUMN account_space_id BIGINT;
```

## Performance Considerations

The database schema is designed with performance in mind:

1. **Appropriate Indexes**: Indexes are created on frequently queried columns
2. **Partitioning**: For high-volume deployments, consider partitioning the `authorization_request` and `authorization_decision` tables by date
3. **Archiving Strategy**: Implement an archiving strategy for old data to maintain optimal performance
4. **Connection Pooling**: Use connection pooling with appropriate settings
5. **Query Optimization**: Use explain plans to optimize complex queries
6. **Reactive Access**: Use R2DBC for non-blocking database access

Example partitioning strategy:

```sql
CREATE TABLE authorization_request_partition (
    request_id BIGSERIAL,
    -- other fields...
    timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (request_id, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE TABLE authorization_request_y2023m01 PARTITION OF authorization_request_partition
    FOR VALUES FROM ('2023-01-01') TO ('2023-02-01');

CREATE TABLE authorization_request_y2023m02 PARTITION OF authorization_request_partition
    FOR VALUES FROM ('2023-02-01') TO ('2023-03-01');

-- Create partitions for other months...
```
