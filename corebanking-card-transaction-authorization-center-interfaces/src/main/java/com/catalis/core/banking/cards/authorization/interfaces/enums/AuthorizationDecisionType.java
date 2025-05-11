package com.catalis.core.banking.cards.authorization.interfaces.enums;

/**
 * Represents the possible outcomes of a card authorization request.
 */
public enum AuthorizationDecisionType {
    /**
     * The transaction is fully approved.
     */
    APPROVED,
    
    /**
     * The transaction is declined.
     */
    DECLINED,
    
    /**
     * Additional verification is required (e.g., 3DS, OTP).
     */
    CHALLENGE,
    
    /**
     * The transaction is approved for a partial amount.
     */
    PARTIAL
}
