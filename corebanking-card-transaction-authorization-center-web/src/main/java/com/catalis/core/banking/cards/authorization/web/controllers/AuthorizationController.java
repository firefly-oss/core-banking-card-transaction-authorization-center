package com.catalis.core.banking.cards.authorization.web.controllers;

import com.catalis.core.banking.cards.authorization.core.services.AuthorizationService;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationDecisionDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * REST controller for card authorization operations.
 */
@RestController
@RequestMapping("/api/v1/authorizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Authorization", description = "API for card transaction authorization")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    /**
     * Authorize a card transaction.
     *
     * @param request The authorization request
     * @return The authorization decision
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Authorize a card transaction",
            description = "Process a card transaction authorization request and return a decision",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Authorization processed successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationDecisionDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request"
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Authorization declined"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationDecisionDTO>> authorizeTransaction(
            @Valid @RequestBody AuthorizationRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Received authorization request: {}", request.getRequestId());

        // If idempotency key is provided, use it to generate a consistent request ID
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            // Generate a deterministic ID from the idempotency key
            request.setRequestId(Math.abs(idempotencyKey.hashCode()) % 900000000000L + 100000000000L);
        }

        return authorizationService.authorizeTransaction(request)
                .map(decision -> {
                    HttpStatus status;
                    switch (decision.getDecision()) {
                        case APPROVED:
                        case PARTIAL:
                            status = HttpStatus.OK;
                            break;
                        case DECLINED:
                            status = HttpStatus.UNPROCESSABLE_ENTITY;
                            break;
                        case CHALLENGE:
                            status = HttpStatus.ACCEPTED;
                            break;
                        default:
                            status = HttpStatus.OK;
                    }
                    return ResponseEntity.status(status).body(decision);
                });
    }

    /**
     * Get an authorization decision by its ID.
     *
     * @param decisionId The ID of the decision to retrieve
     * @return The authorization decision
     */
    @GetMapping("/{decisionId}")
    @Operation(
            summary = "Get an authorization decision",
            description = "Retrieve an authorization decision by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Decision found",
                            content = @Content(schema = @Schema(implementation = AuthorizationDecisionDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Decision not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationDecisionDTO>> getDecision(
            @Parameter(description = "Decision ID", required = true)
            @PathVariable Long decisionId
    ) {
        return authorizationService.getDecisionById(decisionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get an authorization decision by request ID.
     *
     * @param requestId The ID of the request to retrieve the decision for
     * @return The authorization decision
     */
    @GetMapping("/request/{requestId}")
    @Operation(
            summary = "Get an authorization decision by request ID",
            description = "Retrieve an authorization decision for a specific request",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Decision found",
                            content = @Content(schema = @Schema(implementation = AuthorizationDecisionDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Decision not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationDecisionDTO>> getDecisionByRequestId(
            @Parameter(description = "Request ID", required = true)
            @PathVariable Long requestId
    ) {
        return authorizationService.getDecisionByRequestId(requestId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Reverse an authorization.
     *
     * @param requestId The ID of the original authorization request
     * @param reversalRequest The reversal request containing the reason
     * @return The updated authorization decision
     */
    @PostMapping("/{requestId}/reverse")
    @Operation(
            summary = "Reverse an authorization",
            description = "Reverse a previously approved authorization",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Authorization reversed successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationDecisionDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Authorization not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationDecisionDTO>> reverseAuthorization(
            @Parameter(description = "Request ID", required = true)
            @PathVariable Long requestId,
            @RequestBody Map<String, String> reversalRequest
    ) {
        String reason = reversalRequest.getOrDefault("reason", "Merchant initiated reversal");
        return authorizationService.reverseAuthorization(requestId, reason)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Handle a 3DS challenge completion.
     *
     * @param requestId The ID of the original authorization request
     * @param challengeRequest The challenge completion request
     * @return The updated authorization decision
     */
    @PostMapping("/{requestId}/challenge-complete")
    @Operation(
            summary = "Complete a 3DS challenge",
            description = "Handle the completion of a 3DS challenge for an authorization",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Challenge processed successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationDecisionDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Authorization not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationDecisionDTO>> handleChallengeCompletion(
            @Parameter(description = "Request ID", required = true)
            @PathVariable Long requestId,
            @RequestBody Map<String, String> challengeRequest
    ) {
        String challengeResult = challengeRequest.getOrDefault("challengeResult", "");
        return authorizationService.handleChallengeCompletion(requestId, challengeResult)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
