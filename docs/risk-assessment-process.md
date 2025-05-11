# Risk Assessment Process

This document details the risk assessment process implemented in the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Risk Assessment Flow](#risk-assessment-flow)
- [Risk Rules](#risk-rules)
  - [Amount-Based Rules](#amount-based-rules)
  - [Location-Based Rules](#location-based-rules)
  - [Time-Based Rules](#time-based-rules)
  - [Merchant-Based Rules](#merchant-based-rules)
  - [Behavioral Rules](#behavioral-rules)
- [Risk Scoring](#risk-scoring)
- [Decision Thresholds](#decision-thresholds)
- [3D Secure Integration](#3d-secure-integration)
- [Risk Assessment Feedback Loop](#risk-assessment-feedback-loop)
- [Configuration](#configuration)

## Overview

The risk assessment process is a critical component of the authorization flow, designed to detect potentially fraudulent transactions and protect both the financial institution and its customers. The process uses a rule-based approach combined with scoring to evaluate the risk level of each transaction.

## Risk Assessment Flow

1. **Initialization**: Create a new risk assessment record for the transaction
2. **Rule Application**: Apply various risk rules to the transaction
3. **Score Calculation**: Calculate a final risk score based on triggered rules
4. **Recommendation Formation**: Determine the risk level and recommendation
5. **Decision Integration**: Incorporate the risk assessment into the authorization decision

```java
public Mono<RiskAssessmentDTO> assessRisk(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    log.info("Performing risk assessment for request: {}", request.getRequestId());
    
    // Initialize risk assessment
    RiskAssessmentDTO assessment = RiskAssessmentDTO.builder()
            .requestId(request.getRequestId())
            .timestamp(LocalDateTime.now())
            .triggeredRules(new ArrayList<>())
            .build();
    
    // Apply risk rules
    applyAmountBasedRules(assessment, request);
    applyLocationBasedRules(assessment, request, cardDetails);
    applyTimeBasedRules(assessment, request, cardDetails);
    applyMerchantBasedRules(assessment, request);
    applyBehavioralRules(assessment, request, cardDetails);
    
    // Calculate final score
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
    
    log.info("Risk assessment completed for request: {}. Score: {}, Recommendation: {}", 
            request.getRequestId(), finalScore, assessment.getRecommendation());
    
    return Mono.just(assessment);
}
```

## Risk Rules

The system applies various categories of risk rules to evaluate transactions:

### Amount-Based Rules

These rules evaluate the transaction amount in various contexts:

1. **High Value Transaction**: Triggers when the transaction amount exceeds a threshold
2. **Unusual Amount**: Triggers when the amount deviates significantly from the customer's typical transaction amount
3. **Round Amount**: Triggers for suspiciously round amounts (e.g., exactly $1000.00)
4. **Multiple Small Transactions**: Triggers when several small transactions occur in a short time period

```java
private void applyAmountBasedRules(RiskAssessmentDTO assessment, AuthorizationRequestDTO request) {
    BigDecimal amount = request.getAmount();
    
    // High value transaction
    if (amount.compareTo(new BigDecimal("1000.00")) > 0) {
        assessment.getTriggeredRules().add("high_value_transaction");
        assessment.setBaseScore(assessment.getBaseScore() + 10);
    }
    
    // Round amount
    if (amount.remainder(new BigDecimal("100.00")).compareTo(BigDecimal.ZERO) == 0 && 
            amount.compareTo(new BigDecimal("500.00")) >= 0) {
        assessment.getTriggeredRules().add("round_amount");
        assessment.setBaseScore(assessment.getBaseScore() + 5);
    }
    
    // Additional amount-based rules...
}
```

### Location-Based Rules

These rules evaluate the transaction location:

1. **High-Risk Country**: Triggers when the transaction occurs in a high-risk country
2. **Unusual Location**: Triggers when the transaction location differs significantly from the customer's usual locations
3. **Impossible Travel**: Triggers when transactions occur in different geographical locations within an impossible timeframe
4. **Cross-Border Transaction**: Triggers for transactions in a different country than the card's issuing country

```java
private void applyLocationBasedRules(RiskAssessmentDTO assessment, AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    String countryCode = request.getCountryCode();
    String issuerCountry = cardDetails.getIssuerCountry();
    
    // High-risk country
    if (highRiskCountries.contains(countryCode)) {
        assessment.getTriggeredRules().add("high_risk_country");
        assessment.setBaseScore(assessment.getBaseScore() + 20);
    }
    
    // Cross-border transaction
    if (!countryCode.equals(issuerCountry)) {
        assessment.getTriggeredRules().add("cross_border_transaction");
        assessment.setBaseScore(assessment.getBaseScore() + 10);
    }
    
    // Additional location-based rules...
}
```

### Time-Based Rules

These rules evaluate the timing of the transaction:

1. **Unusual Hour**: Triggers for transactions at unusual hours (e.g., 3 AM)
2. **Rapid Succession**: Triggers when multiple transactions occur in rapid succession
3. **First Transaction After Inactivity**: Triggers for the first transaction after a long period of inactivity
4. **Weekend/Holiday Transaction**: Triggers for transactions during weekends or holidays if unusual for the customer

```java
private void applyTimeBasedRules(RiskAssessmentDTO assessment, AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    LocalDateTime timestamp = request.getTimestamp();
    int hour = timestamp.getHour();
    
    // Unusual hour
    if (hour >= 0 && hour <= 5) {
        assessment.getTriggeredRules().add("unusual_hour");
        assessment.setBaseScore(assessment.getBaseScore() + 5);
    }
    
    // Weekend transaction
    DayOfWeek dayOfWeek = timestamp.getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
        assessment.getTriggeredRules().add("weekend_transaction");
        assessment.setBaseScore(assessment.getBaseScore() + 3);
    }
    
    // Additional time-based rules...
}
```

### Merchant-Based Rules

These rules evaluate the merchant involved in the transaction:

1. **High-Risk Merchant Category**: Triggers for transactions in high-risk merchant categories
2. **New Merchant**: Triggers when the customer transacts with a merchant for the first time
3. **Unusual Merchant for Customer**: Triggers when the merchant type differs from the customer's usual patterns
4. **Multiple Transactions with Same Merchant**: Triggers for multiple transactions with the same merchant in a short period

```java
private void applyMerchantBasedRules(RiskAssessmentDTO assessment, AuthorizationRequestDTO request) {
    String mcc = request.getMcc();
    String merchantId = request.getMerchantId();
    
    // High-risk merchant category
    if (highRiskMCCs.contains(mcc)) {
        assessment.getTriggeredRules().add("high_risk_merchant_category");
        assessment.setBaseScore(assessment.getBaseScore() + 15);
    }
    
    // Additional merchant-based rules...
}
```

### Behavioral Rules

These rules evaluate the transaction in the context of the customer's behavior:

1. **Deviation from Spending Pattern**: Triggers when the transaction deviates from the customer's typical spending pattern
2. **Unusual Transaction Type**: Triggers when the transaction type is unusual for the customer
3. **Velocity Check**: Triggers when the number or volume of transactions exceeds normal patterns
4. **Channel Anomaly**: Triggers when the transaction channel is unusual for the customer

```java
private void applyBehavioralRules(RiskAssessmentDTO assessment, AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
    TransactionChannel channel = request.getChannel();
    TransactionType type = request.getTransactionType();
    
    // Channel anomaly
    if (channel == TransactionChannel.E_COMMERCE && !cardDetails.isEcommerceEnabled()) {
        assessment.getTriggeredRules().add("channel_anomaly");
        assessment.setBaseScore(assessment.getBaseScore() + 25);
    }
    
    // Additional behavioral rules...
}
```

## Risk Scoring

The risk scoring process combines the results of all applied rules to calculate a final risk score:

1. Each rule contributes a certain number of points to the base score
2. The base score is adjusted based on the customer's risk profile
3. The final score is calculated by applying weights to different rule categories

```java
private int calculateFinalScore(RiskAssessmentDTO assessment) {
    int baseScore = assessment.getBaseScore();
    
    // Apply category weights
    int amountScore = calculateCategoryScore(assessment, "amount_based", 1.2);
    int locationScore = calculateCategoryScore(assessment, "location_based", 1.5);
    int timeScore = calculateCategoryScore(assessment, "time_based", 0.8);
    int merchantScore = calculateCategoryScore(assessment, "merchant_based", 1.3);
    int behavioralScore = calculateCategoryScore(assessment, "behavioral", 1.7);
    
    // Calculate final score
    int finalScore = baseScore + amountScore + locationScore + timeScore + merchantScore + behavioralScore;
    
    // Cap the score at 100
    return Math.min(finalScore, 100);
}
```

## Decision Thresholds

The system uses configurable thresholds to determine the appropriate action based on the risk score:

1. **Approve Threshold**: Scores below this threshold result in an "APPROVE" recommendation
2. **Challenge Threshold**: Scores between the approve and decline thresholds result in a "CHALLENGE" recommendation
3. **Decline Threshold**: Scores above this threshold result in a "DECLINE" recommendation

```yaml
authorization:
  risk:
    challenge-threshold: 70
    decline-threshold: 90
```

## 3D Secure Integration

For transactions that trigger a "CHALLENGE" recommendation, the system can integrate with 3D Secure:

1. The authorization decision includes a "CHALLENGE" status
2. The response includes a URL for the 3D Secure authentication
3. The merchant redirects the customer to the 3D Secure page
4. After authentication, the transaction is resubmitted with the authentication result

```java
public Mono<AuthorizationDecisionDTO> handleChallengeCompletion(
        Long requestId, 
        Map<String, String> challengeResult
) {
    return authorizationRequestRepository.findById(requestId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Authorization request not found")))
            .flatMap(request -> {
                String authenticationResult = challengeResult.get("authenticationResult");
                
                if ("SUCCESS".equals(authenticationResult)) {
                    // Continue with authorization with reduced risk score
                    return processAuthorizationWithReducedRisk(request);
                } else {
                    // Create declined decision
                    return createDeclinedDecision(request, AuthorizationReasonCode.AUTHENTICATION_FAILED);
                }
            });
}
```

## Risk Assessment Feedback Loop

The system implements a feedback loop to improve risk assessment over time:

1. **Transaction Outcome Recording**: Record the outcome of each transaction (successful, fraudulent, disputed)
2. **Rule Effectiveness Analysis**: Analyze which rules are most effective at identifying fraud
3. **Threshold Adjustment**: Periodically adjust thresholds based on false positive and false negative rates
4. **Rule Refinement**: Refine existing rules and add new rules based on emerging fraud patterns

## Configuration

The risk assessment process is highly configurable:

```yaml
authorization:
  risk:
    challenge-threshold: 70
    decline-threshold: 90
    high-risk-countries:
      - "XY"
      - "ZZ"
    high-risk-mccs:
      - "7995"  # Gambling
      - "5993"  # Cigar stores
    rules:
      high-value-transaction:
        enabled: true
        threshold: 1000.00
        score: 10
      unusual-hour:
        enabled: true
        start-hour: 0
        end-hour: 5
        score: 5
      # Additional rule configurations...
```

This configuration can be updated dynamically through the admin API, allowing for quick responses to emerging fraud patterns.
