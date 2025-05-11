package com.catalis.core.banking.cards.authorization.core.services.impl;

import com.catalis.core.banking.cards.authorization.core.mappers.AuthorizationDecisionMapper;
import com.catalis.core.banking.cards.authorization.core.mappers.AuthorizationRequestMapper;
import com.catalis.core.banking.cards.authorization.core.services.*;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.*;
import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationDecisionType;
import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationReasonCode;
import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationDecision;
import com.catalis.core.banking.cards.authorization.models.entities.AuthorizationRequest;
import com.catalis.core.banking.cards.authorization.models.repositories.AuthorizationDecisionRepository;
import com.catalis.core.banking.cards.authorization.models.repositories.AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of the AuthorizationService interface.
 * This service orchestrates the entire authorization flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationRequestRepository requestRepository;
    private final AuthorizationDecisionRepository decisionRepository;
    private final AuthorizationRequestMapper requestMapper;
    private final AuthorizationDecisionMapper decisionMapper;
    private final CardValidationService cardValidationService;
    private final LimitValidationService limitValidationService;
    private final RiskAssessmentService riskAssessmentService;
    private final BalanceService balanceService;
    private final HoldManagementService holdManagementService;

    /**
     * Process a card authorization request and make a decision.
     *
     * @param request The authorization request to process
     * @return A Mono emitting the authorization decision
     */
    @Override
    public Mono<AuthorizationDecisionDTO> authorizeTransaction(AuthorizationRequestDTO request) {
        log.info("Processing authorization request: {}", request.getRequestId());

        // Generate a request ID if not provided
        if (request.getRequestId() == null) {
            request.setRequestId(generateRandomId());
        }

        // Check for duplicate request
        return requestRepository.existsByRequestId(request.getRequestId())
                .flatMap(exists -> {
                    if (exists) {
                        // If the request already exists, return the existing decision
                        log.info("Duplicate request detected: {}", request.getRequestId());
                        return getDecisionByRequestId(request.getRequestId());
                    }

                    // Save the request
                    AuthorizationRequest requestEntity = requestMapper.toEntity(request);
                    return requestRepository.save(requestEntity)
                            .flatMap(savedRequest -> processAuthorizationRequest(request));
                });
    }

    /**
     * Retrieve an authorization decision by its ID.
     *
     * @param decisionId The ID of the decision to retrieve
     * @return A Mono emitting the authorization decision
     */
    @Override
    public Mono<AuthorizationDecisionDTO> getDecisionById(Long decisionId) {
        log.debug("Getting decision by ID: {}", decisionId);
        return decisionRepository.findByDecisionId(decisionId)
                .map(decisionMapper::toDto);
    }

    /**
     * Retrieve an authorization decision for a specific request.
     *
     * @param requestId The ID of the request to retrieve the decision for
     * @return A Mono emitting the authorization decision
     */
    @Override
    public Mono<AuthorizationDecisionDTO> getDecisionByRequestId(Long requestId) {
        log.debug("Getting decision by request ID: {}", requestId);
        return decisionRepository.findByRequestId(requestId)
                .map(decisionMapper::toDto);
    }

    /**
     * Reverse an authorization (e.g., for a cancelled transaction).
     *
     * @param requestId The ID of the original authorization request
     * @param reason The reason for the reversal
     * @return A Mono emitting the updated authorization decision
     */
    @Override
    public Mono<AuthorizationDecisionDTO> reverseAuthorization(Long requestId, String reason) {
        log.info("Reversing authorization for request: {}, reason: {}", requestId, reason);

        return decisionRepository.findByRequestId(requestId)
                .flatMap(decision -> {
                    // Check if the decision can be reversed
                    if (!AuthorizationDecisionType.APPROVED.equals(decision.getDecision()) &&
                        !AuthorizationDecisionType.PARTIAL.equals(decision.getDecision())) {
                        return Mono.error(new IllegalStateException("Only approved authorizations can be reversed"));
                    }

                    // Update the decision
                    decision.setDecision(AuthorizationDecisionType.DECLINED);
                    decision.setReasonCode(AuthorizationReasonCode.DUPLICATE_TRANSACTION);
                    decision.setReasonMessage("Authorization reversed: " + reason);
                    decision.setUpdatedAt(LocalDateTime.now());

                    // Release the hold if one exists
                    if (decision.getHoldId() != null) {
                        return holdManagementService.releaseHold(decision.getHoldId())
                                .then(decisionRepository.save(decision))
                                .map(decisionMapper::toDto);
                    } else {
                        return decisionRepository.save(decision)
                                .map(decisionMapper::toDto);
                    }
                });
    }

    /**
     * Handle a 3DS challenge completion.
     *
     * @param requestId The ID of the original authorization request
     * @param challengeResult The result of the 3DS challenge
     * @return A Mono emitting the updated authorization decision
     */
    @Override
    public Mono<AuthorizationDecisionDTO> handleChallengeCompletion(Long requestId, String challengeResult) {
        log.info("Handling challenge completion for request: {}, result: {}", requestId, challengeResult);

        return decisionRepository.findByRequestId(requestId)
                .flatMap(decision -> {
                    // Check if the decision is in challenge state
                    if (!AuthorizationDecisionType.CHALLENGE.equals(decision.getDecision())) {
                        return Mono.error(new IllegalStateException("Decision is not in challenge state"));
                    }

                    // Update the decision based on the challenge result
                    if ("SUCCESS".equals(challengeResult)) {
                        decision.setDecision(AuthorizationDecisionType.APPROVED);
                        decision.setReasonCode(AuthorizationReasonCode.APPROVED_TRANSACTION);
                        decision.setReasonMessage("Challenge completed successfully");

                        // Create a hold
                        return requestRepository.findByRequestId(requestId)
                                .flatMap(request -> {
                                    AuthorizationRequestDTO requestDTO = requestMapper.toDto(request);
                                    return cardValidationService.getCardDetails(requestDTO.getPanHash())
                                            .flatMap(cardDetails -> holdManagementService.createHold(
                                                    requestDTO,
                                                    cardDetails.getAccountId(),
                                                    cardDetails.getCardId(),
                                                    decision.getApprovedAmount(),
                                                    decision.getCurrency(),
                                                    decision.getAuthorizationCode(),
                                                    decision.getDecisionId()
                                            ))
                                            .flatMap(hold -> {
                                                decision.setHoldId(hold.getHoldId());
                                                return decisionRepository.save(decision)
                                                        .map(decisionMapper::toDto);
                                            });
                                });
                    } else {
                        decision.setDecision(AuthorizationDecisionType.DECLINED);
                        decision.setReasonCode(AuthorizationReasonCode.SECURITY_VIOLATION);
                        decision.setReasonMessage("Challenge failed: " + challengeResult);

                        return decisionRepository.save(decision)
                                .map(decisionMapper::toDto);
                    }
                });
    }

    /**
     * Process an authorization request and make a decision.
     *
     * @param request The authorization request to process
     * @return A Mono emitting the authorization decision
     */
    private Mono<AuthorizationDecisionDTO> processAuthorizationRequest(AuthorizationRequestDTO request) {
        log.debug("Processing authorization request: {}", request.getRequestId());

        List<String> decisionPath = new ArrayList<>();
        decisionPath.add("Request received: " + request.getRequestId());

        // Step 1: Validate the card
        return cardValidationService.validateCard(request)
                .flatMap(cardDetails -> {
                    decisionPath.add("Card validation successful");

                    // Step 2: Validate limits
                    return limitValidationService.validateLimits(request, cardDetails)
                            .flatMap(limitsSnapshot -> {
                                decisionPath.add("Limit validation successful");

                                // Step 3: Assess risk
                                return riskAssessmentService.assessRisk(request, cardDetails)
                                        .flatMap(riskAssessment -> {
                                            decisionPath.add("Risk assessment completed: score=" + riskAssessment.getRiskScore());

                                            // Check if the transaction should be declined due to high risk
                                            return riskAssessmentService.shouldDecline(riskAssessment)
                                                    .flatMap(shouldDecline -> {
                                                        if (shouldDecline) {
                                                            decisionPath.add("Transaction declined due to high risk");
                                                            return createDeclinedDecision(
                                                                    request,
                                                                    AuthorizationReasonCode.SUSPECTED_FRAUD,
                                                                    "High risk transaction",
                                                                    riskAssessment.getRiskScore(),
                                                                    limitsSnapshot,
                                                                    null,
                                                                    decisionPath
                                                            );
                                                        }

                                                        // Check if the transaction should be challenged
                                                        return riskAssessmentService.shouldChallenge(riskAssessment)
                                                                .flatMap(shouldChallenge -> {
                                                                    if (shouldChallenge) {
                                                                        decisionPath.add("Transaction requires additional verification");
                                                                        return createChallengeDecision(
                                                                                request,
                                                                                "Additional verification required",
                                                                                riskAssessment.getRiskScore(),
                                                                                limitsSnapshot,
                                                                                null,
                                                                                decisionPath
                                                                        );
                                                                    }

                                                                    // Step 4: Check sufficient funds
                                                                    return balanceService.checkSufficientFunds(request, cardDetails)
                                                                            .flatMap(balanceSnapshot -> {
                                                                                decisionPath.add("Sufficient funds available");

                                                                                // Step 5: Create a hold
                                                                                String authorizationCode = generateAuthorizationCode();
                                                                                Long decisionId = generateRandomId();

                                                                                return holdManagementService.createHold(
                                                                                        request,
                                                                                        cardDetails.getAccountId(),
                                                                                        cardDetails.getCardId(),
                                                                                        request.getAmount(),
                                                                                        request.getCurrency(),
                                                                                        authorizationCode,
                                                                                        decisionId
                                                                                )
                                                                                .flatMap(hold -> {
                                                                                    decisionPath.add("Authorization hold created: " + hold.getHoldId());

                                                                                    // Step 6: Create and save the decision
                                                                                    return createApprovedDecision(
                                                                                            request,
                                                                                            decisionId,
                                                                                            authorizationCode,
                                                                                            riskAssessment.getRiskScore(),
                                                                                            limitsSnapshot,
                                                                                            balanceSnapshot,
                                                                                            hold.getHoldId(),
                                                                                            decisionPath
                                                                                    );
                                                                                });
                                                                            })
                                                                            .onErrorResume(e -> {
                                                                                decisionPath.add("Insufficient funds: " + e.getMessage());
                                                                                return createDeclinedDecision(
                                                                                        request,
                                                                                        AuthorizationReasonCode.INSUFFICIENT_FUNDS,
                                                                                        e.getMessage(),
                                                                                        riskAssessment.getRiskScore(),
                                                                                        limitsSnapshot,
                                                                                        null,
                                                                                        decisionPath
                                                                                );
                                                                            });
                                                                });
                                                    });
                                        });
                            })
                            .onErrorResume(e -> {
                                decisionPath.add("Limit validation failed: " + e.getMessage());
                                return createDeclinedDecision(
                                        request,
                                        AuthorizationReasonCode.EXCEEDS_TRANSACTION_LIMIT,
                                        e.getMessage(),
                                        null,
                                        null,
                                        null,
                                        decisionPath
                                );
                            });
                })
                .onErrorResume(e -> {
                    decisionPath.add("Card validation failed: " + e.getMessage());

                    AuthorizationReasonCode reasonCode;
                    if (e.getMessage().contains("expired")) {
                        reasonCode = AuthorizationReasonCode.EXPIRED_CARD;
                    } else if (e.getMessage().contains("not active")) {
                        reasonCode = AuthorizationReasonCode.CARD_NOT_ACTIVE;
                    } else {
                        reasonCode = AuthorizationReasonCode.INVALID_CARD;
                    }

                    return createDeclinedDecision(
                            request,
                            reasonCode,
                            e.getMessage(),
                            null,
                            null,
                            null,
                            decisionPath
                    );
                });
    }

    /**
     * Create an approved decision.
     *
     * @param request The authorization request
     * @param decisionId The decision ID
     * @param authorizationCode The authorization code
     * @param riskScore The risk score
     * @param limitsSnapshot The limits snapshot
     * @param balanceSnapshot The balance snapshot
     * @param holdId The hold ID
     * @param decisionPath The decision path
     * @return A Mono emitting the authorization decision
     */
    private Mono<AuthorizationDecisionDTO> createApprovedDecision(
            AuthorizationRequestDTO request,
            Long decisionId,
            String authorizationCode,
            Integer riskScore,
            LimitSnapshotDTO limitsSnapshot,
            BalanceSnapshotDTO balanceSnapshot,
            Long holdId,
            List<String> decisionPath) {

        AuthorizationDecisionDTO decision = AuthorizationDecisionDTO.builder()
                .requestId(request.getRequestId())
                .decisionId(decisionId)
                .decision(AuthorizationDecisionType.APPROVED)
                .reasonCode(AuthorizationReasonCode.APPROVED_TRANSACTION)
                .reasonMessage("Transaction approved")
                .approvedAmount(request.getAmount())
                .currency(request.getCurrency())
                .authorizationCode(authorizationCode)
                .riskScore(riskScore)
                .limitsSnapshot(limitsSnapshot)
                .balanceSnapshot(balanceSnapshot)
                .holdId(holdId)
                .timestamp(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .decisionPath(decisionPath)
                .build();

        AuthorizationDecision decisionEntity = decisionMapper.toEntity(decision);
        return decisionRepository.save(decisionEntity)
                .doOnSuccess(savedDecision -> {
                    // Mark the request as processed
                    requestRepository.findByRequestId(request.getRequestId())
                            .flatMap(req -> {
                                req.setProcessed(true);
                                req.setProcessedAt(LocalDateTime.now());
                                return requestRepository.save(req);
                            })
                            .subscribe();
                })
                .map(decisionMapper::toDto);
    }

    /**
     * Create a declined decision.
     *
     * @param request The authorization request
     * @param reasonCode The reason code
     * @param reasonMessage The reason message
     * @param riskScore The risk score
     * @param limitsSnapshot The limits snapshot
     * @param balanceSnapshot The balance snapshot
     * @param decisionPath The decision path
     * @return A Mono emitting the authorization decision
     */
    private Mono<AuthorizationDecisionDTO> createDeclinedDecision(
            AuthorizationRequestDTO request,
            AuthorizationReasonCode reasonCode,
            String reasonMessage,
            Integer riskScore,
            LimitSnapshotDTO limitsSnapshot,
            BalanceSnapshotDTO balanceSnapshot,
            List<String> decisionPath) {

        AuthorizationDecisionDTO decision = AuthorizationDecisionDTO.builder()
                .requestId(request.getRequestId())
                .decisionId(generateRandomId())
                .decision(AuthorizationDecisionType.DECLINED)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .approvedAmount(BigDecimal.ZERO)
                .currency(request.getCurrency())
                .riskScore(riskScore)
                .limitsSnapshot(limitsSnapshot)
                .balanceSnapshot(balanceSnapshot)
                .timestamp(LocalDateTime.now())
                .decisionPath(decisionPath)
                .build();

        AuthorizationDecision decisionEntity = decisionMapper.toEntity(decision);
        return decisionRepository.save(decisionEntity)
                .doOnSuccess(savedDecision -> {
                    // Mark the request as processed
                    requestRepository.findByRequestId(request.getRequestId())
                            .flatMap(req -> {
                                req.setProcessed(true);
                                req.setProcessedAt(LocalDateTime.now());
                                return requestRepository.save(req);
                            })
                            .subscribe();
                })
                .map(decisionMapper::toDto);
    }

    /**
     * Create a challenge decision.
     *
     * @param request The authorization request
     * @param reasonMessage The reason message
     * @param riskScore The risk score
     * @param limitsSnapshot The limits snapshot
     * @param balanceSnapshot The balance snapshot
     * @param decisionPath The decision path
     * @return A Mono emitting the authorization decision
     */
    private Mono<AuthorizationDecisionDTO> createChallengeDecision(
            AuthorizationRequestDTO request,
            String reasonMessage,
            Integer riskScore,
            LimitSnapshotDTO limitsSnapshot,
            BalanceSnapshotDTO balanceSnapshot,
            List<String> decisionPath) {

        AuthorizationDecisionDTO decision = AuthorizationDecisionDTO.builder()
                .requestId(request.getRequestId())
                .decisionId(generateRandomId())
                .decision(AuthorizationDecisionType.CHALLENGE)
                .reasonCode(AuthorizationReasonCode.ADDITIONAL_AUTHENTICATION_REQUIRED)
                .reasonMessage(reasonMessage)
                .approvedAmount(request.getAmount())
                .currency(request.getCurrency())
                .riskScore(riskScore)
                .limitsSnapshot(limitsSnapshot)
                .balanceSnapshot(balanceSnapshot)
                .timestamp(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .decisionPath(decisionPath)
                .build();

        AuthorizationDecision decisionEntity = decisionMapper.toEntity(decision);
        return decisionRepository.save(decisionEntity)
                .map(decisionMapper::toDto);
    }

    /**
     * Generate a random authorization code.
     *
     * @return The authorization code
     */
    private String generateAuthorizationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
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
