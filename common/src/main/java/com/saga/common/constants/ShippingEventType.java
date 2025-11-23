package com.saga.common.constants;

/**
 * Enum representing shipping-specific event types.
 * This provides type-safety for shipping service operations.
 */
public enum ShippingEventType {
    PROCESS_SHIPPING,
    SHIPPING_COMPLETED,
    SHIPPING_FAILED,
    COMPENSATE_SHIPPING,
    SHIPPING_COMPENSATED;

    /**
     * Check if this event type matches the given string
     * @param eventType String to compare
     * @return true if matches
     */
    public boolean matches(String eventType) {
        return this.name().equals(eventType);
    }
}

