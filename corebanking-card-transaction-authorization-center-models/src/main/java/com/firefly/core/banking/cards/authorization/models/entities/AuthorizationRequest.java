package com.firefly.core.banking.cards.authorization.models.entities;

import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionType;
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
 * Entity representing a card authorization request stored in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("authorization_requests")
public class AuthorizationRequest {

    @Id
    @Column("id")
    private Long id;

    @Column("request_id")
    private Long requestId;

    @Column("masked_pan")
    private String maskedPan;

    @Column("pan_hash")
    private String panHash;

    @Column("token")
    private String token;

    @Column("expiry_date")
    private String expiryDate;

    @Column("merchant_id")
    private String merchantId;

    @Column("merchant_name")
    private String merchantName;

    @Column("channel")
    private TransactionChannel channel;

    @Column("mcc")
    private String mcc;

    @Column("country_code")
    private String countryCode;

    @Column("transaction_type")
    private TransactionType transactionType;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("timestamp")
    private LocalDateTime timestamp;

    @Column("cryptogram")
    private String cryptogram;

    @Column("pin_data")
    private String pinData;

    @Column("three_ds_data")
    private String threeDsData;

    @Column("additional_data")
    private String additionalData;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("processed")
    private Boolean processed;

    @Column("processed_at")
    private LocalDateTime processedAt;
}
