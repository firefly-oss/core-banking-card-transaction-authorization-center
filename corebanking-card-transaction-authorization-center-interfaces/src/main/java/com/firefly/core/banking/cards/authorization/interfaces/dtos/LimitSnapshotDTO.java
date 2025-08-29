package com.firefly.core.banking.cards.authorization.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object representing a snapshot of spending limits at the time of authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Snapshot of spending limits at the time of authorization")
public class LimitSnapshotDTO {

    @Schema(description = "Daily limit amount", example = "2000.00")
    private BigDecimal dailyLimit;

    @Schema(description = "Amount spent today", example = "750.50")
    private BigDecimal dailySpent;

    @Schema(description = "Remaining daily limit", example = "1249.50")
    private BigDecimal dailyRemaining;

    @Schema(description = "Monthly limit amount", example = "10000.00")
    private BigDecimal monthlyLimit;

    @Schema(description = "Amount spent this month", example = "3500.75")
    private BigDecimal monthlySpent;

    @Schema(description = "Remaining monthly limit", example = "6499.25")
    private BigDecimal monthlyRemaining;

    @Schema(description = "Single transaction limit", example = "1000.00")
    private BigDecimal singleTransactionLimit;

    @Schema(description = "ATM withdrawal daily limit", example = "500.00")
    private BigDecimal atmDailyLimit;

    @Schema(description = "ATM amount withdrawn today", example = "200.00")
    private BigDecimal atmDailySpent;

    @Schema(description = "Contactless transaction limit", example = "100.00")
    private BigDecimal contactlessLimit;

    @Schema(description = "Online transaction limit", example = "1500.00")
    private BigDecimal onlineLimit;

    @Schema(description = "Date of the snapshot", example = "2023-04-15")
    private LocalDate snapshotDate;
}
