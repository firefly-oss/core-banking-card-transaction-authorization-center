package com.firefly.core.banking.cards.authorization.web.controllers;

import com.firefly.core.banking.cards.authorization.core.services.AuthorizationService;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationDecisionDTO;
import com.firefly.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.firefly.core.banking.cards.authorization.interfaces.enums.AuthorizationDecisionType;
import com.firefly.core.banking.cards.authorization.interfaces.enums.AuthorizationReasonCode;
import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void authorizeTransaction_validRequest_returnsApprovedDecision() {
        // Arrange
        AuthorizationRequestDTO request = createSampleRequest();
        AuthorizationDecisionDTO decision = createApprovedDecision(request);

        when(authorizationService.authorizeTransaction(any(AuthorizationRequestDTO.class)))
                .thenReturn(Mono.just(decision));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthorizationDecisionDTO.class)
                .isEqualTo(decision);
    }

    @Test
    void authorizeTransaction_validRequest_returnsDeclinedDecision() {
        // Arrange
        AuthorizationRequestDTO request = createSampleRequest();
        AuthorizationDecisionDTO decision = createDeclinedDecision(request);

        when(authorizationService.authorizeTransaction(any(AuthorizationRequestDTO.class)))
                .thenReturn(Mono.just(decision));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(422) // Unprocessable Entity
                .expectBody(AuthorizationDecisionDTO.class)
                .isEqualTo(decision);
    }

    @Test
    void getDecision_existingDecision_returnsDecision() {
        // Arrange
        Long decisionId = 987654321L;
        AuthorizationDecisionDTO decision = createApprovedDecision(createSampleRequest());
        decision.setDecisionId(decisionId);

        when(authorizationService.getDecisionById(decisionId))
                .thenReturn(Mono.just(decision));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/authorizations/{decisionId}", decisionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthorizationDecisionDTO.class)
                .isEqualTo(decision);
    }

    @Test
    void getDecision_nonExistingDecision_returnsNotFound() {
        // Arrange
        Long decisionId = 987654321L;

        when(authorizationService.getDecisionById(decisionId))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/authorizations/{decisionId}", decisionId)
                .exchange()
                .expectStatus().isNotFound();
    }

    private AuthorizationRequestDTO createSampleRequest() {
        return AuthorizationRequestDTO.builder()
                .requestId(123456789L)
                .maskedPan("411111******1111")
                .panHash("a1b2c3d4e5f6g7h8i9j0")
                .expiryDate("12/25")
                .merchantId("MERCH123456")
                .merchantName("Test Merchant")
                .channel(TransactionChannel.POS)
                .mcc("5411")
                .countryCode("USA")
                .transactionType(TransactionType.PURCHASE)
                .amount(new BigDecimal("125.50"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AuthorizationDecisionDTO createApprovedDecision(AuthorizationRequestDTO request) {
        return AuthorizationDecisionDTO.builder()
                .requestId(request.getRequestId())
                .decisionId(987654321L)
                .decision(AuthorizationDecisionType.APPROVED)
                .reasonCode(AuthorizationReasonCode.APPROVED_TRANSACTION)
                .reasonMessage("Transaction approved")
                .approvedAmount(request.getAmount())
                .currency(request.getCurrency())
                .authorizationCode("123456")
                .riskScore(25)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AuthorizationDecisionDTO createDeclinedDecision(AuthorizationRequestDTO request) {
        return AuthorizationDecisionDTO.builder()
                .requestId(request.getRequestId())
                .decisionId(987654321L)
                .decision(AuthorizationDecisionType.DECLINED)
                .reasonCode(AuthorizationReasonCode.INSUFFICIENT_FUNDS)
                .reasonMessage("Insufficient funds")
                .approvedAmount(BigDecimal.ZERO)
                .currency(request.getCurrency())
                .riskScore(25)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
