package com.catalis.core.banking.cards.authorization.core.services;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import reactor.core.publisher.Mono;

/**
 * Service for validating card details and status.
 */
public interface CardValidationService {

    /**
     * Validate a card based on the authorization request.
     * This includes checking if the card exists, is active, and has not expired.
     *
     * @param request The authorization request containing card details
     * @return A Mono emitting the card details if validation is successful, or an error if validation fails
     */
    Mono<CardDetailsDTO> validateCard(AuthorizationRequestDTO request);

    /**
     * Retrieve card details from the card service.
     *
     * @param panHash The hash of the card's PAN
     * @return A Mono emitting the card details
     */
    Mono<CardDetailsDTO> getCardDetails(String panHash);

    /**
     * Retrieve card details from the card service using a token.
     *
     * @param token The card token
     * @return A Mono emitting the card details
     */
    Mono<CardDetailsDTO> getCardDetailsByToken(String token);

    /**
     * Check if a card is active and can be used for transactions.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card is active, or an error if the card is not active
     */
    Mono<Boolean> isCardActive(CardDetailsDTO cardDetails);

    /**
     * Check if a card has expired.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card has not expired, or an error if the card has expired
     */
    Mono<Boolean> isCardNotExpired(CardDetailsDTO cardDetails);

    /**
     * Check if a card is enrolled in 3DS.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card is enrolled in 3DS, false otherwise
     */
    Mono<Boolean> isCardEnrolledIn3DS(CardDetailsDTO cardDetails);
}
