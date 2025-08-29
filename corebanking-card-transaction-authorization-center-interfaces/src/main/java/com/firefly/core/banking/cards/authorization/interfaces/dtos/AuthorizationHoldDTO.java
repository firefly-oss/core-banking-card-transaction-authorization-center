package com.firefly.core.banking.cards.authorization.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an authorization hold on funds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authorization hold on funds")
public class AuthorizationHoldDTO {

    @Schema(description = "Hold ID", example = "123456")
    private Long holdId;

    @Schema(description = "Request ID that this hold is for", example = "123456789")
    private Long requestId;

    @Schema(description = "Decision ID that created this hold", example = "987654321")
    private Long decisionId;

    @Schema(description = "Account ID", example = "111222")
    private Long accountId;

    @Schema(description = "Account Space ID", example = "333444")
    private Long accountSpaceId;

    @Schema(description = "Card ID", example = "555666")
    private Long cardId;

    @Schema(description = "Merchant ID", example = "MERCH123456")
    private String merchantId;

    @Schema(description = "Merchant name", example = "Amazon.com")
    private String merchantName;

    @Schema(description = "Hold amount", example = "125.50")
    private BigDecimal amount;

    @Schema(description = "Currency of the hold amount", example = "USD")
    private String currency;

    @Schema(description = "Original amount (if currency conversion applied)", example = "100.00")
    private BigDecimal originalAmount;

    @Schema(description = "Original currency (if different from account currency)", example = "EUR")
    private String originalCurrency;

    @Schema(description = "Exchange rate (if currency conversion applied)", example = "1.05")
    private BigDecimal exchangeRate;

    @Schema(description = "Ledger entry ID if posted to ledger", example = "789012")
    private Long ledgerEntryId;

    @Schema(description = "Hold creation timestamp", example = "2023-04-15T14:30:15")
    private LocalDateTime createdAt;

    @Schema(description = "Hold expiration timestamp", example = "2023-04-22T14:30:15")
    private LocalDateTime expiresAt;

    @Schema(description = "Capture status (PENDING, CAPTURED, PARTIALLY_CAPTURED, EXPIRED, RELEASED)", example = "PENDING")
    private String captureStatus;

    @Schema(description = "Amount captured (for partial captures)", example = "100.00")
    private BigDecimal capturedAmount;

    @Schema(description = "Timestamp when the hold was captured", example = "2023-04-16T10:15:30")
    private LocalDateTime capturedAt;

    @Schema(description = "Authorization code", example = "123456")
    private String authorizationCode;
}
