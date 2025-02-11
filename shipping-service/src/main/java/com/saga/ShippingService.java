package com.saga;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ShippingService {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Value("${shipping.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "shipping-events", groupId = "shipping-group")
    public void consumeEvent(@Payload OrderEvent event) {
        if ("PROCESS_SHIPPING".equals(event.getEventType())) {
            System.out.println("Processing shipping for order: " + event.getOrderId());
                
            if (simulateSuccess) {
                System.out.println("Shipping completed successfully for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_COMPLETED"));
            } else {
                System.out.println("Shipping failed for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_FAILED"));
            }
        } else if ("COMPENSATE_SHIPPING".equals(event.getEventType())) {
            System.out.println("Compensating shipping for order: " + event.getOrderId());
            kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "SHIPPING_COMPENSATED"));
        }
    }
} 