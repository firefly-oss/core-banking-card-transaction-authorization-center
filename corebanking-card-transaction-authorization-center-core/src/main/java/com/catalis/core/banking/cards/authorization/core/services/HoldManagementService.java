package com.catalis.core.banking.cards.authorization.core.services;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationHoldDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Service for managing authorization holds.
 */
public interface HoldManagementService {

    /**
     * Create a new authorization hold.
     *
     * @param request The authorization request
     * @param accountId The ID of the account
     * @param cardId The ID of the card
     * @param amount The amount to hold
     * @param currency The currency of the amount
     * @param authorizationCode The authorization code
     * @param decisionId The ID of the authorization decision
     * @return A Mono emitting the created authorization hold
     */
    Mono<AuthorizationHoldDTO> createHold(
            AuthorizationRequestDTO request,
            Long accountId,
            Long cardId,
            BigDecimal amount,
            String currency,
            String authorizationCode,
            Long decisionId);

    /**
     * Get an authorization hold by its ID.
     *
     * @param holdId The ID of the hold to retrieve
     * @return A Mono emitting the authorization hold
     */
    Mono<AuthorizationHoldDTO> getHoldById(Long holdId);

    /**
     * Get an authorization hold for a specific request.
     *
     * @param requestId The ID of the request to retrieve the hold for
     * @return A Mono emitting the authorization hold
     */
    Mono<AuthorizationHoldDTO> getHoldByRequestId(Long requestId);

    /**
     * Get all authorization holds for a specific account.
     *
     * @param accountId The ID of the account
     * @return A Flux emitting all authorization holds for the account
     */
    Flux<AuthorizationHoldDTO> getHoldsByAccountId(Long accountId);

    /**
     * Get all authorization holds for a specific account and account space.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space
     * @return A Flux emitting all authorization holds for the account and account space
     */
    Flux<AuthorizationHoldDTO> getHoldsByAccountIdAndAccountSpaceId(Long accountId, Long accountSpaceId);

    /**
     * Get all authorization holds for a specific card.
     *
     * @param cardId The ID of the card
     * @return A Flux emitting all authorization holds for the card
     */
    Flux<AuthorizationHoldDTO> getHoldsByCardId(Long cardId);

    /**
     * Capture an authorization hold (e.g., when a transaction is settled).
     *
     * @param holdId The ID of the hold to capture
     * @param amount The amount to capture (may be less than the hold amount for partial captures)
     * @return A Mono emitting the updated authorization hold
     */
    Mono<AuthorizationHoldDTO> captureHold(Long holdId, BigDecimal amount);

    /**
     * Release an authorization hold (e.g., when a transaction is cancelled).
     *
     * @param holdId The ID of the hold to release
     * @return A Mono emitting the updated authorization hold and balance snapshot
     */
    Mono<BalanceSnapshotDTO> releaseHold(Long holdId);

    /**
     * Process expired holds (e.g., holds that have not been captured within the expiration period).
     *
     * @return A Flux emitting the processed authorization holds
     */
    Flux<AuthorizationHoldDTO> processExpiredHolds();
}
