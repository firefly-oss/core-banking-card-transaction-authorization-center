package com.firefly.core.banking.cards.authorization.core.services.impl;

import com.firefly.core.banking.cards.authorization.core.services.BalanceService;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the BalanceService interface.
 * This service checks and manages account balances.
 * Currently uses a simplified in-memory approach for demonstration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    // In-memory cache of account balances for demonstration
    private final Map<String, BalanceSnapshotDTO> accountBalances = new ConcurrentHashMap<>();

    // In-memory cache of account space balances for demonstration
    private final Map<String, BalanceSnapshotDTO> accountSpaceBalances = new ConcurrentHashMap<>();

    // In-memory cache of exchange rates for demonstration
    private final Map<String, BigDecimal> exchangeRates = new HashMap<>();

    /**
     * Initialize the exchange rates.
     */
    {
        // Base currency is USD
        exchangeRates.put("USD-USD", BigDecimal.ONE);
        exchangeRates.put("USD-EUR", new BigDecimal("0.85"));
        exchangeRates.put("USD-GBP", new BigDecimal("0.75"));
        exchangeRates.put("USD-JPY", new BigDecimal("110.0"));

        exchangeRates.put("EUR-USD", new BigDecimal("1.18"));
        exchangeRates.put("EUR-EUR", BigDecimal.ONE);
        exchangeRates.put("EUR-GBP", new BigDecimal("0.88"));
        exchangeRates.put("EUR-JPY", new BigDecimal("130.0"));

        exchangeRates.put("GBP-USD", new BigDecimal("1.33"));
        exchangeRates.put("GBP-EUR", new BigDecimal("1.14"));
        exchangeRates.put("GBP-GBP", BigDecimal.ONE);
        exchangeRates.put("GBP-JPY", new BigDecimal("145.0"));

        exchangeRates.put("JPY-USD", new BigDecimal("0.009"));
        exchangeRates.put("JPY-EUR", new BigDecimal("0.0077"));
        exchangeRates.put("JPY-GBP", new BigDecimal("0.0069"));
        exchangeRates.put("JPY-JPY", BigDecimal.ONE);
    }

    /**
     * Check if there are sufficient funds for a transaction.
     *
     * @param request The authorization request
     * @param cardDetails The card details
     * @return A Mono emitting the balance snapshot if there are sufficient funds, or an error if there are not
     */
    @Override
    public Mono<BalanceSnapshotDTO> checkSufficientFunds(AuthorizationRequestDTO request, CardDetailsDTO cardDetails) {
        log.debug("Checking sufficient funds for request: {}", request.getRequestId());

        Long accountId = cardDetails.getAccountId();
        Long accountSpaceId = cardDetails.getAccountSpaceId();
        BigDecimal amount = request.getAmount();
        String currency = request.getCurrency();

        return getBalance(accountId, accountSpaceId)
                .flatMap(balanceSnapshot -> {
                    // If the currencies are different, convert the amount
                    if (!currency.equals(balanceSnapshot.getCurrency())) {
                        return convertCurrency(amount, currency, balanceSnapshot.getCurrency())
                                .flatMap(convertedAmount -> {
                                    if (convertedAmount.compareTo(balanceSnapshot.getAvailableBalanceBefore()) > 0) {
                                        return Mono.error(new IllegalStateException("Insufficient funds"));
                                    }

                                    // Create a new balance snapshot with the converted amount
                                    BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                            .accountId(accountId)
                                            .accountSpaceId(accountSpaceId)
                                            .currency(balanceSnapshot.getCurrency())
                                            .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                            .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().subtract(convertedAmount))
                                            .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                            .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().add(convertedAmount))
                                            .exchangeRate(exchangeRates.get(currency + "-" + balanceSnapshot.getCurrency()))
                                            .originalCurrency(currency)
                                            .originalAmount(amount)
                                            .timestamp(LocalDateTime.now())
                                            .build();

                                    return Mono.just(newSnapshot);
                                });
                    } else {
                        // Currencies are the same
                        if (amount.compareTo(balanceSnapshot.getAvailableBalanceBefore()) > 0) {
                            return Mono.error(new IllegalStateException("Insufficient funds"));
                        }

                        // Create a new balance snapshot
                        BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                .accountId(accountId)
                                .accountSpaceId(accountSpaceId)
                                .currency(currency)
                                .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().subtract(amount))
                                .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().add(amount))
                                .timestamp(LocalDateTime.now())
                                .build();

                        return Mono.just(newSnapshot);
                    }
                });
    }

    /**
     * Get the current balance for an account.
     *
     * @param accountId The ID of the account
     * @return A Mono emitting the current balance snapshot
     */
    @Override
    public Mono<BalanceSnapshotDTO> getBalance(Long accountId) {
        log.debug("Getting balance for account: {}", accountId);

        // Check if we have a cached balance
        String key = accountId.toString();
        BalanceSnapshotDTO cachedBalance = accountBalances.get(key);
        if (cachedBalance != null) {
            return Mono.just(cachedBalance);
        }

        // TODO: Implement actual ledger service integration
        // For now, create a mock balance
        BalanceSnapshotDTO mockBalance = createMockBalance(accountId, null);
        accountBalances.put(key, mockBalance);

        return Mono.just(mockBalance);
    }

    /**
     * Get the current balance for an account space.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space
     * @return A Mono emitting the current balance snapshot
     */
    public Mono<BalanceSnapshotDTO> getBalance(Long accountId, Long accountSpaceId) {
        log.debug("Getting balance for account: {}, space: {}", accountId, accountSpaceId);

        // If accountSpaceId is null, return the main account balance
        if (accountSpaceId == null) {
            return getBalance(accountId, null);
        }

        // Check if we have a cached balance for the account space
        String key = accountId + "-" + accountSpaceId;
        BalanceSnapshotDTO cachedBalance = accountSpaceBalances.get(key);
        if (cachedBalance != null) {
            return Mono.just(cachedBalance);
        }

        // TODO: Implement actual ledger service integration
        // For now, create a mock balance for the account space
        BalanceSnapshotDTO mockBalance = createMockBalance(accountId, accountSpaceId);
        accountSpaceBalances.put(key, mockBalance);

        return Mono.just(mockBalance);
    }

    /**
     * Reserve funds for a transaction.
     *
     * @param accountId The ID of the account
     * @param amount The amount to reserve
     * @param currency The currency of the amount
     * @return A Mono emitting the updated balance snapshot
     */
    @Override
    public Mono<BalanceSnapshotDTO> reserveFunds(Long accountId, BigDecimal amount, String currency) {
        log.debug("Reserving funds for account: {}, amount: {}, currency: {}", accountId, amount, currency);

        return getBalance(accountId)
                .flatMap(balanceSnapshot -> {
                    // If the currencies are different, convert the amount
                    if (!currency.equals(balanceSnapshot.getCurrency())) {
                        return convertCurrency(amount, currency, balanceSnapshot.getCurrency())
                                .flatMap(convertedAmount -> {
                                    if (convertedAmount.compareTo(balanceSnapshot.getAvailableBalanceBefore()) > 0) {
                                        return Mono.error(new IllegalStateException("Insufficient funds"));
                                    }

                                    // Create a new balance snapshot with the converted amount
                                    BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                            .accountId(accountId)
                                            .currency(balanceSnapshot.getCurrency())
                                            .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                            .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().subtract(convertedAmount))
                                            .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                            .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().add(convertedAmount))
                                            .exchangeRate(exchangeRates.get(currency + "-" + balanceSnapshot.getCurrency()))
                                            .originalCurrency(currency)
                                            .originalAmount(amount)
                                            .timestamp(LocalDateTime.now())
                                            .build();

                                    // Update the cached balance
                                    accountBalances.put(accountId.toString(), newSnapshot);

                                    return Mono.just(newSnapshot);
                                });
                    } else {
                        // Currencies are the same
                        if (amount.compareTo(balanceSnapshot.getAvailableBalanceBefore()) > 0) {
                            return Mono.error(new IllegalStateException("Insufficient funds"));
                        }

                        // Create a new balance snapshot
                        BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                .accountId(accountId)
                                .accountSpaceId(balanceSnapshot.getAccountSpaceId())
                                .currency(currency)
                                .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().subtract(amount))
                                .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().add(amount))
                                .timestamp(LocalDateTime.now())
                                .build();

                        // Update the cached balance
                        accountBalances.put(accountId.toString(), newSnapshot);

                        return Mono.just(newSnapshot);
                    }
                });
    }

    /**
     * Release reserved funds.
     *
     * @param accountId The ID of the account
     * @param amount The amount to release
     * @param currency The currency of the amount
     * @return A Mono emitting the updated balance snapshot
     */
    @Override
    public Mono<BalanceSnapshotDTO> releaseFunds(Long accountId, BigDecimal amount, String currency) {
        log.debug("Releasing funds for account: {}, amount: {}, currency: {}", accountId, amount, currency);

        return getBalance(accountId)
                .flatMap(balanceSnapshot -> {
                    // If the currencies are different, convert the amount
                    if (!currency.equals(balanceSnapshot.getCurrency())) {
                        return convertCurrency(amount, currency, balanceSnapshot.getCurrency())
                                .flatMap(convertedAmount -> {
                                    // Create a new balance snapshot with the converted amount
                                    BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                            .accountId(accountId)
                                            .accountSpaceId(balanceSnapshot.getAccountSpaceId())
                                            .currency(balanceSnapshot.getCurrency())
                                            .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                            .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().add(convertedAmount))
                                            .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                            .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().subtract(convertedAmount))
                                            .exchangeRate(exchangeRates.get(currency + "-" + balanceSnapshot.getCurrency()))
                                            .originalCurrency(currency)
                                            .originalAmount(amount)
                                            .timestamp(LocalDateTime.now())
                                            .build();

                                    // Update the cached balance
                                    accountBalances.put(accountId.toString(), newSnapshot);

                                    return Mono.just(newSnapshot);
                                });
                    } else {
                        // Currencies are the same
                        // Create a new balance snapshot
                        BalanceSnapshotDTO newSnapshot = BalanceSnapshotDTO.builder()
                                .accountId(accountId)
                                .accountSpaceId(balanceSnapshot.getAccountSpaceId())
                                .currency(currency)
                                .availableBalanceBefore(balanceSnapshot.getAvailableBalanceBefore())
                                .availableBalanceAfter(balanceSnapshot.getAvailableBalanceBefore().add(amount))
                                .ledgerBalance(balanceSnapshot.getLedgerBalance())
                                .totalHoldAmount(balanceSnapshot.getTotalHoldAmount().subtract(amount))
                                .timestamp(LocalDateTime.now())
                                .build();

                        // Update the cached balance
                        accountBalances.put(accountId.toString(), newSnapshot);

                        return Mono.just(newSnapshot);
                    }
                });
    }

    /**
     * Convert an amount from one currency to another.
     *
     * @param amount The amount to convert
     * @param fromCurrency The currency to convert from
     * @param toCurrency The currency to convert to
     * @return A Mono emitting the converted amount
     */
    @Override
    public Mono<BigDecimal> convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        log.debug("Converting currency: {} {} to {}", amount, fromCurrency, toCurrency);

        // If the currencies are the same, no conversion needed
        if (fromCurrency.equals(toCurrency)) {
            return Mono.just(amount);
        }

        // Get the exchange rate
        return getExchangeRate(fromCurrency, toCurrency)
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Get the exchange rate between two currencies.
     *
     * @param fromCurrency The currency to convert from
     * @param toCurrency The currency to convert to
     * @return A Mono emitting the exchange rate
     */
    @Override
    public Mono<BigDecimal> getExchangeRate(String fromCurrency, String toCurrency) {
        log.debug("Getting exchange rate from {} to {}", fromCurrency, toCurrency);

        // If the currencies are the same, the rate is 1
        if (fromCurrency.equals(toCurrency)) {
            return Mono.just(BigDecimal.ONE);
        }

        // Get the exchange rate from the cache
        String key = fromCurrency + "-" + toCurrency;
        BigDecimal rate = exchangeRates.get(key);

        if (rate != null) {
            return Mono.just(rate);
        } else {
            // TODO: Implement actual FX service integration
            // For now, return an error
            return Mono.error(new IllegalArgumentException("Exchange rate not found for " + key));
        }
    }

    /**
     * Create a mock balance for testing purposes.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space (can be null)
     * @return The mock balance
     */
    private BalanceSnapshotDTO createMockBalance(Long accountId, Long accountSpaceId) {
        // If this is an account space, adjust the balance accordingly
        BigDecimal availableBalance = accountSpaceId != null ? new BigDecimal("1000.00") : new BigDecimal("5000.00");
        BigDecimal ledgerBalance = accountSpaceId != null ? new BigDecimal("1000.00") : new BigDecimal("5000.00");

        return BalanceSnapshotDTO.builder()
                .accountId(accountId)
                .accountSpaceId(accountSpaceId)
                .currency("USD")
                .availableBalanceBefore(availableBalance)
                .availableBalanceAfter(availableBalance)
                .ledgerBalance(ledgerBalance)
                .totalHoldAmount(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
