package com.catalis.core.banking.cards.authorization.interfaces.enums;

/**
 * Represents the different types of card transactions.
 */
public enum TransactionType {
    /**
     * Purchase of goods or services.
     */
    PURCHASE,
    
    /**
     * Cash withdrawal.
     */
    WITHDRAWAL,
    
    /**
     * Balance inquiry.
     */
    BALANCE_INQUIRY,
    
    /**
     * Funds transfer.
     */
    TRANSFER,
    
    /**
     * Payment (e.g., bill payment).
     */
    PAYMENT,
    
    /**
     * Refund to the card.
     */
    REFUND,
    
    /**
     * Pre-authorization (hold funds without capture).
     */
    PRE_AUTHORIZATION,
    
    /**
     * Capture of a previous pre-authorization.
     */
    CAPTURE,
    
    /**
     * Reversal of a previous transaction.
     */
    REVERSAL,
    
    /**
     * PIN change.
     */
    PIN_CHANGE,
    
    /**
     * Other transaction types not specifically categorized.
     */
    OTHER
}
