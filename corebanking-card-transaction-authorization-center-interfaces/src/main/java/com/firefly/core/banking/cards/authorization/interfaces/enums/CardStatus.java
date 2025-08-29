package com.firefly.core.banking.cards.authorization.interfaces.enums;

/**
 * Represents the possible statuses of a payment card.
 */
public enum CardStatus {
    /**
     * Card is active and can be used for transactions.
     */
    ACTIVE,
    
    /**
     * Card is inactive and cannot be used until activated.
     */
    INACTIVE,
    
    /**
     * Card is blocked due to suspicious activity or at customer request.
     */
    BLOCKED,
    
    /**
     * Card is temporarily frozen by the customer.
     */
    FROZEN,
    
    /**
     * Card has expired.
     */
    EXPIRED,
    
    /**
     * Card has been reported as lost.
     */
    LOST,
    
    /**
     * Card has been reported as stolen.
     */
    STOLEN,
    
    /**
     * Card has been permanently closed.
     */
    CLOSED,
    
    /**
     * Card is in the process of being issued.
     */
    PENDING_ACTIVATION,
    
    /**
     * Card is in the process of being replaced.
     */
    PENDING_REPLACEMENT
}
