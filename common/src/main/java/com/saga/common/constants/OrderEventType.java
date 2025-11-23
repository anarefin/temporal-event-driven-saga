package com.saga.common.constants;

/**
 * Enum representing order-specific event types.
 * This provides type-safety for order service operations.
 */
public enum OrderEventType {
    ORDER_CREATED;

    /**
     * Check if this event type matches the given string
     * @param eventType String to compare
     * @return true if matches
     */
    public boolean matches(String eventType) {
        return this.name().equals(eventType);
    }
}

