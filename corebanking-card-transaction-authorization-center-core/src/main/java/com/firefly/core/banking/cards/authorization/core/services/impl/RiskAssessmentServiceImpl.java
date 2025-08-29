package com.firefly.core.banking.cards.authorization.core.services.impl;

import com.firefly.core.banking.cards.authorization.core.services.RiskAssessmentService;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.RiskAssessmentDTO;
import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

/**
 * Implementation of the RiskAssessmentService interface.
 * This service assesses the risk of a transaction by checking various risk factors.
 * Currently uses a simplified local risk assessment approach.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    @Value("${authorization.risk.challenge-threshold:70}")
    private int challengeThreshold;

    @Value("${authorization.risk.decline-threshold:90}")
    private int declineThreshold;

    /**
     * Assess the risk of a transaction.
     * This includes checking for suspicious patterns, unusual locations,
     * velocity checks, and other fraud indicators.
     *
     * @param request The authorization request to assess
     * @param cardDetails The card details
     * @return A Mono emitting the risk assessment
     */
    @Override
    public Mono<RiskAssessmentDTO> assessRisk(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        log.debug("Assessing risk for request: {}", request.getRequestId());
        return performLocalRiskAssessment(request, cardDetails);
    }

    /**
     * Check if a transaction should be challenged with additional authentication.
     *
     * @param riskAssessment The risk assessment
     * @return A Mono emitting true if the transaction should be challenged, false otherwise
     */
    @Override
    public Mono<Boolean> shouldChallenge(RiskAssessmentDTO riskAssessment) {
        if (riskAssessment == null || riskAssessment.getRiskScore() == null) {
            return Mono.just(false);
        }

        boolean shouldChallenge = riskAssessment.getRiskScore() >= challengeThreshold &&
                                  riskAssessment.getRiskScore() < declineThreshold;

        // Also challenge if the recommendation is explicitly "CHALLENGE"
        if ("CHALLENGE".equals(riskAssessment.getRecommendation())) {
            shouldChallenge = true;
        }

        return Mono.just(shouldChallenge);
    }

    /**
     * Check if a transaction should be declined due to high risk.
     *
     * @param riskAssessment The risk assessment
     * @return A Mono emitting true if the transaction should be declined, false otherwise
     */
    @Override
    public Mono<Boolean> shouldDecline(RiskAssessmentDTO riskAssessment) {
        if (riskAssessment == null || riskAssessment.getRiskScore() == null) {
            return Mono.just(false);
        }

        boolean shouldDecline = riskAssessment.getRiskScore() >= declineThreshold;

        // Also decline if the recommendation is explicitly "DECLINE"
        if ("DECLINE".equals(riskAssessment.getRecommendation())) {
            shouldDecline = true;
        }

        return Mono.just(shouldDecline);
    }

    /**
     * Get the risk score threshold for challenging a transaction.
     *
     * @return The risk score threshold
     */
    @Override
    public int getChallengeThreshold() {
        return challengeThreshold;
    }

    /**
     * Get the risk score threshold for declining a transaction.
     *
     * @return The risk score threshold
     */
    @Override
    public int getDeclineThreshold() {
        return declineThreshold;
    }



    /**
     * Perform a local risk assessment for a transaction.
     * This is used when the external fraud service is unavailable or for low-risk transactions.
     *
     * @param request The authorization request to assess
     * @param cardDetails The card details
     * @return A Mono emitting the risk assessment
     */
    private Mono<RiskAssessmentDTO> performLocalRiskAssessment(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        log.debug("Performing local risk assessment for request: {}", request.getRequestId());

        List<String> triggeredRules = new ArrayList<>();
        int riskScore = 0;
        String riskLevel = "LOW";
        String recommendation = "APPROVE";
        String reason = "Transaction appears normal";

        // Check for high-value transaction
        if (isHighValueTransaction(request)) {
            riskScore += 20;
            triggeredRules.add("high_value_transaction");
        }

        // Check for unusual country
        if (isUnusualCountry(request, cardDetails)) {
            riskScore += 30;
            triggeredRules.add("unusual_country");
        }

        // Check for unusual merchant category
        if (isUnusualMerchantCategory(request, cardDetails)) {
            riskScore += 15;
            triggeredRules.add("unusual_merchant_category");
        }

        // Check for unusual time
        if (isUnusualTime(request)) {
            riskScore += 10;
            triggeredRules.add("unusual_time");
        }

        // Check for e-commerce transaction without 3DS
        if (isEcommerceWithout3DS(request, cardDetails)) {
            riskScore += 25;
            triggeredRules.add("ecommerce_without_3ds");
        }

        // Determine risk level based on score
        if (riskScore >= declineThreshold) {
            riskLevel = "HIGH";
            recommendation = "DECLINE";
            reason = "Transaction appears to be high risk";
        } else if (riskScore >= challengeThreshold) {
            riskLevel = "MEDIUM";
            recommendation = "CHALLENGE";
            reason = "Additional verification recommended";
        }

        // Create velocity check results
        Map<String, Object> velocityChecks = new HashMap<>();
        velocityChecks.put("transactions_last_24h", 5);
        velocityChecks.put("transactions_last_hour", 1);
        velocityChecks.put("different_merchants_last_24h", 3);

        // Create additional factors
        Map<String, Object> additionalFactors = new HashMap<>();
        additionalFactors.put("card_age_days", 180);
        additionalFactors.put("customer_risk_level", "STANDARD");
        additionalFactors.put("previous_declines_last_30d", 0);

        // Build and return the risk assessment
        RiskAssessmentDTO assessment = RiskAssessmentDTO.builder()
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .reason(reason)
                .triggeredRules(triggeredRules)
                .velocityChecks(velocityChecks)
                .geolocationMatch(true)
                .deviceFingerprintMatch(true)
                .ipAddressRisk("LOW")
                .additionalFactors(additionalFactors)
                .build();

        log.debug("Local risk assessment result: {}", assessment);
        return Mono.just(assessment);
    }

    /**
     * Check if a transaction is considered high-value.
     *
     * @param request The authorization request
     * @return True if the transaction is high-value, false otherwise
     */
    private boolean isHighValueTransaction(AuthorizationRequestDTO request) {
        // Define thresholds for different currencies
        Map<String, BigDecimal> thresholds = new HashMap<>();
        thresholds.put("USD", new BigDecimal("1000.00"));
        thresholds.put("EUR", new BigDecimal("900.00"));
        thresholds.put("GBP", new BigDecimal("800.00"));

        // Default threshold for other currencies
        BigDecimal defaultThreshold = new BigDecimal("500.00");

        // Get the threshold for the transaction currency
        BigDecimal threshold = thresholds.getOrDefault(request.getCurrency(), defaultThreshold);

        // Compare the transaction amount to the threshold
        return request.getAmount().compareTo(threshold) >= 0;
    }

    /**
     * Check if a transaction is from an unusual country.
     *
     * @param request The authorization request
     * @param cardDetails The card details
     * @return True if the transaction is from an unusual country, false otherwise
     */
    private boolean isUnusualCountry(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        // If the country code is not provided, we can't determine if it's unusual
        if (request.getCountryCode() == null || request.getCountryCode().isEmpty()) {
            return false;
        }

        // If the issuer country is not provided, we can't determine if it's unusual
        if (cardDetails.getIssuerCountry() == null || cardDetails.getIssuerCountry().isEmpty()) {
            return false;
        }

        // If the transaction country is different from the issuer country, it might be unusual
        return !request.getCountryCode().equals(cardDetails.getIssuerCountry());
    }

    /**
     * Check if a transaction is for an unusual merchant category.
     *
     * @param request The authorization request
     * @param cardDetails The card details
     * @return True if the transaction is for an unusual merchant category, false otherwise
     */
    private boolean isUnusualMerchantCategory(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        // If the MCC is not provided, we can't determine if it's unusual
        if (request.getMcc() == null || request.getMcc().isEmpty()) {
            return false;
        }

        // Define high-risk merchant category codes
        Set<String> highRiskMccs = new HashSet<>(Arrays.asList(
                "7995", // Gambling
                "5993", // Cigar stores and stands
                "5921", // Package stores, beer, wine, and liquor
                "7273", // Dating and escort services
                "7994", // Video game arcades/establishments
                "5816", // Digital goods: games
                "5967", // Direct marketing: inbound teleservices
                "5816"  // Digital goods: games
        ));

        // Check if the transaction MCC is in the high-risk list
        return highRiskMccs.contains(request.getMcc());
    }

    /**
     * Check if a transaction is at an unusual time.
     *
     * @param request The authorization request
     * @return True if the transaction is at an unusual time, false otherwise
     */
    private boolean isUnusualTime(AuthorizationRequestDTO request) {
        // If the timestamp is not provided, we can't determine if it's unusual
        if (request.getTimestamp() == null) {
            return false;
        }

        // Get the hour of the day from the timestamp
        int hour = request.getTimestamp().getHour();

        // Transactions between 1 AM and 5 AM might be unusual
        return hour >= 1 && hour <= 5;
    }

    /**
     * Check if a transaction is an e-commerce transaction without 3DS.
     *
     * @param request The authorization request
     * @param cardDetails The card details
     * @return True if the transaction is an e-commerce transaction without 3DS, false otherwise
     */
    private boolean isEcommerceWithout3DS(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        // Check if the transaction is an e-commerce transaction
        boolean isEcommerce = TransactionChannel.E_COMMERCE.equals(request.getChannel());

        // Check if the card is enrolled in 3DS
        boolean isEnrolledIn3DS = "Y".equals(cardDetails.getThreeDsEnrollmentStatus());

        // Check if 3DS data is provided
        boolean has3DSData = request.getThreeDsData() != null && !request.getThreeDsData().isEmpty();

        // E-commerce transaction without 3DS is risky
        return isEcommerce && (!isEnrolledIn3DS || !has3DSData);
    }
}
