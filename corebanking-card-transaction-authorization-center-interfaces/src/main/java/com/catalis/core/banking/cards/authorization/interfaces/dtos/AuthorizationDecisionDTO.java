package com.catalis.core.banking.cards.authorization.interfaces.dtos;

import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationDecisionType;
import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing an authorization decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authorization decision for a card transaction")
public class AuthorizationDecisionDTO {

    @Schema(description = "Request ID that this decision is for", example = "123456789")
    private Long requestId;

    @Schema(description = "Decision ID", example = "987654321")
    private Long decisionId;

    @Schema(description = "Decision type", example = "APPROVED")
    private AuthorizationDecisionType decision;

    @Schema(description = "Reason code", example = "APPROVED_TRANSACTION")
    private AuthorizationReasonCode reasonCode;

    @Schema(description = "Detailed reason message", example = "Transaction approved")
    private String reasonMessage;

    @Schema(description = "Approved amount (may differ from requested amount)", example = "125.50")
    private BigDecimal approvedAmount;

    @Schema(description = "Currency of the approved amount", example = "USD")
    private String currency;

    @Schema(description = "Authorization code to be provided to the merchant", example = "123456")
    private String authorizationCode;

    @Schema(description = "Risk score from fraud assessment", example = "25")
    private Integer riskScore;

    @Schema(description = "Snapshot of limits at time of decision")
    private LimitSnapshotDTO limitsSnapshot;

    @Schema(description = "Snapshot of balance at time of decision")
    private BalanceSnapshotDTO balanceSnapshot;

    @Schema(description = "Hold ID if a hold was placed", example = "123456")
    private Long holdId;

    @Schema(description = "Ledger entry ID if posted to ledger", example = "789012")
    private Long ledgerEntryId;

    @Schema(description = "Decision timestamp", example = "2023-04-15T14:30:15")
    private LocalDateTime timestamp;

    @Schema(description = "Expiration time for this authorization", example = "2023-04-22T14:30:15")
    private LocalDateTime expiresAt;

    @Schema(description = "Decision path showing rules that were evaluated")
    private List<String> decisionPath;

    @Schema(description = "Network response data")
    private Map<String, Object> networkResponseData;

    @Schema(description = "Additional data related to the decision")
    private Map<String, Object> additionalData;

    @Schema(description = "Challenge data if additional verification is required")
    private Map<String, Object> challengeData;
}
