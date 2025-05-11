# Hold Management

This document details the hold management process in the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Hold Lifecycle](#hold-lifecycle)
  - [Hold Creation](#hold-creation)
  - [Hold Capture](#hold-capture)
  - [Hold Release](#hold-release)
  - [Hold Expiration](#hold-expiration)
- [Hold States](#hold-states)
- [Balance Management](#balance-management)
- [Partial Captures](#partial-captures)
- [Scheduled Processing](#scheduled-processing)
- [API Endpoints](#api-endpoints)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)

## Overview

Hold management is a critical component of the card authorization process. When a transaction is authorized, funds are not immediately transferred but are instead placed on hold (reserved) to ensure they are available when the transaction is settled. The hold management process handles the entire lifecycle of these authorization holds.

## Hold Lifecycle

### Hold Creation

When a transaction is approved, an authorization hold is created to reserve the funds:

```java
public Mono<AuthorizationHoldDTO> createHold(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.info("Creating authorization hold for request: {}", request.getRequestId());
    
    AuthorizationHold hold = new AuthorizationHold();
    hold.setRequestId(request.getRequestId());
    hold.setCardId(cardDetails.getCardId());
    hold.setAccountId(cardDetails.getAccountId());
    hold.setAccountSpaceId(cardDetails.getAccountSpaceId());
    hold.setAmount(request.getAmount());
    hold.setCurrency(request.getCurrency());
    hold.setMerchantId(request.getMerchantId());
    hold.setMerchantName(request.getMerchantName());
    hold.setStatus("ACTIVE");
    hold.setCapturedAmount(BigDecimal.ZERO);
    hold.setCreatedAt(LocalDateTime.now());
    hold.setExpiresAt(LocalDateTime.now().plusHours(holdExpiryHours));
    
    return holdRepository.save(hold)
            .flatMap(savedHold -> 
                balanceService.reserveFunds(
                    savedHold.getAccountId(), 
                    savedHold.getAccountSpaceId(),
                    savedHold.getAmount(), 
                    savedHold.getCurrency()
                )
                .thenReturn(holdMapper.toDto(savedHold))
            )
            .doOnSuccess(h -> log.info("Successfully created hold: {}", h.getHoldId()))
            .doOnError(e -> log.error("Failed to create hold for request: {}", request.getRequestId(), e));
}
```

The hold creation process includes:

1. Creating a hold record with transaction details
2. Setting an expiration time for the hold
3. Reserving the funds in the customer's account
4. Returning the hold details to be included in the authorization decision

### Hold Capture

When a transaction is settled, the hold is captured to finalize the transfer of funds:

```java
public Mono<AuthorizationHoldDTO> captureHold(Long holdId, BigDecimal amount) {
    log.info("Capturing hold: {} for amount: {}", holdId, amount);
    
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        return Mono.error(new IllegalArgumentException("Capture amount must be positive"));
    }
    
    return holdRepository.findById(holdId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Hold not found: " + holdId)))
            .flatMap(hold -> {
                // Validate the hold status
                if (!"ACTIVE".equals(hold.getStatus())) {
                    return Mono.error(new IllegalStateException(
                            "Hold cannot be captured. Current status: " + hold.getStatus()));
                }
                
                // Validate the amount
                if (amount.compareTo(hold.getAmount()) > 0) {
                    return Mono.error(new IllegalArgumentException("Capture amount cannot exceed hold amount"));
                }
                
                // Update the hold
                AuthorizationHold updatedHold = holdMapper.updateCaptureStatus(
                        hold,
                        "CAPTURED",
                        amount,
                        LocalDateTime.now()
                );
                
                // If the capture amount is less than the hold amount, release the difference
                if (amount.compareTo(updatedHold.getAmount()) < 0) {
                    BigDecimal releaseAmount = updatedHold.getAmount().subtract(amount);
                    return balanceService.releaseFunds(
                            updatedHold.getAccountId(), 
                            updatedHold.getAccountSpaceId(),
                            releaseAmount, 
                            updatedHold.getCurrency()
                    )
                    .then(holdRepository.save(updatedHold))
                    .map(holdMapper::toDto);
                } else {
                    // Full capture, just save the hold
                    return holdRepository.save(updatedHold)
                            .map(holdMapper::toDto);
                }
            })
            .doOnSuccess(h -> log.info("Successfully captured hold: {}", h.getHoldId()))
            .doOnError(e -> log.error("Failed to capture hold: {}", holdId, e));
}
```

The hold capture process includes:

1. Validating that the hold exists and is in an ACTIVE state
2. Validating that the capture amount does not exceed the hold amount
3. Updating the hold status to CAPTURED
4. If the capture amount is less than the hold amount, releasing the difference
5. Saving the updated hold record

### Hold Release

When a transaction is cancelled or reversed, the hold is released to free up the funds:

```java
public Mono<BalanceSnapshotDTO> releaseHold(Long holdId) {
    log.info("Releasing hold: {}", holdId);
    
    return holdRepository.findById(holdId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Hold not found: " + holdId)))
            .flatMap(hold -> {
                // Validate the hold status
                if (!"ACTIVE".equals(hold.getStatus())) {
                    return Mono.error(new IllegalStateException(
                            "Hold cannot be released. Current status: " + hold.getStatus()));
                }
                
                // Update the hold
                AuthorizationHold updatedHold = holdMapper.updateCaptureStatus(
                        hold,
                        "RELEASED",
                        BigDecimal.ZERO,
                        LocalDateTime.now()
                );
                
                // Release the funds
                return balanceService.releaseFunds(
                        updatedHold.getAccountId(), 
                        updatedHold.getAccountSpaceId(),
                        updatedHold.getAmount(), 
                        updatedHold.getCurrency()
                )
                .flatMap(balanceSnapshot -> holdRepository.save(updatedHold)
                        .thenReturn(balanceSnapshot));
            })
            .doOnSuccess(b -> log.info("Successfully released hold: {}", holdId))
            .doOnError(e -> log.error("Failed to release hold: {}", holdId, e));
}
```

The hold release process includes:

1. Validating that the hold exists and is in an ACTIVE state
2. Updating the hold status to RELEASED
3. Releasing the reserved funds back to the customer's available balance
4. Saving the updated hold record
5. Returning the updated balance snapshot

### Hold Expiration

Holds that are not captured or released within a specified time period will expire:

```java
@Scheduled(fixedRate = 3600000) // Run every hour
public void processExpiredHolds() {
    log.info("Processing expired authorization holds");
    
    LocalDateTime now = LocalDateTime.now();
    
    holdRepository.findByStatusAndExpiresAtBefore("ACTIVE", now)
            .flatMap(hold -> {
                // Update the hold
                AuthorizationHold updatedHold = holdMapper.updateCaptureStatus(
                        hold,
                        "EXPIRED",
                        BigDecimal.ZERO,
                        now
                );
                
                // Release the funds
                return balanceService.releaseFunds(
                        updatedHold.getAccountId(), 
                        updatedHold.getAccountSpaceId(),
                        updatedHold.getAmount(), 
                        updatedHold.getCurrency()
                )
                .then(holdRepository.save(updatedHold))
                .map(holdMapper::toDto);
            })
            .doOnNext(hold -> log.info("Expired hold processed: {}", hold.getHoldId()))
            .doOnError(e -> log.error("Error processing expired holds", e))
            .subscribe();
}
```

The hold expiration process includes:

1. Finding all ACTIVE holds that have expired
2. Updating each hold's status to EXPIRED
3. Releasing the reserved funds back to the customer's available balance
4. Saving the updated hold records

## Hold States

Authorization holds can be in one of the following states:

1. **ACTIVE**: The hold is active and funds are reserved
2. **CAPTURED**: The hold has been captured (fully or partially) and funds have been transferred
3. **RELEASED**: The hold has been released and funds have been returned to the available balance
4. **EXPIRED**: The hold has expired and funds have been returned to the available balance

The state transitions are as follows:

```
ACTIVE ──────> CAPTURED
  │               
  │               
  ├─────────> RELEASED
  │               
  │               
  └─────────> EXPIRED
```

## Balance Management

The hold management process interacts with the balance service to manage funds:

1. **Reserve Funds**: When a hold is created, funds are moved from available balance to reserved balance
2. **Capture Funds**: When a hold is captured, funds are moved from reserved balance to posted balance
3. **Release Funds**: When a hold is released or expires, funds are moved from reserved balance back to available balance

```java
public interface BalanceService {
    Mono<BalanceSnapshotDTO> reserveFunds(Long accountId, Long accountSpaceId, BigDecimal amount, String currency);
    Mono<BalanceSnapshotDTO> releaseFunds(Long accountId, Long accountSpaceId, BigDecimal amount, String currency);
    Mono<BalanceSnapshotDTO> postFunds(Long accountId, Long accountSpaceId, BigDecimal amount, String currency);
    Mono<BalanceSnapshotDTO> checkBalance(Long accountId, Long accountSpaceId, String currency);
}
```

## Partial Captures

The system supports partial captures, where the settlement amount is less than the authorization amount:

1. The capture amount must be less than or equal to the hold amount
2. If the capture amount is less than the hold amount, the difference is released
3. The hold status is updated to CAPTURED regardless of whether it's a full or partial capture

This is common in scenarios like:
- Restaurant transactions where the final amount includes a tip
- Fuel purchases where the initial authorization is for a maximum amount
- Hotel stays where the final charges may be less than the initial authorization

## Scheduled Processing

The system includes scheduled tasks to process holds:

1. **Expired Hold Processing**: Runs every hour to process holds that have expired
2. **Hold Cleanup**: Runs daily to archive old hold records (older than 90 days)
3. **Hold Metrics**: Runs daily to generate metrics on hold creation, capture, and expiration

## API Endpoints

The system exposes several API endpoints for hold management:

1. **Create Hold**: `POST /api/v1/holds`
2. **Get Hold**: `GET /api/v1/holds/{holdId}`
3. **Capture Hold**: `POST /api/v1/holds/{holdId}/capture`
4. **Release Hold**: `POST /api/v1/holds/{holdId}/release`
5. **List Holds**: `GET /api/v1/holds?accountId={accountId}&status={status}`

Example request for capturing a hold:

```http
POST /api/v1/holds/123456789/capture
Content-Type: application/json

{
  "amount": 95.50,
  "currency": "USD",
  "reference": "SETTLEMENT-123456"
}
```

Example response:

```json
{
  "holdId": 123456789,
  "requestId": 987654321,
  "accountId": 456789123,
  "accountSpaceId": 789123456,
  "amount": 100.00,
  "capturedAmount": 95.50,
  "currency": "USD",
  "merchantId": "MERCH123456",
  "merchantName": "Example Merchant",
  "status": "CAPTURED",
  "createdAt": "2023-05-01T14:30:00Z",
  "updatedAt": "2023-05-02T10:15:00Z",
  "expiresAt": "2023-05-08T14:30:00Z"
}
```

## Error Handling

The hold management process includes comprehensive error handling:

1. **Hold Not Found**: Returns a 404 Not Found response
2. **Invalid Hold State**: Returns a 409 Conflict response with details about the current state
3. **Invalid Amount**: Returns a 400 Bad Request response with details about the validation error
4. **Balance Service Errors**: Returns a 500 Internal Server Error response with appropriate error details

## Database Schema

The authorization hold is stored in the database with the following schema:

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

CREATE INDEX idx_hold_account_id ON authorization_hold (account_id);
CREATE INDEX idx_hold_status ON authorization_hold (status);
CREATE INDEX idx_hold_expires_at ON authorization_hold (expires_at);
```
