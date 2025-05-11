package com.catalis.core.banking.cards.authorization.core.services;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationDecisionDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import reactor.core.publisher.Mono;

/**
 * Service for processing card authorization requests.
 * This is the main orchestration service that coordinates the entire authorization flow.
 */
public interface AuthorizationService {

    /**
     * Process a card authorization request and make a decision.
     *
     * @param request The authorization request to process
     * @return A Mono emitting the authorization decision
     */
    Mono<AuthorizationDecisionDTO> authorizeTransaction(AuthorizationRequestDTO request);

    /**
     * Retrieve an authorization decision by its ID.
     *
     * @param decisionId The ID of the decision to retrieve
     * @return A Mono emitting the authorization decision
     */
    Mono<AuthorizationDecisionDTO> getDecisionById(Long decisionId);

    /**
     * Retrieve an authorization decision for a specific request.
     *
     * @param requestId The ID of the request to retrieve the decision for
     * @return A Mono emitting the authorization decision
     */
    Mono<AuthorizationDecisionDTO> getDecisionByRequestId(Long requestId);

    /**
     * Reverse an authorization (e.g., for a cancelled transaction).
     *
     * @param requestId The ID of the original authorization request
     * @param reason The reason for the reversal
     * @return A Mono emitting the updated authorization decision
     */
    Mono<AuthorizationDecisionDTO> reverseAuthorization(Long requestId, String reason);

    /**
     * Handle a 3DS challenge completion.
     *
     * @param requestId The ID of the original authorization request
     * @param challengeResult The result of the 3DS challenge
     * @return A Mono emitting the updated authorization decision
     */
    Mono<AuthorizationDecisionDTO> handleChallengeCompletion(Long requestId, String challengeResult);
}
