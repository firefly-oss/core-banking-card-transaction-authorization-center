package com.catalis.core.banking.cards.authorization.models.entities;

import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationDecisionType;
import com.catalis.core.banking.cards.authorization.interfaces.enums.AuthorizationReasonCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an authorization decision stored in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("authorization_decisions")
public class AuthorizationDecision {

    @Id
    @Column("id")
    private Long id;

    @Column("decision_id")
    private Long decisionId;

    @Column("request_id")
    private Long requestId;

    @Column("decision")
    private AuthorizationDecisionType decision;

    @Column("reason_code")
    private AuthorizationReasonCode reasonCode;

    @Column("reason_message")
    private String reasonMessage;

    @Column("approved_amount")
    private BigDecimal approvedAmount;

    @Column("currency")
    private String currency;

    @Column("authorization_code")
    private String authorizationCode;

    @Column("risk_score")
    private Integer riskScore;

    @Column("hold_id")
    private Long holdId;

    @Column("ledger_entry_id")
    private Long ledgerEntryId;

    @Column("timestamp")
    private LocalDateTime timestamp;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("decision_path")
    private String decisionPath;

    @Column("network_response_data")
    private String networkResponseData;

    @Column("additional_data")
    private String additionalData;

    @Column("challenge_data")
    private String challengeData;

    @Column("daily_limit")
    private BigDecimal dailyLimit;

    @Column("daily_spent")
    private BigDecimal dailySpent;

    @Column("daily_remaining")
    private BigDecimal dailyRemaining;

    @Column("monthly_limit")
    private BigDecimal monthlyLimit;

    @Column("monthly_spent")
    private BigDecimal monthlySpent;

    @Column("monthly_remaining")
    private BigDecimal monthlyRemaining;

    @Column("account_id")
    private Long accountId;

    @Column("account_space_id")
    private Long accountSpaceId;

    @Column("available_balance_before")
    private BigDecimal availableBalanceBefore;

    @Column("available_balance_after")
    private BigDecimal availableBalanceAfter;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
