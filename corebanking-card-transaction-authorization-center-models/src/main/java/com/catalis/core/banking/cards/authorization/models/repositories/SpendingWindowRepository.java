package com.catalis.core.banking.cards.authorization.models.repositories;

import com.catalis.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.catalis.core.banking.cards.authorization.models.entities.SpendingWindow;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Repository for managing SpendingWindow entities.
 */
@Repository
public interface SpendingWindowRepository extends BaseRepository<SpendingWindow, Long> {

    /**
     * Find a daily spending window for a specific card.
     *
     * @param cardId The card ID
     * @param windowDate The date of the spending window
     * @param windowType The type of window (e.g., "DAILY")
     * @return A Mono emitting the found SpendingWindow or empty if not found
     */
    Mono<SpendingWindow> findByCardIdAndWindowDateAndWindowType(Long cardId, LocalDate windowDate, String windowType);

    /**
     * Find a monthly spending window for a specific card.
     *
     * @param cardId The card ID
     * @param windowMonth The month of the spending window
     * @param windowYear The year of the spending window
     * @param windowType The type of window (e.g., "MONTHLY")
     * @return A Mono emitting the found SpendingWindow or empty if not found
     */
    Mono<SpendingWindow> findByCardIdAndWindowMonthAndWindowYearAndWindowType(
            Long cardId, Integer windowMonth, Integer windowYear, String windowType);

    /**
     * Find a daily spending window for a specific card and channel.
     *
     * @param cardId The card ID
     * @param channel The transaction channel
     * @param windowDate The date of the spending window
     * @param windowType The type of window (e.g., "DAILY_CHANNEL")
     * @return A Mono emitting the found SpendingWindow or empty if not found
     */
    Mono<SpendingWindow> findByCardIdAndChannelAndWindowDateAndWindowType(
            Long cardId, TransactionChannel channel, LocalDate windowDate, String windowType);

    /**
     * Find all spending windows for a specific card.
     *
     * @param cardId The card ID
     * @return A Flux emitting all SpendingWindows for the card
     */
    Flux<SpendingWindow> findByCardId(Long cardId);

    /**
     * Find all spending windows for a specific account.
     *
     * @param accountId The account ID
     * @return A Flux emitting all SpendingWindows for the account
     */
    Flux<SpendingWindow> findByAccountId(Long accountId);

    /**
     * Find all spending windows for a specific account and account space.
     *
     * @param accountId The account ID
     * @param accountSpaceId The account space ID
     * @return A Flux emitting all SpendingWindows for the account and account space
     */
    Flux<SpendingWindow> findByAccountIdAndAccountSpaceId(Long accountId, Long accountSpaceId);
}
