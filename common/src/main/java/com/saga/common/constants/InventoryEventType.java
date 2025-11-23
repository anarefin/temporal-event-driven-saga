package com.saga.common.constants;

/**
 * Enum representing inventory-specific event types.
 * This provides type-safety for inventory service operations.
 */
public enum InventoryEventType {
    RESERVE_INVENTORY,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    COMPENSATE_INVENTORY,
    INVENTORY_COMPENSATED;

    /**
     * Check if this event type matches the given string
     * @param eventType String to compare
     * @return true if matches
     */
    public boolean matches(String eventType) {
        return this.name().equals(eventType);
    }
}

