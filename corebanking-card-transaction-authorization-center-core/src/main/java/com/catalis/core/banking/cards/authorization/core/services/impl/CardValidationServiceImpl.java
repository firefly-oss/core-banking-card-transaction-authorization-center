package com.catalis.core.banking.cards.authorization.core.services.impl;

import com.catalis.core.banking.cards.authorization.core.services.CardValidationService;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.catalis.core.banking.cards.authorization.interfaces.enums.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the CardValidationService interface.
 * This service validates card details and status.
 * Currently uses a simplified approach with mock data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardValidationServiceImpl implements CardValidationService {

    /**
     * Validate a card based on the authorization request.
     * This includes checking if the card exists, is active, and has not expired.
     *
     * @param request The authorization request containing card details
     * @return A Mono emitting the card details if validation is successful, or an error if validation fails
     */
    @Override
    public Mono<CardDetailsDTO> validateCard(AuthorizationRequestDTO request) {
        log.debug("Validating card for request: {}", request.getRequestId());

        // Get card details based on the request
        Mono<CardDetailsDTO> cardDetailsMono;
        if (request.getPanHash() != null && !request.getPanHash().isEmpty()) {
            cardDetailsMono = getCardDetails(request.getPanHash());
        } else if (request.getToken() != null && !request.getToken().isEmpty()) {
            cardDetailsMono = getCardDetailsByToken(request.getToken());
        } else {
            return Mono.error(new IllegalArgumentException("Neither PAN hash nor token provided"));
        }

        // Validate the card
        return cardDetailsMono
                .flatMap(cardDetails -> isCardActive(cardDetails)
                        .then(isCardNotExpired(cardDetails))
                        .thenReturn(cardDetails))
                .doOnSuccess(cardDetails -> log.debug("Card validation successful for request: {}", request.getRequestId()))
                .doOnError(e -> log.error("Card validation failed for request {}: {}", request.getRequestId(), e.getMessage()));
    }

    /**
     * Retrieve card details from the card service.
     *
     * @param panHash The hash of the card's PAN
     * @return A Mono emitting the card details
     */
    @Override
    public Mono<CardDetailsDTO> getCardDetails(String panHash) {
        log.debug("Getting card details for PAN hash: {}", panHash);

        // TODO: Implement actual card service integration
        // For now, return mock data
        return Mono.just(createMockCardDetails(panHash, null));
    }

    /**
     * Retrieve card details from the card service using a token.
     *
     * @param token The card token
     * @return A Mono emitting the card details
     */
    @Override
    public Mono<CardDetailsDTO> getCardDetailsByToken(String token) {
        log.debug("Getting card details for token: {}", token);

        // TODO: Implement actual card service integration
        // For now, return mock data
        return Mono.just(createMockCardDetails(null, token));
    }

    /**
     * Check if a card is active and can be used for transactions.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card is active, or an error if the card is not active
     */
    @Override
    public Mono<Boolean> isCardActive(CardDetailsDTO cardDetails) {
        if (CardStatus.ACTIVE.equals(cardDetails.getStatus())) {
            return Mono.just(true);
        } else {
            return Mono.error(new IllegalStateException("Card is not active. Current status: " + cardDetails.getStatus()));
        }
    }

    /**
     * Check if a card has expired.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card has not expired, or an error if the card has expired
     */
    @Override
    public Mono<Boolean> isCardNotExpired(CardDetailsDTO cardDetails) {
        if (cardDetails.getExpiryDate() != null && cardDetails.getExpiryDate().isAfter(LocalDate.now())) {
            return Mono.just(true);
        } else {
            return Mono.error(new IllegalStateException("Card has expired"));
        }
    }

    /**
     * Check if a card is enrolled in 3DS.
     *
     * @param cardDetails The card details to check
     * @return A Mono emitting true if the card is enrolled in 3DS, false otherwise
     */
    @Override
    public Mono<Boolean> isCardEnrolledIn3DS(CardDetailsDTO cardDetails) {
        return Mono.just("Y".equals(cardDetails.getThreeDsEnrollmentStatus()));
    }

    /**
     * Create mock card details for testing purposes.
     *
     * @param panHash The hash of the card's PAN
     * @param token The card token
     * @return The mock card details
     */
    private CardDetailsDTO createMockCardDetails(String panHash, String token) {
        // Generate a random ID for the card
        Long cardId = 100000000000L + (long) (Math.random() * 900000000000L);
        String maskedPan = "411111******1111";

        return CardDetailsDTO.builder()
                .cardId(cardId)
                .maskedPan(maskedPan)
                .panHash(panHash)
                .token(token)
                .bin("411111")
                .cardType("DEBIT")
                .cardBrand("VISA")
                .status(CardStatus.ACTIVE)
                .cardholderName("JOHN DOE")
                .expiryDate(LocalDate.now().plusYears(2))
                .issueDate(LocalDate.now().minusYears(1))
                .accountId(100000000000L + (long) (Math.random() * 900000000000L))
                .accountSpaceId(100000000000L + (long) (Math.random() * 900000000000L))
                .customerId(100000000000L + (long) (Math.random() * 900000000000L))
                .threeDsEnrollmentStatus("Y")
                .productCode("GOLD_REWARDS")
                .issuerCountry("USA")
                .build();
    }
}
