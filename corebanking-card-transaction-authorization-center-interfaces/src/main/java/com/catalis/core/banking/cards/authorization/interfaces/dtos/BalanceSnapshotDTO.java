package com.catalis.core.banking.cards.authorization.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a snapshot of account balance before and after authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Snapshot of account balance before and after authorization")
public class BalanceSnapshotDTO {

    @Schema(description = "Account ID", example = "111222")
    private Long accountId;

    @Schema(description = "Account Space ID", example = "333444")
    private Long accountSpaceId;

    @Schema(description = "Account currency", example = "USD")
    private String currency;

    @Schema(description = "Available balance before authorization", example = "1250.75")
    private BigDecimal availableBalanceBefore;

    @Schema(description = "Available balance after authorization", example = "1125.25")
    private BigDecimal availableBalanceAfter;

    @Schema(description = "Ledger balance", example = "1350.00")
    private BigDecimal ledgerBalance;

    @Schema(description = "Total amount on hold", example = "224.75")
    private BigDecimal totalHoldAmount;

    @Schema(description = "Exchange rate (if currency conversion applied)", example = "1.05")
    private BigDecimal exchangeRate;

    @Schema(description = "Original currency (if different from account currency)", example = "EUR")
    private String originalCurrency;

    @Schema(description = "Original amount (if currency conversion applied)", example = "100.00")
    private BigDecimal originalAmount;

    @Schema(description = "Timestamp of the balance snapshot", example = "2023-04-15T14:30:15")
    private LocalDateTime timestamp;
}
