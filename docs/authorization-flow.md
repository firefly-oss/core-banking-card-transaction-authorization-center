# Authorization Flow

This document provides a detailed explanation of the transaction authorization flow in the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Authorization Process](#authorization-process)
  - [Request Reception](#request-reception)
  - [Request Validation](#request-validation)
  - [Card Validation](#card-validation)
  - [Limit Validation](#limit-validation)
  - [Risk Assessment](#risk-assessment)
  - [Balance Check](#balance-check)
  - [Hold Creation](#hold-creation)
  - [Decision Formation](#decision-formation)
  - [Response Delivery](#response-delivery)
  - [Post-Processing](#post-processing)
- [Decision Types](#decision-types)
- [Error Handling](#error-handling)
- [Idempotency](#idempotency)
- [Performance Considerations](#performance-considerations)
- [Sequence Diagram](#sequence-diagram)

## Overview

The authorization flow is the core process of the system, responsible for evaluating transaction requests and making authorization decisions. The process follows a reactive programming model, allowing for high throughput and low latency.

## Authorization Process

### Request Reception

The authorization process begins when a transaction request is received through the API. The request is typically initiated by a payment terminal, ATM, or e-commerce platform.

```java
@PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public Mono<ResponseEntity<AuthorizationDecisionDTO>> authorizeTransaction(
        @Valid @RequestBody AuthorizationRequestDTO request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
) {
    log.info("Received authorization request: {}", request.getRequestId());
    
    // Process idempotency key if provided
    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
        request.setRequestId(Math.abs(idempotencyKey.hashCode()) % 900000000000L + 100000000000L);
    }
    
    return authorizationService.authorizeTransaction(request)
            .map(decision -> {
                HttpStatus status;
                switch (decision.getDecision()) {
                    case APPROVED:
                    case PARTIAL:
                        status = HttpStatus.OK;
                        break;
                    case DECLINED:
                        status = HttpStatus.UNPROCESSABLE_ENTITY;
                        break;
                    case CHALLENGE:
                        status = HttpStatus.ACCEPTED;
                        break;
                    default:
                        status = HttpStatus.OK;
                }
                return ResponseEntity.status(status).body(decision);
            });
}
```

### Request Validation

The system performs basic validation of the request format and required fields. This includes checking for:

- Required fields (PAN, amount, merchant ID, etc.)
- Valid format for fields (e.g., expiry date format)
- Reasonable values (e.g., positive amount)

If the request fails validation, the system immediately returns a declined decision with an appropriate reason code.

### Card Validation

The system validates the card by checking:

1. Card existence and status (active, blocked, frozen, etc.)
2. Card expiry date
3. Card type and capabilities (e.g., whether the card can be used for the requested transaction type)

This step involves retrieving card details from the card service:

```java
public Mono<CardDetailsDTO> getCardDetails(String panHash) {
    return cardServiceClient.getCardByPanHash(panHash)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Card not found")))
            .flatMap(card -> {
                if (card.getStatus() != CardStatus.ACTIVE) {
                    return Mono.error(new IllegalStateException("Card is not active"));
                }
                
                if (isCardExpired(card.getExpiryDate())) {
                    return Mono.error(new IllegalStateException("Card is expired"));
                }
                
                return Mono.just(card);
            });
}
```

### Limit Validation

The system checks the transaction against various limits:

1. **Transaction Limit**: Maximum amount allowed for a single transaction
2. **Daily Limit**: Maximum amount allowed in a 24-hour period
3. **Monthly Limit**: Maximum amount allowed in a calendar month
4. **Channel-specific Limits**: Limits specific to the transaction channel (POS, ATM, e-commerce)

The system maintains spending windows to track usage against these limits:

```java
public Mono<Boolean> validateLimits(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    return Mono.zip(
            validateTransactionLimit(request, cardDetails),
            validateDailyLimit(request, cardDetails),
            validateMonthlyLimit(request, cardDetails)
    ).map(tuple -> tuple.getT1() && tuple.getT2() && tuple.getT3());
}

private Mono<Boolean> validateDailyLimit(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    LocalDate today = LocalDate.now();
    
    return spendingWindowRepository.findByCardIdAndDateAndType(
            cardDetails.getCardId(), today, SpendingWindowType.DAILY
    ).switchIfEmpty(createDailySpendingWindow(cardDetails.getCardId(), today))
    .flatMap(window -> {
        BigDecimal newTotal = window.getTotalAmount().add(request.getAmount());
        
        if (newTotal.compareTo(cardDetails.getDailyLimit()) > 0) {
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

### Risk Assessment

The system performs risk assessment to detect potentially fraudulent transactions. This includes:

1. Checking for unusual transaction patterns
2. Evaluating the merchant risk
3. Considering the transaction location
4. Analyzing the transaction amount in context of the customer's usual behavior

The risk assessment produces a risk score and recommendation:

```java
public Mono<RiskAssessmentDTO> assessRisk(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    RiskAssessmentDTO assessment = RiskAssessmentDTO.builder()
            .requestId(request.getRequestId())
            .timestamp(LocalDateTime.now())
            .triggeredRules(new ArrayList<>())
            .build();
    
    // Apply various risk rules
    applyAmountBasedRules(assessment, request);
    applyLocationBasedRules(assessment, request, cardDetails);
    applyTimeBasedRules(assessment, request, cardDetails);
    applyMerchantBasedRules(assessment, request);
    
    // Calculate final risk score
    int finalScore = calculateFinalScore(assessment);
    assessment.setRiskScore(finalScore);
    
    // Determine risk level and recommendation
    if (finalScore >= declineThreshold) {
        assessment.setRiskLevel("HIGH");
        assessment.setRecommendation("DECLINE");
    } else if (finalScore >= challengeThreshold) {
        assessment.setRiskLevel("MEDIUM");
        assessment.setRecommendation("CHALLENGE");
    } else {
        assessment.setRiskLevel("LOW");
        assessment.setRecommendation("APPROVE");
    }
    
    return Mono.just(assessment);
}
```

### Balance Check

For transactions that require funds (purchases, withdrawals), the system checks if the account has sufficient available balance:

```java
public Mono<BalanceSnapshotDTO> checkBalance(Long accountId, BigDecimal amount, String currency) {
    return ledgerServiceClient.getAccountBalance(accountId, currency)
            .flatMap(balance -> {
                if (balance.getAvailableBalance().compareTo(amount) < 0) {
                    return Mono.error(new InsufficientFundsException("Insufficient funds for transaction"));
                }
                
                return Mono.just(balance);
            });
}
```

### Hold Creation

If all validations pass, the system creates an authorization hold to reserve the funds:

```java
public Mono<AuthorizationHoldDTO> createHold(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    AuthorizationHold hold = new AuthorizationHold();
    hold.setRequestId(request.getRequestId());
    hold.setCardId(cardDetails.getCardId());
    hold.setAccountId(cardDetails.getAccountId());
    hold.setAmount(request.getAmount());
    hold.setCurrency(request.getCurrency());
    hold.setMerchantId(request.getMerchantId());
    hold.setMerchantName(request.getMerchantName());
    hold.setStatus("ACTIVE");
    hold.setCreatedAt(LocalDateTime.now());
    hold.setExpiresAt(LocalDateTime.now().plusHours(holdExpiryHours));
    
    return holdRepository.save(hold)
            .flatMap(savedHold -> 
                balanceService.reserveFunds(savedHold.getAccountId(), savedHold.getAmount(), savedHold.getCurrency())
                    .thenReturn(holdMapper.toDto(savedHold))
            );
}
```

### Decision Formation

Based on the results of all validation steps, the system forms an authorization decision:

```java
private Mono<AuthorizationDecisionDTO> createDecision(
        AuthorizationRequestDTO request,
        CardDetailsDTO cardDetails,
        RiskAssessmentDTO riskAssessment,
        AuthorizationHoldDTO hold
) {
    AuthorizationDecisionDTO decision = AuthorizationDecisionDTO.builder()
            .requestId(request.getRequestId())
            .decisionId(UUID.randomUUID().toString())
            .cardId(cardDetails.getCardId())
            .accountId(cardDetails.getAccountId())
            .customerId(cardDetails.getCustomerId())
            .decision(AuthorizationDecisionType.APPROVED)
            .reasonCode(AuthorizationReasonCode.APPROVED_TRANSACTION)
            .reasonMessage("Transaction approved")
            .approvedAmount(request.getAmount())
            .currency(request.getCurrency())
            .authorizationCode(generateAuthCode())
            .riskScore(riskAssessment.getRiskScore())
            .holdId(hold != null ? hold.getHoldId() : null)
            .timestamp(LocalDateTime.now())
            .build();
    
    return decisionRepository.save(decisionMapper.toEntity(decision))
            .thenReturn(decision);
}
```

### Response Delivery

The final decision is returned to the requesting system:

```java
return ResponseEntity.status(status).body(decision);
```

### Post-Processing

After delivering the response, the system performs various asynchronous post-processing tasks:

1. Sending notifications (for declined transactions or suspicious activity)
2. Recording detailed audit logs
3. Collecting metrics
4. Updating customer profiles with transaction data

## Decision Types

The system supports several types of authorization decisions:

1. **APPROVED**: The transaction is fully approved for the requested amount
2. **PARTIAL**: The transaction is approved for a partial amount (less than requested)
3. **DECLINED**: The transaction is declined
4. **CHALLENGE**: Additional verification is required (e.g., 3D Secure)

## Error Handling

The authorization flow includes comprehensive error handling:

1. **Validation Errors**: Return a declined decision with appropriate reason code
2. **Service Unavailability**: Return a declined decision with a system error reason code
3. **Timeout**: Return a declined decision with a timeout reason code
4. **Unexpected Errors**: Log the error, return a declined decision with a system error reason code

## Idempotency

The system supports idempotent processing of authorization requests through the use of idempotency keys:

1. If an idempotency key is provided, the system generates a deterministic request ID
2. Before processing, the system checks if a decision already exists for the request ID
3. If a decision exists, the system returns the existing decision instead of processing the request again

## Performance Considerations

The authorization flow is designed for high performance:

1. **Reactive Programming**: Non-blocking I/O for all operations
2. **Parallel Processing**: Independent validation steps are performed in parallel
3. **Caching**: Frequently accessed data (e.g., card details, limits) is cached
4. **Database Optimization**: Efficient database queries and indexes
5. **Connection Pooling**: Optimized connection pools for external services

## Sequence Diagram

```
┌─────────┐          ┌─────────────┐          ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
│  Client │          │Authorization │          │    Card     │          │    Risk     │          │   Balance   │
│         │          │  Controller  │          │   Service   │          │   Service   │          │   Service   │
└────┬────┘          └──────┬──────┘          └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
     │                       │                        │                        │                        │
     │ Authorization Request │                        │                        │                        │
     │──────────────────────>│                        │                        │                        │
     │                       │                        │                        │                        │
     │                       │ Get Card Details       │                        │                        │
     │                       │───────────────────────>│                        │                        │
     │                       │                        │                        │                        │
     │                       │ Card Details           │                        │                        │
     │                       │<───────────────────────│                        │                        │
     │                       │                        │                        │                        │
     │                       │ Validate Limits        │                        │                        │
     │                       │─────────────────────────────────────────────────────────────────────────>│
     │                       │                        │                        │                        │
     │                       │ Limits Valid           │                        │                        │
     │                       │<─────────────────────────────────────────────────────────────────────────│
     │                       │                        │                        │                        │
     │                       │ Assess Risk            │                        │                        │
     │                       │───────────────────────────────────────────────>│                        │
     │                       │                        │                        │                        │
     │                       │ Risk Assessment        │                        │                        │
     │                       │<───────────────────────────────────────────────│                        │
     │                       │                        │                        │                        │
     │                       │ Check Balance          │                        │                        │
     │                       │─────────────────────────────────────────────────────────────────────────>│
     │                       │                        │                        │                        │
     │                       │ Balance Sufficient     │                        │                        │
     │                       │<─────────────────────────────────────────────────────────────────────────│
     │                       │                        │                        │                        │
     │                       │ Create Hold            │                        │                        │
     │                       │─────────────────────────────────────────────────────────────────────────>│
     │                       │                        │                        │                        │
     │                       │ Hold Created           │                        │                        │
     │                       │<─────────────────────────────────────────────────────────────────────────│
     │                       │                        │                        │                        │
     │ Authorization Decision│                        │                        │                        │
     │<──────────────────────│                        │                        │                        │
     │                       │                        │                        │                        │
```
