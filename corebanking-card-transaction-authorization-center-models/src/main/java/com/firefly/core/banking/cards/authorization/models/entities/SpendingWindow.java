package com.firefly.core.banking.cards.authorization.models.entities;

import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a spending window for tracking limits.
 * This is used to track daily, monthly, or other time-based spending limits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("spending_windows")
public class SpendingWindow {

    @Id
    @Column("id")
    private Long id;

    @Column("card_id")
    private Long cardId;

    @Column("account_id")
    private Long accountId;

    @Column("account_space_id")
    private Long accountSpaceId;

    @Column("window_type")
    private String windowType; // DAILY, MONTHLY, etc.

    @Column("channel")
    private TransactionChannel channel;

    @Column("country_code")
    private String countryCode;

    @Column("mcc")
    private String mcc;

    @Column("window_date")
    private LocalDate windowDate;

    @Column("window_month")
    private Integer windowMonth;

    @Column("window_year")
    private Integer windowYear;

    @Column("limit_amount")
    private BigDecimal limitAmount;

    @Column("spent_amount")
    private BigDecimal spentAmount;

    @Column("remaining_amount")
    private BigDecimal remainingAmount;

    @Column("transaction_count")
    private Integer transactionCount;

    @Column("last_transaction_id")
    private Long lastTransactionId;

    @Column("last_transaction_time")
    private LocalDateTime lastTransactionTime;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
