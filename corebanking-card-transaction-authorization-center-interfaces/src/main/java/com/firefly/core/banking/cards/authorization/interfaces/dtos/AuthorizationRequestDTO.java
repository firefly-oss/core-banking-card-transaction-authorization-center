package com.firefly.core.banking.cards.authorization.interfaces.dtos;

import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionChannel;
import com.firefly.core.banking.cards.authorization.interfaces.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a card authorization request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Card transaction authorization request")
public class AuthorizationRequestDTO {

    @Schema(description = "Unique request ID for idempotency", example = "123456789")
    private Long requestId;

    @NotBlank
    @Schema(description = "Masked PAN (Primary Account Number)", example = "411111******1111")
    private String maskedPan;

    @Schema(description = "PAN hash for secure identification", example = "a1b2c3d4e5f6g7h8i9j0")
    private String panHash;

    @Schema(description = "Card token (if tokenized)", example = "tkn_abcdef123456")
    private String token;

    @NotBlank
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Expiry date must be in MM/YY format")
    @Schema(description = "Card expiry date (MM/YY)", example = "12/25")
    private String expiryDate;

    @NotBlank
    @Schema(description = "Merchant ID", example = "MERCH123456")
    private String merchantId;

    @Schema(description = "Merchant name", example = "Amazon.com")
    private String merchantName;

    @NotNull
    @Schema(description = "Transaction channel", example = "POS")
    private TransactionChannel channel;

    @Schema(description = "Merchant Category Code", example = "5411")
    private String mcc;

    @Schema(description = "Country code (ISO 3166-1 alpha-3)", example = "USA")
    private String countryCode;

    @NotNull
    @Schema(description = "Transaction type", example = "PURCHASE")
    private TransactionType transactionType;

    @NotNull
    @Positive
    @Schema(description = "Transaction amount", example = "125.50")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "Transaction currency (ISO 4217)", example = "USD")
    private String currency;

    @Schema(description = "Transaction timestamp", example = "2023-04-15T14:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Cryptogram for EMV transactions", example = "A1B2C3D4E5F6G7H8")
    private String cryptogram;

    @Schema(description = "PIN verification data (encrypted)", example = "ENCRYPTED_PIN_DATA")
    private String pinData;

    @Schema(description = "3DS authentication data", example = "3DS_AUTH_DATA")
    private String threeDsData;

    @Schema(description = "Additional transaction data", example = "{\"referenceNumber\":\"REF123456\"}")
    private String additionalData;
}
