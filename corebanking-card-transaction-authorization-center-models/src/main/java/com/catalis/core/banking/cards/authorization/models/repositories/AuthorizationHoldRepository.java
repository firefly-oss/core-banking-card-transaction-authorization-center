package com.catalis.core.banking.cards.authorization.models.repositories;

import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationHold;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repository for managing AuthorizationHold entities.
 */
@Repository
public interface AuthorizationHoldRepository extends BaseRepository<AuthorizationHold, Long> {

    /**
     * Find an authorization hold by its unique hold ID.
     *
     * @param holdId The unique hold ID
     * @return A Mono emitting the found AuthorizationHold or empty if not found
     */
    Mono<AuthorizationHold> findByHoldId(Long holdId);

    /**
     * Find an authorization hold by the request ID it's associated with.
     *
     * @param requestId The request ID
     * @return A Mono emitting the found AuthorizationHold or empty if not found
     */
    Mono<AuthorizationHold> findByRequestId(Long requestId);

    /**
     * Find an authorization hold by the decision ID it's associated with.
     *
     * @param decisionId The decision ID
     * @return A Mono emitting the found AuthorizationHold or empty if not found
     */
    Mono<AuthorizationHold> findByDecisionId(Long decisionId);

    /**
     * Find all authorization holds for a specific account.
     *
     * @param accountId The account ID
     * @return A Flux emitting all AuthorizationHolds for the account
     */
    Flux<AuthorizationHold> findByAccountId(Long accountId);

    /**
     * Find all authorization holds for a specific account and account space.
     *
     * @param accountId The account ID
     * @param accountSpaceId The account space ID
     * @return A Flux emitting all AuthorizationHolds for the account and account space
     */
    Flux<AuthorizationHold> findByAccountIdAndAccountSpaceId(Long accountId, Long accountSpaceId);

    /**
     * Find all authorization holds for a specific card.
     *
     * @param cardId The card ID
     * @return A Flux emitting all AuthorizationHolds for the card
     */
    Flux<AuthorizationHold> findByCardId(Long cardId);

    /**
     * Find all authorization holds that have expired but have not been released.
     *
     * @param now The current time
     * @param captureStatus The capture status to filter by (typically "PENDING")
     * @return A Flux emitting all expired AuthorizationHolds
     */
    Flux<AuthorizationHold> findByExpiresAtBeforeAndCaptureStatus(LocalDateTime now, String captureStatus);
}
