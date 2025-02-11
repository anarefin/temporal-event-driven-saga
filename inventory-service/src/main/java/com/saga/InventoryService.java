package com.saga;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class InventoryService {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Value("${inventory.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "inventory-events", groupId = "inventory-group")
    public void consumeEvent(@Payload OrderEvent event) {
        if ("RESERVE_INVENTORY".equals(event.getEventType())) {
            System.out.println("Reserving inventory for order: " + event.getOrderId());
            
            // Simulate inventory reservation with configurable success/failure
            if (simulateSuccess) {
                System.out.println("Inventory reserved successfully for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_RESERVED"));
            } else {
                System.out.println("Inventory reservation failed for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_FAILED"));
            }
        } else if ("COMPENSATE_INVENTORY".equals(event.getEventType())) {
            System.out.println("Compensating inventory reservation for order: " + event.getOrderId());
            // Implement inventory release logic here
            kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_COMPENSATED"));
        }
    }
}
