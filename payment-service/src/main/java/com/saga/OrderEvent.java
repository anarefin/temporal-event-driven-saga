package com.saga;

public class OrderEvent {
    private String orderId;
    private String eventType; // CREATED, PAYMENT_COMPLETED, INVENTORY_RESERVED

    public OrderEvent() {}

    public OrderEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
    }

    public String getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
}

