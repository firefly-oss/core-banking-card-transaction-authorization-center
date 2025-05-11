package com.catalis.core.banking.cards.authorization.core.services.impl;

import com.catalis.core.banking.cards.authorization.interfaces.dtos.AuthorizationRequestDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.CardDetailsDTO;
import com.catalis.core.banking.cards.authorization.interfaces.dtos.RiskAssessmentDTO;
import com.catalis.core.banking.cards.authorization.interfaces.enums.CardStatus;
import com.catalis.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.catalis.core.banking.cards.authorization.interfaces.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceImplTest {

    @InjectMocks
    private RiskAssessmentServiceImpl riskAssessmentService;

    @BeforeEach
    void setUp() {
        // Set threshold values
        ReflectionTestUtils.setField(riskAssessmentService, "challengeThreshold", 70);
        ReflectionTestUtils.setField(riskAssessmentService, "declineThreshold", 90);
    }

    @Test
    void assessRisk_lowRiskTransaction_returnsLowRiskAssessment() {
        // Arrange
        AuthorizationRequestDTO request = createLowRiskRequest();
        CardDetailsDTO cardDetails = createCardDetails();

        // Act
        Mono<RiskAssessmentDTO> result = riskAssessmentService.assessRisk(request, cardDetails);

        // Assert
        StepVerifier.create(result)
                .assertNext(assessment -> {
                    assertNotNull(assessment);
                    assertTrue(assessment.getRiskScore() < 70);
                    assertEquals("LOW", assessment.getRiskLevel());
                    assertEquals("APPROVE", assessment.getRecommendation());
                })
                .verifyComplete();
    }

    @Test
    void assessRisk_highValueTransaction_returnsHigherRiskScore() {
        // Arrange
        AuthorizationRequestDTO request = createHighValueRequest();
        CardDetailsDTO cardDetails = createCardDetails();

        // Act
        Mono<RiskAssessmentDTO> result = riskAssessmentService.assessRisk(request, cardDetails);

        // Assert
        StepVerifier.create(result)
                .assertNext(assessment -> {
                    assertNotNull(assessment);
                    assertTrue(assessment.getRiskScore() > 0);
                    assertTrue(assessment.getTriggeredRules().contains("high_value_transaction"));
                })
                .verifyComplete();
    }

    @Test
    void shouldChallenge_riskScoreAboveThreshold_returnsTrue() {
        // Arrange
        RiskAssessmentDTO assessment = RiskAssessmentDTO.builder()
                .riskScore(75)
                .riskLevel("MEDIUM")
                .recommendation("CHALLENGE")
                .build();

        // Act
        Mono<Boolean> result = riskAssessmentService.shouldChallenge(assessment);

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDecline_riskScoreAboveThreshold_returnsTrue() {
        // Arrange
        RiskAssessmentDTO assessment = RiskAssessmentDTO.builder()
                .riskScore(95)
                .riskLevel("HIGH")
                .recommendation("DECLINE")
                .build();

        // Act
        Mono<Boolean> result = riskAssessmentService.shouldDecline(assessment);

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    private AuthorizationRequestDTO createLowRiskRequest() {
        return AuthorizationRequestDTO.builder()
                .requestId(123456789L)
                .maskedPan("411111******1111")
                .panHash("a1b2c3d4e5f6g7h8i9j0")
                .expiryDate("12/25")
                .merchantId("MERCH123456")
                .merchantName("Test Merchant")
                .channel(TransactionChannel.POS)
                .mcc("5411") // Grocery stores
                .countryCode("USA")
                .transactionType(TransactionType.PURCHASE)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AuthorizationRequestDTO createHighValueRequest() {
        return AuthorizationRequestDTO.builder()
                .requestId(456789123L)
                .maskedPan("411111******1111")
                .panHash("a1b2c3d4e5f6g7h8i9j0")
                .expiryDate("12/25")
                .merchantId("MERCH123456")
                .merchantName("Test Merchant")
                .channel(TransactionChannel.POS)
                .mcc("5411") // Grocery stores
                .countryCode("USA")
                .transactionType(TransactionType.PURCHASE)
                .amount(new BigDecimal("2000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private CardDetailsDTO createCardDetails() {
        return CardDetailsDTO.builder()
                .cardId(123456789L)
                .maskedPan("411111******1111")
                .panHash("a1b2c3d4e5f6g7h8i9j0")
                .bin("411111")
                .cardType("DEBIT")
                .cardBrand("VISA")
                .status(CardStatus.ACTIVE)
                .cardholderName("JOHN DOE")
                .expiryDate(LocalDate.now().plusYears(2))
                .issueDate(LocalDate.now().minusYears(1))
                .accountId(987654321L)
                .customerId(123123123L)
                .threeDsEnrollmentStatus("Y")
                .productCode("GOLD_REWARDS")
                .issuerCountry("USA")
                .build();
    }
}
