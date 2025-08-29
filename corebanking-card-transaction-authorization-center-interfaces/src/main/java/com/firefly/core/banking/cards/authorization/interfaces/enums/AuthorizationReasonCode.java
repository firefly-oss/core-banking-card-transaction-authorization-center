package com.firefly.core.banking.cards.authorization.interfaces.enums;

/**
 * Reason codes for authorization decisions, providing detailed information about
 * why a transaction was approved, declined, or challenged.
 */
public enum AuthorizationReasonCode {
    // Approval codes
    APPROVED_TRANSACTION("00", "Approved transaction", true),
    APPROVED_WITH_ID("08", "Approved with identification", true),
    APPROVED_PARTIAL("10", "Approved for partial amount", true),
    APPROVED_VIP("11", "Approved VIP", true),
    
    // Decline codes - Card related
    INVALID_CARD("14", "Invalid card number", false),
    EXPIRED_CARD("54", "Expired card", false),
    CARD_NOT_ACTIVE("62", "Card not active", false),
    CARD_RESTRICTED("36", "Card restricted", false),
    CARD_LOST_STOLEN("41", "Card reported lost or stolen", false),
    
    // Decline codes - Limit related
    EXCEEDS_WITHDRAWAL_LIMIT("61", "Exceeds withdrawal limit", false),
    EXCEEDS_DAILY_LIMIT("65", "Exceeds daily limit", false),
    EXCEEDS_MONTHLY_LIMIT("66", "Exceeds monthly limit", false),
    EXCEEDS_TRANSACTION_LIMIT("13", "Exceeds transaction limit", false),
    
    // Decline codes - Funds related
    INSUFFICIENT_FUNDS("51", "Insufficient funds", false),
    ACCOUNT_CLOSED("64", "Account closed", false),
    
    // Decline codes - Security related
    SUSPECTED_FRAUD("59", "Suspected fraud", false),
    SECURITY_VIOLATION("63", "Security violation", false),
    INVALID_PIN("55", "Invalid PIN", false),
    EXCEEDS_PIN_RETRIES("75", "Exceeds PIN retries", false),
    
    // Challenge codes
    VERIFICATION_REQUIRED("01", "Verification required", false),
    ADDITIONAL_AUTHENTICATION_REQUIRED("02", "Additional authentication required", false),
    
    // System related
    SYSTEM_ERROR("96", "System error", false),
    FORMAT_ERROR("30", "Format error", false),
    DUPLICATE_TRANSACTION("94", "Duplicate transaction", false),
    ISSUER_UNAVAILABLE("91", "Issuer unavailable", false);
    
    private final String code;
    private final String description;
    private final boolean isApproval;
    
    AuthorizationReasonCode(String code, String description, boolean isApproval) {
        this.code = code;
        this.description = description;
        this.isApproval = isApproval;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isApproval() {
        return isApproval;
    }
    
    /**
     * Find a reason code by its numeric code.
     *
     * @param code The numeric code to search for
     * @return The matching reason code or null if not found
     */
    public static AuthorizationReasonCode findByCode(String code) {
        for (AuthorizationReasonCode reasonCode : values()) {
            if (reasonCode.getCode().equals(code)) {
                return reasonCode;
            }
        }
        return null;
    }
}
