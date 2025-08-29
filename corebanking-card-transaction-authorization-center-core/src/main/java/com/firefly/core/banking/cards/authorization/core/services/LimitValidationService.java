package com.firefly.core.banking.cards.authorization.core.services;

import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.LimitSnapshotDTO;
import reactor.core.publisher.Mono;

/**
 * Service for validating transaction limits.
 */
public interface LimitValidationService {

    /**
     * Validate that a transaction does not exceed any applicable limits.
     * This includes checking single transaction limits, daily limits, monthly limits,
     * and channel-specific limits.
     *
     * @param request The authorization request to validate
     * @param cardDetails The card details
     * @return A Mono emitting the limit snapshot if validation is successful, or an error if validation fails
     */
    Mono<LimitSnapshotDTO> validateLimits(AuthorizationRequestDTO request, CardDetailsDTO cardDetails);

    /**
     * Get the current limit snapshot for a card.
     *
     * @param cardId The ID of the card
     * @return A Mono emitting the current limit snapshot
     */
    Mono<LimitSnapshotDTO> getLimitSnapshot(Long cardId);

    /**
     * Update the spending counters for a card after a successful authorization.
     *
     * @param cardId The ID of the card
     * @param amount The amount of the transaction
     * @param channel The channel of the transaction
     * @return A Mono emitting the updated limit snapshot
     */
    Mono<LimitSnapshotDTO> updateSpendingCounters(Long cardId, java.math.BigDecimal amount, String channel);

    /**
     * Reverse the spending counters for a card after a reversal.
     *
     * @param cardId The ID of the card
     * @param amount The amount of the transaction to reverse
     * @param channel The channel of the transaction
     * @return A Mono emitting the updated limit snapshot
     */
    Mono<LimitSnapshotDTO> reverseSpendingCounters(Long cardId, java.math.BigDecimal amount, String channel);
}
