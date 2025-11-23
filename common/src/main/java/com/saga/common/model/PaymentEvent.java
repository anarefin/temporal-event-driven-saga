package com.saga.common.model;

import com.saga.common.constants.PaymentEventType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment-specific event for payment service operations.
 * Contains payment domain fields like amount, currency, and payment method.
 */
public class PaymentEvent {
    private String orderId;
    private String eventType; // String for JSON serialization
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private Instant timestamp;

    /**
     * Default constructor for JSON deserialization
     */
    public PaymentEvent() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and eventType string
     */
    public PaymentEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with orderId and PaymentEventType enum
     */
    public PaymentEvent(String orderId, PaymentEventType eventType) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.timestamp = Instant.now();
    }

    /**
     * Full constructor with all payment details
     */
    public PaymentEvent(String orderId, PaymentEventType eventType, BigDecimal amount, String currency, String paymentMethod) {
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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
    public PaymentEventType getEventTypeEnum() {
        try {
            return PaymentEventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "orderId='" + orderId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

