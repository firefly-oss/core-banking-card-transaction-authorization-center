package com.firefly.core.banking.cards.authorization.web.controllers;

import com.firefly.core.banking.cards.authorization.core.services.HoldManagementService;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationHoldDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.BalanceSnapshotDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for managing authorization holds.
 */
@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authorization Holds", description = "API for managing authorization holds")
public class HoldManagementController {

    private final HoldManagementService holdManagementService;

    /**
     * Get an authorization hold by its ID.
     *
     * @param holdId The ID of the hold to retrieve
     * @return The authorization hold
     */
    @GetMapping("/{holdId}")
    @Operation(
            summary = "Get an authorization hold",
            description = "Retrieve an authorization hold by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Hold found",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Hold not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationHoldDTO>> getHold(
            @Parameter(description = "Hold ID", required = true)
            @PathVariable Long holdId
    ) {
        return holdManagementService.getHoldById(holdId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get an authorization hold by request ID.
     *
     * @param requestId The ID of the request to retrieve the hold for
     * @return The authorization hold
     */
    @GetMapping("/request/{requestId}")
    @Operation(
            summary = "Get an authorization hold by request ID",
            description = "Retrieve an authorization hold for a specific request",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Hold found",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Hold not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationHoldDTO>> getHoldByRequestId(
            @Parameter(description = "Request ID", required = true)
            @PathVariable Long requestId
    ) {
        return holdManagementService.getHoldByRequestId(requestId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get all authorization holds for a specific account.
     *
     * @param accountId The ID of the account
     * @return A list of authorization holds
     */
    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Get holds by account",
            description = "Retrieve all authorization holds for a specific account",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Holds retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    )
            }
    )
    public Mono<ResponseEntity<Flux<AuthorizationHoldDTO>>> getHoldsByAccount(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long accountId
    ) {
        Flux<AuthorizationHoldDTO> holds = holdManagementService.getHoldsByAccountId(accountId);
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(holds));
    }

    /**
     * Get all authorization holds for a specific account and account space.
     *
     * @param accountId The ID of the account
     * @param accountSpaceId The ID of the account space
     * @return A list of authorization holds
     */
    @GetMapping("/account/{accountId}/space/{accountSpaceId}")
    @Operation(
            summary = "Get holds by account and account space",
            description = "Retrieve all authorization holds for a specific account and account space",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Holds retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    )
            }
    )
    public Mono<ResponseEntity<Flux<AuthorizationHoldDTO>>> getHoldsByAccountAndSpace(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long accountId,
            @Parameter(description = "Account Space ID", required = true)
            @PathVariable Long accountSpaceId
    ) {
        Flux<AuthorizationHoldDTO> holds = holdManagementService.getHoldsByAccountIdAndAccountSpaceId(accountId, accountSpaceId);
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(holds));
    }

    /**
     * Get all authorization holds for a specific card.
     *
     * @param cardId The ID of the card
     * @return A list of authorization holds
     */
    @GetMapping("/card/{cardId}")
    @Operation(
            summary = "Get holds by card",
            description = "Retrieve all authorization holds for a specific card",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Holds retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    )
            }
    )
    public Mono<ResponseEntity<Flux<AuthorizationHoldDTO>>> getHoldsByCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable Long cardId
    ) {
        Flux<AuthorizationHoldDTO> holds = holdManagementService.getHoldsByCardId(cardId);
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(holds));
    }

    /**
     * Capture an authorization hold.
     *
     * @param holdId The ID of the hold to capture
     * @param captureRequest The capture request containing the amount
     * @return The updated authorization hold
     */
    @PostMapping("/{holdId}/capture")
    @Operation(
            summary = "Capture a hold",
            description = "Capture an authorization hold (e.g., when a transaction is settled)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Hold captured successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Hold not found"
                    )
            }
    )
    public Mono<ResponseEntity<AuthorizationHoldDTO>> captureHold(
            @Parameter(description = "Hold ID", required = true)
            @PathVariable Long holdId,
            @RequestBody Map<String, Object> captureRequest
    ) {
        BigDecimal amount = new BigDecimal(captureRequest.getOrDefault("amount", "0").toString());
        return holdManagementService.captureHold(holdId, amount)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Release an authorization hold.
     *
     * @param holdId The ID of the hold to release
     * @return The updated balance snapshot
     */
    @PostMapping("/{holdId}/release")
    @Operation(
            summary = "Release a hold",
            description = "Release an authorization hold (e.g., when a transaction is cancelled)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Hold released successfully",
                            content = @Content(schema = @Schema(implementation = BalanceSnapshotDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Hold not found"
                    )
            }
    )
    public Mono<ResponseEntity<BalanceSnapshotDTO>> releaseHold(
            @Parameter(description = "Hold ID", required = true)
            @PathVariable Long holdId
    ) {
        return holdManagementService.releaseHold(holdId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Process expired holds.
     *
     * @return A list of processed authorization holds
     */
    @PostMapping("/process-expired")
    @Operation(
            summary = "Process expired holds",
            description = "Process holds that have expired (e.g., holds that have not been captured within the expiration period)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Expired holds processed successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizationHoldDTO.class))
                    )
            }
    )
    public Mono<ResponseEntity<Flux<AuthorizationHoldDTO>>> processExpiredHolds() {
        Flux<AuthorizationHoldDTO> processedHolds = holdManagementService.processExpiredHolds();
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(processedHolds));
    }
}
