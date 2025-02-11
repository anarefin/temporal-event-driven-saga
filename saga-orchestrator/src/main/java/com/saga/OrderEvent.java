package com.saga;

public class OrderEvent {
    private String orderId;
    private String eventType; // ORDER_CREATED, PAYMENT_COMPLETED, INVENTORY_RESERVED

    public OrderEvent() {}

    public OrderEvent(String orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}

