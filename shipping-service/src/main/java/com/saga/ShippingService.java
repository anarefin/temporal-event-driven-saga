package com.saga;

import com.saga.common.model.ShippingEvent;
import com.saga.common.model.OrderEvent;
import com.saga.common.constants.ShippingEventType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ShippingService {
    
    @Autowired
    private KafkaTemplate<String, ShippingEvent> shippingKafkaTemplate;
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> orderKafkaTemplate;
    
    @Value("${shipping.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "shipping-events", groupId = "shipping-group")
    public void consumeEvent(@Payload ShippingEvent event) {
        if (ShippingEventType.PROCESS_SHIPPING.matches(event.getEventType())) {
            System.out.println("Processing shipping for order: " + event.getOrderId());
            System.out.println("Shipping address: " + event.getShippingAddress());
                
            if (simulateSuccess) {
                System.out.println("Shipping completed successfully for order: " + event.getOrderId());
                ShippingEvent completedEvent = new ShippingEvent(
                    event.getOrderId(), 
                    ShippingEventType.SHIPPING_COMPLETED,
                    event.getShippingAddress(),
                    "TRACK-" + event.getOrderId(), // Generate tracking number
                    "Standard Carrier"
                );
                orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_COMPLETED"));
            } else {
                System.out.println("Shipping failed for order: " + event.getOrderId());
                ShippingEvent failedEvent = new ShippingEvent(
                    event.getOrderId(), 
                    ShippingEventType.SHIPPING_FAILED
                );
                orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_FAILED"));
            }
        } else if (ShippingEventType.COMPENSATE_SHIPPING.matches(event.getEventType())) {
            System.out.println("Compensating shipping for order: " + event.getOrderId());
            ShippingEvent compensatedEvent = new ShippingEvent(
                event.getOrderId(), 
                ShippingEventType.SHIPPING_COMPENSATED
            );
            orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_COMPENSATED"));
        }
    }
} 