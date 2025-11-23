package com.saga.common.model;

import com.saga.common.constants.OrderEventType;
import java.time.Instant;

/**
 * Order-specific event for order service operations.
 * Contains order domain fields like customerId and order-specific metadata.
 */
public class OrderEvent {
    private String orderId;
    private String eventType; // String representation for JSON serialization compatibility
    private String customerId;
    private Instant timestamp;

    /**
     * Default constructor for JSON deserialization
     */
    public OrderEvent() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and eventType string
     * @param orderId The order identifier
     * @param eventType The event type as a string
     */
    public OrderEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and OrderEventType enum
     * @param orderId The order identifier
     * @param eventType The event type enum
     */
    public OrderEvent(String orderId, OrderEventType eventType) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.timestamp = Instant.now();
    }

    /**
     * Full constructor with all order details
     */
    public OrderEvent(String orderId, OrderEventType eventType, String customerId) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.customerId = customerId;
        this.timestamp = Instant.now();
    }

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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the event type as an enum
     * @return OrderEventType enum value
     */
    public OrderEventType getEventTypeEnum() {
        try {
            return OrderEventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "orderId='" + orderId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", customerId='" + customerId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

