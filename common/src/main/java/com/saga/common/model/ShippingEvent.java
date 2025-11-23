package com.saga.common.model;

import com.saga.common.constants.ShippingEventType;
import java.time.Instant;

/**
 * Shipping-specific event for shipping service operations.
 * Contains shipping domain fields like address, tracking number, and carrier.
 */
public class ShippingEvent {
    private String orderId;
    private String eventType; // String for JSON serialization
    private String shippingAddress;
    private String trackingNumber;
    private String carrier;
    private Instant timestamp;

    /**
     * Default constructor for JSON deserialization
     */
    public ShippingEvent() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and eventType string
     */
    public ShippingEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and ShippingEventType enum
     */
    public ShippingEvent(String orderId, ShippingEventType eventType) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.timestamp = Instant.now();
    }

    /**
     * Full constructor with all shipping details
     */
    public ShippingEvent(String orderId, ShippingEventType eventType, String shippingAddress, String trackingNumber, String carrier) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.shippingAddress = shippingAddress;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
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

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
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
    public ShippingEventType getEventTypeEnum() {
        try {
            return ShippingEventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "ShippingEvent{" +
                "orderId='" + orderId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", carrier='" + carrier + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

