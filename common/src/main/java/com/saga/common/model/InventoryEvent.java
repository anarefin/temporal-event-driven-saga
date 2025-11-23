package com.saga.common.model;

import com.saga.common.constants.InventoryEventType;
import java.time.Instant;

/**
 * Inventory-specific event for inventory service operations.
 * Contains inventory domain fields like productId, quantity, and warehouseId.
 */
public class InventoryEvent {
    private String orderId;
    private String eventType; // String for JSON serialization
    private String productId;
    private Integer quantity;
    private String warehouseId;
    private Instant timestamp;

    /**
     * Default constructor for JSON deserialization
     */
    public InventoryEvent() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and eventType string
     */
    public InventoryEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and InventoryEventType enum
     */
    public InventoryEvent(String orderId, InventoryEventType eventType) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.timestamp = Instant.now();
    }

    /**
     * Full constructor with all inventory details
     */
    public InventoryEvent(String orderId, InventoryEventType eventType, String productId, Integer quantity, String warehouseId) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.productId = productId;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the event type as an enum
     */
    public InventoryEventType getEventTypeEnum() {
        try {
            return InventoryEventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "InventoryEvent{" +
                "orderId='" + orderId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", warehouseId='" + warehouseId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

