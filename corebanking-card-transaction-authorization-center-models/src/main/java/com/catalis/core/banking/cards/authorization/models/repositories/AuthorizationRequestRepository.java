package com.catalis.core.banking.cards.authorization.models.repositories;

import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationRequest;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for managing AuthorizationRequest entities.
 */
@Repository
public interface AuthorizationRequestRepository extends BaseRepository<AuthorizationRequest, Long> {

    /**
     * Find an authorization request by its unique request ID.
     *
     * @param requestId The unique request ID
     * @return A Mono emitting the found AuthorizationRequest or empty if not found
     */
    Mono<AuthorizationRequest> findByRequestId(Long requestId);

    /**
     * Check if an authorization request with the given request ID exists.
     *
     * @param requestId The unique request ID
     * @return A Mono emitting true if the request exists, false otherwise
     */
    Mono<Boolean> existsByRequestId(Long requestId);
}
