package com.saga.common.constants;

/**
 * Enum representing payment-specific event types.
 * This provides type-safety for payment service operations.
 */
public enum PaymentEventType {
    PROCESS_PAYMENT,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    COMPENSATE_PAYMENT,
    PAYMENT_COMPENSATED;

    /**
     * Check if this event type matches the given string
     * @param eventType String to compare
     * @return true if matches
     */
    public boolean matches(String eventType) {
        return this.name().equals(eventType);
    }
}

