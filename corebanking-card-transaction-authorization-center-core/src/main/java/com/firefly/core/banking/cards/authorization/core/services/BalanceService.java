package com.firefly.core.banking.cards.authorization.core.services;

import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Service for checking and managing account balances.
 */
public interface BalanceService {

    /**
     * Check if there are sufficient funds for a transaction.
     *
     * @param request The authorization request
     * @param cardDetails The card details
     * @return A Mono emitting the balance snapshot if there are sufficient funds, or an error if there are not
     */
    Mono<BalanceSnapshotDTO> checkSufficientFunds(AuthorizationRequestDTO request, CardDetailsDTO cardDetails);

    /**
     * Get the current balance for an account.
     *
     * @param accountId The ID of the account
     * @return A Mono emitting the current balance snapshot
     */
    Mono<BalanceSnapshotDTO> getBalance(Long accountId);

    /**
     * Get the current balance for an account space.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space
     * @return A Mono emitting the current balance snapshot
     */
    Mono<BalanceSnapshotDTO> getBalance(Long accountId, Long accountSpaceId);

    /**
     * Reserve funds for a transaction.
     *
     * @param accountId The ID of the account
     * @param amount The amount to reserve
     * @param currency The currency of the amount
     * @return A Mono emitting the updated balance snapshot
     */
    Mono<BalanceSnapshotDTO> reserveFunds(Long accountId, BigDecimal amount, String currency);

    /**
     * Release reserved funds.
     *
     * @param accountId The ID of the account
     * @param amount The amount to release
     * @param currency The currency of the amount
     * @return A Mono emitting the updated balance snapshot
     */
    Mono<BalanceSnapshotDTO> releaseFunds(Long accountId, BigDecimal amount, String currency);

    /**
     * Convert an amount from one currency to another.
     *
     * @param amount The amount to convert
     * @param fromCurrency The currency to convert from
     * @param toCurrency The currency to convert to
     * @return A Mono emitting the converted amount
     */
    Mono<BigDecimal> convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Get the exchange rate between two currencies.
     *
     * @param fromCurrency The currency to convert from
     * @param toCurrency The currency to convert to
     * @return A Mono emitting the exchange rate
     */
    Mono<BigDecimal> getExchangeRate(String fromCurrency, String toCurrency);
}
