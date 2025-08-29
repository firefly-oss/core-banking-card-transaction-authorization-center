package com.firefly.core.banking.cards.authorization.interfaces.enums;

/**
 * Represents the different channels through which a card transaction can be initiated.
 */
public enum TransactionChannel {
    /**
     * Point of Sale terminal (physical merchant).
     */
    POS,
    
    /**
     * E-commerce transaction (online merchant).
     */
    E_COMMERCE,
    
    /**
     * Automated Teller Machine.
     */
    ATM,
    
    /**
     * Mobile application.
     */
    MOBILE_APP,
    
    /**
     * Contactless payment.
     */
    CONTACTLESS,
    
    /**
     * Manual entry (e.g., telephone order).
     */
    MANUAL_ENTRY,
    
    /**
     * Recurring payment.
     */
    RECURRING,
    
    /**
     * Other channels not specifically categorized.
     */
    OTHER
}
