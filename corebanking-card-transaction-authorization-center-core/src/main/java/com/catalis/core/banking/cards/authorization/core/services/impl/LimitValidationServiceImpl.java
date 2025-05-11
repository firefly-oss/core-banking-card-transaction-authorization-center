package com.catalis.core.banking.cards.authorization.core.services.impl;

import com.catalis.core.banking.cards.authorization.core.services.LimitValidationService;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.LimitSnapshotDTO;
import com.catalis.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.catalis.core.banking.cards.authorization.models.entities.SpendingWindow;
import com.catalis.core.banking.cards.authorization.models.repositories.SpendingWindowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implementation of the LimitValidationService interface.
 * This service validates transaction limits and manages spending windows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LimitValidationServiceImpl implements LimitValidationService {

    private final SpendingWindowRepository spendingWindowRepository;

    @Value("${authorization.limits.default-daily:5000.00}")
    private BigDecimal defaultDailyLimit;

    @Value("${authorization.limits.default-monthly:20000.00}")
    private BigDecimal defaultMonthlyLimit;

    @Value("${authorization.limits.default-single-transaction:2000.00}")
    private BigDecimal defaultSingleTransactionLimit;

    @Value("${authorization.limits.default-atm-daily:1000.00}")
    private BigDecimal defaultAtmDailyLimit;

    @Value("${authorization.limits.default-contactless:100.00}")
    private BigDecimal defaultContactlessLimit;

    @Value("${authorization.limits.default-online:3000.00}")
    private BigDecimal defaultOnlineLimit;

    /**
     * Validate that a transaction does not exceed any applicable limits.
     * This includes checking single transaction limits, daily limits, monthly limits,
     * and channel-specific limits.
     *
     * @param request The authorization request to validate
     * @param cardDetails The card details
     * @return A Mono emitting the limit snapshot if validation is successful, or an error if validation fails
     */
    @Override
    public Mono<LimitSnapshotDTO> validateLimits(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        log.debug("Validating limits for request: {}", request.getRequestId());

        // Get the current limit snapshot
        return getLimitSnapshot(cardDetails.getCardId())
                .flatMap(limitSnapshot -> {
                    // Validate single transaction limit
                    if (request.getAmount().compareTo(limitSnapshot.getSingleTransactionLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction amount exceeds single transaction limit"));
                    }

                    // Validate channel-specific limits
                    if (TransactionChannel.ATM.equals(request.getChannel()) &&
                            request.getAmount().compareTo(limitSnapshot.getAtmDailyLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction amount exceeds ATM daily limit"));
                    }

                    if (TransactionChannel.CONTACTLESS.equals(request.getChannel()) &&
                            request.getAmount().compareTo(limitSnapshot.getContactlessLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction amount exceeds contactless limit"));
                    }

                    if (TransactionChannel.E_COMMERCE.equals(request.getChannel()) &&
                            request.getAmount().compareTo(limitSnapshot.getOnlineLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction amount exceeds online limit"));
                    }

                    // Validate daily limit
                    BigDecimal newDailySpent = limitSnapshot.getDailySpent().add(request.getAmount());
                    if (newDailySpent.compareTo(limitSnapshot.getDailyLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction would exceed daily spending limit"));
                    }

                    // Validate monthly limit
                    BigDecimal newMonthlySpent = limitSnapshot.getMonthlySpent().add(request.getAmount());
                    if (newMonthlySpent.compareTo(limitSnapshot.getMonthlyLimit()) > 0) {
                        return Mono.error(new IllegalStateException("Transaction would exceed monthly spending limit"));
                    }

                    // All validations passed
                    return Mono.just(limitSnapshot);
                });
    }

    /**
     * Get the current limit snapshot for a card.
     *
     * @param cardId The ID of the card
     * @return A Mono emitting the current limit snapshot
     */
    @Override
    public Mono<LimitSnapshotDTO> getLimitSnapshot(Long cardId) {
        log.debug("Getting limit snapshot for card: {}", cardId);

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Get or create daily spending window
        Mono<SpendingWindow> dailyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowDateAndWindowType(cardId, today, "DAILY")
                .switchIfEmpty(createDailySpendingWindow(cardId, today));

        // Get or create monthly spending window
        Mono<SpendingWindow> monthlyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowMonthAndWindowYearAndWindowType(cardId, currentMonth, currentYear, "MONTHLY")
                .switchIfEmpty(createMonthlySpendingWindow(cardId, currentMonth, currentYear));

        // Combine the results to create a limit snapshot
        return Mono.zip(dailyWindowMono, monthlyWindowMono)
                .map(tuple -> {
                    SpendingWindow dailyWindow = tuple.getT1();
                    SpendingWindow monthlyWindow = tuple.getT2();

                    return LimitSnapshotDTO.builder()
                            .dailyLimit(dailyWindow.getLimitAmount())
                            .dailySpent(dailyWindow.getSpentAmount())
                            .dailyRemaining(dailyWindow.getRemainingAmount())
                            .monthlyLimit(monthlyWindow.getLimitAmount())
                            .monthlySpent(monthlyWindow.getSpentAmount())
                            .monthlyRemaining(monthlyWindow.getRemainingAmount())
                            .singleTransactionLimit(defaultSingleTransactionLimit)
                            .atmDailyLimit(defaultAtmDailyLimit)
                            .atmDailySpent(BigDecimal.ZERO) // TODO: Implement ATM-specific tracking
                            .contactlessLimit(defaultContactlessLimit)
                            .onlineLimit(defaultOnlineLimit)
                            .snapshotDate(today)
                            .build();
                });
    }

    /**
     * Update the spending counters for a card after a successful authorization.
     *
     * @param cardId The ID of the card
     * @param amount The amount of the transaction
     * @param channel The channel of the transaction
     * @return A Mono emitting the updated limit snapshot
     */
    @Override
    public Mono<LimitSnapshotDTO> updateSpendingCounters(Long cardId, BigDecimal amount, String channel) {
        log.debug("Updating spending counters for card: {}, amount: {}, channel: {}", cardId, amount, channel);

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Update daily spending window
        Mono<SpendingWindow> dailyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowDateAndWindowType(cardId, today, "DAILY")
                .switchIfEmpty(createDailySpendingWindow(cardId, today))
                .flatMap(window -> {
                    window.setSpentAmount(window.getSpentAmount().add(amount));
                    window.setRemainingAmount(window.getLimitAmount().subtract(window.getSpentAmount()));
                    window.setTransactionCount(window.getTransactionCount() + 1);
                    window.setLastTransactionTime(java.time.LocalDateTime.now());
                    return spendingWindowRepository.save(window);
                });

        // Update monthly spending window
        Mono<SpendingWindow> monthlyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowMonthAndWindowYearAndWindowType(cardId, currentMonth, currentYear, "MONTHLY")
                .switchIfEmpty(createMonthlySpendingWindow(cardId, currentMonth, currentYear))
                .flatMap(window -> {
                    window.setSpentAmount(window.getSpentAmount().add(amount));
                    window.setRemainingAmount(window.getLimitAmount().subtract(window.getSpentAmount()));
                    window.setTransactionCount(window.getTransactionCount() + 1);
                    window.setLastTransactionTime(java.time.LocalDateTime.now());
                    return spendingWindowRepository.save(window);
                });

        // Combine the results to create an updated limit snapshot
        return Mono.zip(dailyWindowMono, monthlyWindowMono)
                .flatMap(tuple -> getLimitSnapshot(cardId));
    }

    /**
     * Reverse the spending counters for a card after a reversal.
     *
     * @param cardId The ID of the card
     * @param amount The amount of the transaction to reverse
     * @param channel The channel of the transaction
     * @return A Mono emitting the updated limit snapshot
     */
    @Override
    public Mono<LimitSnapshotDTO> reverseSpendingCounters(Long cardId, BigDecimal amount, String channel) {
        log.debug("Reversing spending counters for card: {}, amount: {}, channel: {}", cardId, amount, channel);

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Update daily spending window
        Mono<SpendingWindow> dailyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowDateAndWindowType(cardId, today, "DAILY")
                .flatMap(window -> {
                    window.setSpentAmount(window.getSpentAmount().subtract(amount));
                    if (window.getSpentAmount().compareTo(BigDecimal.ZERO) < 0) {
                        window.setSpentAmount(BigDecimal.ZERO);
                    }
                    window.setRemainingAmount(window.getLimitAmount().subtract(window.getSpentAmount()));
                    return spendingWindowRepository.save(window);
                });

        // Update monthly spending window
        Mono<SpendingWindow> monthlyWindowMono = spendingWindowRepository
                .findByCardIdAndWindowMonthAndWindowYearAndWindowType(cardId, currentMonth, currentYear, "MONTHLY")
                .flatMap(window -> {
                    window.setSpentAmount(window.getSpentAmount().subtract(amount));
                    if (window.getSpentAmount().compareTo(BigDecimal.ZERO) < 0) {
                        window.setSpentAmount(BigDecimal.ZERO);
                    }
                    window.setRemainingAmount(window.getLimitAmount().subtract(window.getSpentAmount()));
                    return spendingWindowRepository.save(window);
                });

        // Combine the results to create an updated limit snapshot
        return Mono.zip(dailyWindowMono, monthlyWindowMono)
                .flatMap(tuple -> getLimitSnapshot(cardId));
    }

    /**
     * Create a new daily spending window for a card.
     *
     * @param cardId The ID of the card
     * @param date The date for the spending window
     * @return A Mono emitting the created spending window
     */
    private Mono<SpendingWindow> createDailySpendingWindow(Long cardId, LocalDate date) {
        SpendingWindow window = new SpendingWindow();
        window.setCardId(cardId);
        window.setWindowType("DAILY");
        window.setWindowDate(date);
        window.setLimitAmount(defaultDailyLimit);
        window.setSpentAmount(BigDecimal.ZERO);
        window.setRemainingAmount(defaultDailyLimit);
        window.setTransactionCount(0);
        window.setCreatedAt(java.time.LocalDateTime.now());
        window.setUpdatedAt(java.time.LocalDateTime.now());

        return spendingWindowRepository.save(window);
    }

    /**
     * Create a new monthly spending window for a card.
     *
     * @param cardId The ID of the card
     * @param month The month for the spending window
     * @param year The year for the spending window
     * @return A Mono emitting the created spending window
     */
    private Mono<SpendingWindow> createMonthlySpendingWindow(Long cardId, int month, int year) {
        SpendingWindow window = new SpendingWindow();
        window.setCardId(cardId);
        window.setWindowType("MONTHLY");
        window.setWindowMonth(month);
        window.setWindowYear(year);
        window.setLimitAmount(defaultMonthlyLimit);
        window.setSpentAmount(BigDecimal.ZERO);
        window.setRemainingAmount(defaultMonthlyLimit);
        window.setTransactionCount(0);
        window.setCreatedAt(java.time.LocalDateTime.now());
        window.setUpdatedAt(java.time.LocalDateTime.now());

        return spendingWindowRepository.save(window);
    }
}
