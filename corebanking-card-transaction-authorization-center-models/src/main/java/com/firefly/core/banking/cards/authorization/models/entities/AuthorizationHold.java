package com.firefly.core.banking.cards.authorization.models.entities;

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
 * Entity representing an authorization hold stored in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("authorization_holds")
public class AuthorizationHold {

    @Id
    @Column("id")
    private Long id;

    @Column("hold_id")
    private Long holdId;

    @Column("request_id")
    private Long requestId;

    @Column("decision_id")
    private Long decisionId;

    @Column("account_id")
    private Long accountId;

    @Column("account_space_id")
    private Long accountSpaceId;

    @Column("card_id")
    private Long cardId;

    @Column("merchant_id")
    private String merchantId;

    @Column("merchant_name")
    private String merchantName;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("original_amount")
    private BigDecimal originalAmount;

    @Column("original_currency")
    private String originalCurrency;

    @Column("exchange_rate")
    private BigDecimal exchangeRate;

    @Column("ledger_entry_id")
    private Long ledgerEntryId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("capture_status")
    private String captureStatus;

    @Column("captured_amount")
    private BigDecimal capturedAmount;

    @Column("captured_at")
    private LocalDateTime capturedAt;

    @Column("authorization_code")
    private String authorizationCode;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
