# Limit Validation

This document details the limit validation process in the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Types of Limits](#types-of-limits)
  - [Transaction Limits](#transaction-limits)
  - [Daily Limits](#daily-limits)
  - [Monthly Limits](#monthly-limits)
  - [Channel-Specific Limits](#channel-specific-limits)
- [Spending Windows](#spending-windows)
- [Limit Validation Process](#limit-validation-process)
- [Limit Hierarchy](#limit-hierarchy)
- [Limit Overrides](#limit-overrides)
- [Limit Management](#limit-management)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)

## Overview

Limit validation is a critical component of the authorization process, designed to control spending and mitigate risk by enforcing various spending limits. The system validates each transaction against multiple types of limits before approving it.

## Types of Limits

### Transaction Limits

Transaction limits define the maximum amount allowed for a single transaction:

```java
public Mono<Boolean> validateTransactionLimit(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.debug("Validating transaction limit for request: {}", request.getRequestId());
    
    BigDecimal transactionLimit = cardDetails.getTransactionLimit();
    if (transactionLimit == null) {
        transactionLimit = defaultTransactionLimit;
    }
    
    // Apply channel-specific adjustments
    transactionLimit = adjustLimitForChannel(transactionLimit, request.getChannel());
    
    // Compare transaction amount against limit
    boolean isValid = request.getAmount().compareTo(transactionLimit) <= 0;
    
    if (!isValid) {
        log.info("Transaction limit exceeded. Amount: {}, Limit: {}, Request: {}", 
                request.getAmount(), transactionLimit, request.getRequestId());
    }
    
    return Mono.just(isValid);
}
```

Transaction limits can vary based on:
- Card type (e.g., standard, gold, platinum)
- Customer segment
- Transaction channel (e.g., POS, ATM, e-commerce)

### Daily Limits

Daily limits define the maximum amount that can be spent in a 24-hour period:

```java
public Mono<Boolean> validateDailyLimit(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.debug("Validating daily limit for request: {}", request.getRequestId());
    
    LocalDate today = LocalDate.now();
    
    return spendingWindowRepository.findByCardIdAndDateAndType(
            cardDetails.getCardId(), today, SpendingWindowType.DAILY
    ).switchIfEmpty(createDailySpendingWindow(cardDetails.getCardId(), today))
    .flatMap(window -> {
        BigDecimal dailyLimit = cardDetails.getDailyLimit();
        if (dailyLimit == null) {
            dailyLimit = defaultDailyLimit;
        }
        
        // Apply channel-specific adjustments
        dailyLimit = adjustLimitForChannel(dailyLimit, request.getChannel());
        
        // Calculate new total
        BigDecimal newTotal = window.getTotalAmount().add(request.getAmount());
        
        // Compare against limit
        boolean isValid = newTotal.compareTo(dailyLimit) <= 0;
        
        if (!isValid) {
            log.info("Daily limit exceeded. New total: {}, Limit: {}, Request: {}", 
                    newTotal, dailyLimit, request.getRequestId());
            return Mono.just(false);
        }
        
        // Update the spending window
        window.setTotalAmount(newTotal);
        window.setTransactionCount(window.getTransactionCount() + 1);
        window.setLastUpdated(LocalDateTime.now());
        
        return spendingWindowRepository.save(window)
                .thenReturn(true);
    });
}
```

Daily limits help control spending on a day-to-day basis and are particularly useful for:
- Limiting potential fraud damage
- Helping customers manage their spending
- Controlling cash withdrawals

### Monthly Limits

Monthly limits define the maximum amount that can be spent in a calendar month:

```java
public Mono<Boolean> validateMonthlyLimit(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.debug("Validating monthly limit for request: {}", request.getRequestId());
    
    LocalDate today = LocalDate.now();
    int month = today.getMonthValue();
    int year = today.getYear();
    
    return spendingWindowRepository.findByCardIdAndMonthAndYearAndType(
            cardDetails.getCardId(), month, year, SpendingWindowType.MONTHLY
    ).switchIfEmpty(createMonthlySpendingWindow(cardDetails.getCardId(), month, year))
    .flatMap(window -> {
        BigDecimal monthlyLimit = cardDetails.getMonthlyLimit();
        if (monthlyLimit == null) {
            monthlyLimit = defaultMonthlyLimit;
        }
        
        // Calculate new total
        BigDecimal newTotal = window.getTotalAmount().add(request.getAmount());
        
        // Compare against limit
        boolean isValid = newTotal.compareTo(monthlyLimit) <= 0;
        
        if (!isValid) {
            log.info("Monthly limit exceeded. New total: {}, Limit: {}, Request: {}", 
                    newTotal, monthlyLimit, request.getRequestId());
            return Mono.just(false);
        }
        
        // Update the spending window
        window.setTotalAmount(newTotal);
        window.setTransactionCount(window.getTransactionCount() + 1);
        window.setLastUpdated(LocalDateTime.now());
        
        return spendingWindowRepository.save(window)
                .thenReturn(true);
    });
}
```

Monthly limits are useful for:
- Long-term spending control
- Budget management
- Credit risk management

### Channel-Specific Limits

Channel-specific limits define different spending limits based on the transaction channel:

```java
private BigDecimal adjustLimitForChannel(BigDecimal baseLimit, TransactionChannel channel) {
    switch (channel) {
        case ATM:
            return baseLimit.multiply(atmLimitMultiplier);
        case E_COMMERCE:
            return baseLimit.multiply(ecommerceLimitMultiplier);
        case POS:
            return baseLimit.multiply(posLimitMultiplier);
        default:
            return baseLimit;
    }
}
```

Channel-specific limits allow for more granular control based on the risk profile of different channels:
- ATM withdrawals might have lower limits
- E-commerce transactions might have different limits than in-person transactions
- Contactless transactions might have lower limits than chip-and-PIN transactions

## Spending Windows

Spending windows are used to track spending against daily and monthly limits:

```java
public class SpendingWindow {
    @Id
    private Long id;
    
    private Long cardId;
    private SpendingWindowType type; // DAILY, MONTHLY
    
    // For daily windows
    private LocalDate date;
    
    // For monthly windows
    private Integer month;
    private Integer year;
    
    private BigDecimal totalAmount;
    private Integer transactionCount;
    private LocalDateTime lastUpdated;
    
    // Getters and setters
}
```

Spending windows are created on-demand when a transaction is processed:

```java
private Mono<SpendingWindow> createDailySpendingWindow(Long cardId, LocalDate date) {
    SpendingWindow window = new SpendingWindow();
    window.setCardId(cardId);
    window.setType(SpendingWindowType.DAILY);
    window.setDate(date);
    window.setTotalAmount(BigDecimal.ZERO);
    window.setTransactionCount(0);
    window.setLastUpdated(LocalDateTime.now());
    
    return spendingWindowRepository.save(window);
}

private Mono<SpendingWindow> createMonthlySpendingWindow(Long cardId, int month, int year) {
    SpendingWindow window = new SpendingWindow();
    window.setCardId(cardId);
    window.setType(SpendingWindowType.MONTHLY);
    window.setMonth(month);
    window.setYear(year);
    window.setTotalAmount(BigDecimal.ZERO);
    window.setTransactionCount(0);
    window.setLastUpdated(LocalDateTime.now());
    
    return spendingWindowRepository.save(window);
}
```

## Limit Validation Process

The limit validation process follows these steps:

1. Retrieve the card details with associated limits
2. Validate the transaction against the transaction limit
3. Validate the transaction against the daily limit
4. Validate the transaction against the monthly limit
5. If any validation fails, return a declined decision with the appropriate reason code

```java
public Mono<Boolean> validateLimits(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.info("Validating limits for request: {}", request.getRequestId());
    
    return Mono.zip(
            validateTransactionLimit(request, cardDetails),
            validateDailyLimit(request, cardDetails),
            validateMonthlyLimit(request, cardDetails)
    ).map(tuple -> {
        boolean transactionLimitValid = tuple.getT1();
        boolean dailyLimitValid = tuple.getT2();
        boolean monthlyLimitValid = tuple.getT3();
        
        if (!transactionLimitValid) {
            log.info("Transaction limit validation failed for request: {}", request.getRequestId());
            return false;
        }
        
        if (!dailyLimitValid) {
            log.info("Daily limit validation failed for request: {}", request.getRequestId());
            return false;
        }
        
        if (!monthlyLimitValid) {
            log.info("Monthly limit validation failed for request: {}", request.getRequestId());
            return false;
        }
        
        log.info("All limits validated successfully for request: {}", request.getRequestId());
        return true;
    });
}
```

## Limit Hierarchy

Limits follow a hierarchy, with more specific limits taking precedence over general limits:

1. **Customer-specific limits**: Limits set specifically for a customer
2. **Card-specific limits**: Limits set for a specific card
3. **Product-specific limits**: Limits set for a card product (e.g., gold card)
4. **Default limits**: System-wide default limits

```java
private BigDecimal getEffectiveLimit(CardDetailsDTO cardDetails, BigDecimal defaultLimit, LimitType limitType) {
    // Check for customer-specific limit
    if (cardDetails.getCustomerLimits() != null && cardDetails.getCustomerLimits().get(limitType) != null) {
        return cardDetails.getCustomerLimits().get(limitType);
    }
    
    // Check for card-specific limit
    switch (limitType) {
        case TRANSACTION:
            if (cardDetails.getTransactionLimit() != null) {
                return cardDetails.getTransactionLimit();
            }
            break;
        case DAILY:
            if (cardDetails.getDailyLimit() != null) {
                return cardDetails.getDailyLimit();
            }
            break;
        case MONTHLY:
            if (cardDetails.getMonthlyLimit() != null) {
                return cardDetails.getMonthlyLimit();
            }
            break;
    }
    
    // Check for product-specific limit
    if (cardDetails.getProductCode() != null) {
        BigDecimal productLimit = getProductLimit(cardDetails.getProductCode(), limitType);
        if (productLimit != null) {
            return productLimit;
        }
    }
    
    // Fall back to default limit
    return defaultLimit;
}
```

## Limit Overrides

In certain scenarios, limits can be temporarily overridden:

1. **Customer request**: A customer may request a temporary limit increase
2. **Travel notification**: Limits may be adjusted when a customer is traveling
3. **Special events**: Limits may be increased for special events (e.g., holiday shopping)

```java
public Mono<Boolean> checkForOverrides(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    return limitOverrideRepository.findActiveOverrideForCard(cardDetails.getCardId())
            .map(override -> {
                // Apply override to card limits
                if (override.getTransactionLimit() != null) {
                    cardDetails.setTransactionLimit(override.getTransactionLimit());
                }
                
                if (override.getDailyLimit() != null) {
                    cardDetails.setDailyLimit(override.getDailyLimit());
                }
                
                if (override.getMonthlyLimit() != null) {
                    cardDetails.setMonthlyLimit(override.getMonthlyLimit());
                }
                
                return true;
            })
            .defaultIfEmpty(true);
}
```

## Limit Management

Limits can be managed through the administrative API:

1. **View Limits**: Retrieve current limits for a card or customer
2. **Update Limits**: Update permanent limits for a card or customer
3. **Create Override**: Create a temporary limit override
4. **Delete Override**: Remove a temporary limit override

Example request to update limits:

```http
PUT /api/v1/admin/cards/123456789/limits
Content-Type: application/json

{
  "transactionLimit": 2000.00,
  "dailyLimit": 5000.00,
  "monthlyLimit": 20000.00,
  "channelLimits": {
    "ATM": {
      "transactionLimit": 500.00,
      "dailyLimit": 1000.00
    },
    "E_COMMERCE": {
      "transactionLimit": 1000.00,
      "dailyLimit": 3000.00
    }
  }
}
```

## Database Schema

The spending window is stored in the database with the following schema:

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

CREATE INDEX idx_spending_window_card_id ON spending_window (card_id);
CREATE INDEX idx_spending_window_date ON spending_window (date);
CREATE INDEX idx_spending_window_month_year ON spending_window (month, year);
```

The limit override is stored with the following schema:

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
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_override_card FOREIGN KEY (card_id) 
        REFERENCES card (id)
);

CREATE INDEX idx_limit_override_card_id ON limit_override (card_id);
CREATE INDEX idx_limit_override_expires_at ON limit_override (expires_at);
```

## API Endpoints

The system exposes several API endpoints for limit management:

1. **Get Card Limits**: `GET /api/v1/admin/cards/{cardId}/limits`
2. **Update Card Limits**: `PUT /api/v1/admin/cards/{cardId}/limits`
3. **Create Limit Override**: `POST /api/v1/admin/cards/{cardId}/limit-overrides`
4. **Get Limit Overrides**: `GET /api/v1/admin/cards/{cardId}/limit-overrides`
5. **Delete Limit Override**: `DELETE /api/v1/admin/limit-overrides/{overrideId}`
6. **Get Spending Summary**: `GET /api/v1/admin/cards/{cardId}/spending-summary`
