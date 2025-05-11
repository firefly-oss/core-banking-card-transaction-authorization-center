package com.catalis.core.banking.cards.authorization.interfaces.dtos;

import com.catalis.core.banking.cards.authorization.interfaces.enums.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Data Transfer Object representing card details retrieved from the card service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Card details from the card service")
public class CardDetailsDTO {

    @Schema(description = "Card ID", example = "555666")
    private Long cardId;

    @Schema(description = "Masked PAN (Primary Account Number)", example = "411111******1111")
    private String maskedPan;

    @Schema(description = "PAN hash for secure identification", example = "a1b2c3d4e5f6g7h8i9j0")
    private String panHash;

    @Schema(description = "Card token (if tokenized)", example = "tkn_abcdef123456")
    private String token;

    @Schema(description = "Card BIN (Bank Identification Number)", example = "411111")
    private String bin;

    @Schema(description = "Card type (CREDIT, DEBIT, PREPAID)", example = "DEBIT")
    private String cardType;

    @Schema(description = "Card brand (VISA, MASTERCARD, etc.)", example = "VISA")
    private String cardBrand;

    @Schema(description = "Card status", example = "ACTIVE")
    private CardStatus status;

    @Schema(description = "Cardholder name", example = "JOHN DOE")
    private String cardholderName;

    @Schema(description = "Expiry date", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(description = "Issue date", example = "2022-01-15")
    private LocalDate issueDate;

    @Schema(description = "Account ID linked to the card", example = "111222")
    private Long accountId;

    @Schema(description = "Account Space ID", example = "333444")
    private Long accountSpaceId;

    @Schema(description = "Customer ID", example = "777888")
    private Long customerId;

    @Schema(description = "3DS enrollment status (Y, N, U)", example = "Y")
    private String threeDsEnrollmentStatus;

    @Schema(description = "Card controls and preferences")
    private Map<String, Object> cardControls;

    @Schema(description = "Custom spending limits")
    private LimitSnapshotDTO customLimits;

    @Schema(description = "Card product code", example = "GOLD_REWARDS")
    private String productCode;

    @Schema(description = "Issuer country code", example = "USA")
    private String issuerCountry;
}
