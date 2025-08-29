package com.firefly.core.banking.cards.authorization.core.services.impl;

import com.firefly.core.banking.cards.authorization.core.services.BalanceService;
import com.firefly.core.banking.cards.authorization.core.services.HoldManagementService;
import com.firefly.core.banking.cards.authorization.core.mappers.AuthorizationHoldMapper;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationHoldDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import com.firefly.core.banking.cards.authorization.models.entities.AuthorizationHold;
import com.firefly.core.banking.cards.authorization.models.repositories.AuthorizationHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Implementation of the HoldManagementService interface.
 * This service manages authorization holds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HoldManagementServiceImpl implements HoldManagementService {

    private final AuthorizationHoldRepository holdRepository;
    private final AuthorizationHoldMapper holdMapper;
    private final BalanceService balanceService;

    @Value("${authorization.hold.default-expiry-days:7}")
    private int defaultExpiryDays;

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
    @Override
    public Mono<AuthorizationHoldDTO> createHold(
            AuthorizationRequestDTO request,
            Long accountId,
            Long cardId,
            BigDecimal amount,
            String currency,
            String authorizationCode,
            Long decisionId) {
        log.debug("Creating hold for request: {}", request.getRequestId());

        // Reserve the funds
        return balanceService.reserveFunds(accountId, amount, currency)
                .flatMap(balanceSnapshot -> {
                    // Create the hold entity
                    AuthorizationHoldDTO holdDTO = AuthorizationHoldDTO.builder()
                            .holdId(generateRandomId())
                            .requestId(request.getRequestId())
                            .decisionId(decisionId)
                            .accountId(accountId)
                            .accountSpaceId(balanceSnapshot.getAccountSpaceId())
                            .cardId(cardId)
                            .merchantId(request.getMerchantId())
                            .merchantName(request.getMerchantName())
                            .amount(amount)
                            .currency(currency)
                            .originalAmount(balanceSnapshot.getOriginalAmount())
                            .originalCurrency(balanceSnapshot.getOriginalCurrency())
                            .exchangeRate(balanceSnapshot.getExchangeRate())
                            .authorizationCode(authorizationCode)
                            .createdAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plusDays(defaultExpiryDays))
                            .captureStatus("PENDING")
                            .build();

                    // Convert to entity and save
                    AuthorizationHold holdEntity = holdMapper.toEntity(holdDTO);
                    return holdRepository.save(holdEntity)
                            .map(holdMapper::toDto);
                });
    }

    /**
     * Get an authorization hold by its ID.
     *
     * @param holdId The ID of the hold to retrieve
     * @return A Mono emitting the authorization hold
     */
    @Override
    public Mono<AuthorizationHoldDTO> getHoldById(Long holdId) {
        log.debug("Getting hold by ID: {}", holdId);
        return holdRepository.findByHoldId(holdId)
                .map(holdMapper::toDto);
    }

    /**
     * Get an authorization hold for a specific request.
     *
     * @param requestId The ID of the request to retrieve the hold for
     * @return A Mono emitting the authorization hold
     */
    @Override
    public Mono<AuthorizationHoldDTO> getHoldByRequestId(Long requestId) {
        log.debug("Getting hold by request ID: {}", requestId);
        return holdRepository.findByRequestId(requestId)
                .map(holdMapper::toDto);
    }

    /**
     * Get all authorization holds for a specific account.
     *
     * @param accountId The ID of the account
     * @return A Flux emitting all authorization holds for the account
     */
    @Override
    public Flux<AuthorizationHoldDTO> getHoldsByAccountId(Long accountId) {
        log.debug("Getting holds by account ID: {}", accountId);
        return holdRepository.findByAccountId(accountId)
                .map(holdMapper::toDto);
    }

    /**
     * Get all authorization holds for a specific account and account space.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space
     * @return A Flux emitting all authorization holds for the account and account space
     */
    @Override
    public Flux<AuthorizationHoldDTO> getHoldsByAccountIdAndAccountSpaceId(Long accountId, Long accountSpaceId) {
        log.debug("Getting holds by account ID: {} and account space ID: {}", accountId, accountSpaceId);
        return holdRepository.findByAccountIdAndAccountSpaceId(accountId, accountSpaceId)
                .map(holdMapper::toDto);
    }

    /**
     * Get all authorization holds for a specific card.
     *
     * @param cardId The ID of the card
     * @return A Flux emitting all authorization holds for the card
     */
    @Override
    public Flux<AuthorizationHoldDTO> getHoldsByCardId(Long cardId) {
        log.debug("Getting holds by card ID: {}", cardId);
        return holdRepository.findByCardId(cardId)
                .map(holdMapper::toDto);
    }

    /**
     * Capture an authorization hold (e.g., when a transaction is settled).
     *
     * @param holdId The ID of the hold to capture
     * @param amount The amount to capture (may be less than the hold amount for partial captures)
     * @return A Mono emitting the updated authorization hold
     */
    @Override
    public Mono<AuthorizationHoldDTO> captureHold(Long holdId, BigDecimal amount) {
        log.debug("Capturing hold: {}, amount: {}", holdId, amount);

        return holdRepository.findByHoldId(holdId)
                .flatMap(hold -> {
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
                        return balanceService.releaseFunds(updatedHold.getAccountId(), releaseAmount, updatedHold.getCurrency())
                                .then(holdRepository.save(updatedHold))
                                .map(holdMapper::toDto);
                    } else {
                        // Full capture, just save the hold
                        return holdRepository.save(updatedHold)
                                .map(holdMapper::toDto);
                    }
                });
    }

    /**
     * Release an authorization hold (e.g., when a transaction is cancelled).
     *
     * @param holdId The ID of the hold to release
     * @return A Mono emitting the updated authorization hold and balance snapshot
     */
    @Override
    public Mono<BalanceSnapshotDTO> releaseHold(Long holdId) {
        log.debug("Releasing hold: {}", holdId);

        return holdRepository.findByHoldId(holdId)
                .flatMap(hold -> {
                    // Update the hold
                    AuthorizationHold updatedHold = holdMapper.updateCaptureStatus(
                            hold,
                            "RELEASED",
                            BigDecimal.ZERO,
                            LocalDateTime.now()
                    );

                    // Release the funds
                    return balanceService.releaseFunds(updatedHold.getAccountId(), updatedHold.getAmount(), updatedHold.getCurrency())
                            .flatMap(balanceSnapshot -> holdRepository.save(updatedHold)
                                    .thenReturn(balanceSnapshot));
                });
    }

    /**
     * Process expired holds (e.g., holds that have not been captured within the expiration period).
     *
     * @return A Flux emitting the processed authorization holds
     */
    @Override
    public Flux<AuthorizationHoldDTO> processExpiredHolds() {
        log.debug("Processing expired holds");

        LocalDateTime now = LocalDateTime.now();

        return holdRepository.findByExpiresAtBeforeAndCaptureStatus(now, "PENDING")
                .flatMap(hold -> {
                    // Update the hold
                    AuthorizationHold updatedHold = holdMapper.updateCaptureStatus(
                            hold,
                            "EXPIRED",
                            BigDecimal.ZERO,
                            now
                    );

                    // Release the funds
                    return balanceService.releaseFunds(updatedHold.getAccountId(), updatedHold.getAmount(), updatedHold.getCurrency())
                            .then(holdRepository.save(updatedHold))
                            .map(holdMapper::toDto);
                });
    }

    /**
     * Generate a random ID for entities.
     *
     * @return The random ID
     */
    private Long generateRandomId() {
        Random random = new Random();
        // Generate a positive long value between 100000000000L and 999999999999L (12 digits)
        return 100000000000L + (long) (random.nextDouble() * 900000000000L);
    }
}
