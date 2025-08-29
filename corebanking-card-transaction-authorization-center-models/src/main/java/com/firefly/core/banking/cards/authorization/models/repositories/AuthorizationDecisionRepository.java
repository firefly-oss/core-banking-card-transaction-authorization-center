package com.firefly.core.banking.cards.authorization.models.repositories;

import com.firefly.core.banking.cards.authorization.models.entities.AuthorizationDecision;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for managing AuthorizationDecision entities.
 */
@Repository
public interface AuthorizationDecisionRepository extends BaseRepository<AuthorizationDecision, Long> {

    /**
     * Find an authorization decision by its unique decision ID.
     *
     * @param decisionId The unique decision ID
     * @return A Mono emitting the found AuthorizationDecision or empty if not found
     */
    Mono<AuthorizationDecision> findByDecisionId(Long decisionId);

    /**
     * Find an authorization decision by the request ID it's associated with.
     *
     * @param requestId The request ID
     * @return A Mono emitting the found AuthorizationDecision or empty if not found
     */
    Mono<AuthorizationDecision> findByRequestId(Long requestId);

    /**
     * Find authorization decisions by account ID and account space ID.
     *
     * @param accountId The account ID
     * @param accountSpaceId The account space ID
     * @return A Mono emitting the found AuthorizationDecision or empty if not found
     */
    Mono<AuthorizationDecision> findByAccountIdAndAccountSpaceId(Long accountId, Long accountSpaceId);

    /**
     * Check if an authorization decision with the given decision ID exists.
     *
     * @param decisionId The unique decision ID
     * @return A Mono emitting true if the decision exists, false otherwise
     */
    Mono<Boolean> existsByDecisionId(Long decisionId);
}
