package com.firefly.core.banking.cards.authorization.core.services;

import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.RiskAssessmentDTO;
import reactor.core.publisher.Mono;

/**
 * Service for assessing the risk of a transaction.
 */
public interface RiskAssessmentService {

    /**
     * Assess the risk of a transaction.
     * This includes checking for suspicious patterns, unusual locations,
     * velocity checks, and other fraud indicators.
     *
     * @param request The authorization request to assess
     * @param cardDetails The card details
     * @return A Mono emitting the risk assessment
     */
    Mono<RiskAssessmentDTO> assessRisk(AuthorizationRequestDTO request, CardDetailsDTO cardDetails);

    /**
     * Check if a transaction should be challenged with additional authentication.
     *
     * @param riskAssessment The risk assessment
     * @return A Mono emitting true if the transaction should be challenged, false otherwise
     */
    Mono<Boolean> shouldChallenge(RiskAssessmentDTO riskAssessment);

    /**
     * Check if a transaction should be declined due to high risk.
     *
     * @param riskAssessment The risk assessment
     * @return A Mono emitting true if the transaction should be declined, false otherwise
     */
    Mono<Boolean> shouldDecline(RiskAssessmentDTO riskAssessment);

    /**
     * Get the risk score threshold for challenging a transaction.
     *
     * @return The risk score threshold
     */
    int getChallengeThreshold();

    /**
     * Get the risk score threshold for declining a transaction.
     *
     * @return The risk score threshold
     */
    int getDeclineThreshold();
}
