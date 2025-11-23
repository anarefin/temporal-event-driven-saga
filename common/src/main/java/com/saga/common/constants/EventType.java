package com.saga.common.constants;

/**
 * Enum representing all event types used in the saga pattern orchestration.
 * This provides type-safety and prevents typos in event type strings.
 */
public enum EventType {
    // Order events
    ORDER_CREATED,
    
    // Payment events
    PROCESS_PAYMENT,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    COMPENSATE_PAYMENT,
    PAYMENT_COMPENSATED,
    
    // Inventory events
    RESERVE_INVENTORY,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    COMPENSATE_INVENTORY,
    INVENTORY_COMPENSATED,
    
    // Shipping events
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

