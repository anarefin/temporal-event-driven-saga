package com.saga;

import com.saga.common.model.InventoryEvent;
import com.saga.common.model.OrderEvent;
import com.saga.common.constants.InventoryEventType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class InventoryService {
    @Autowired
    private KafkaTemplate<String, InventoryEvent> inventoryKafkaTemplate;
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> orderKafkaTemplate;
    
    @Value("${inventory.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "inventory-events", groupId = "inventory-group")
    public void consumeEvent(@Payload InventoryEvent event) {
        if (InventoryEventType.RESERVE_INVENTORY.matches(event.getEventType())) {
            System.out.println("Reserving inventory for order: " + event.getOrderId());
            System.out.println("Product: " + event.getProductId() + ", Quantity: " + event.getQuantity());
            
            // Simulate inventory reservation with configurable success/failure
            if (simulateSuccess) {
                System.out.println("Inventory reserved successfully for order: " + event.getOrderId());
                InventoryEvent reservedEvent = new InventoryEvent(
                    event.getOrderId(), 
                    InventoryEventType.INVENTORY_RESERVED,
                    event.getProductId(),
                    event.getQuantity(),
                    event.getWarehouseId()
                );
                orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_RESERVED"));
            } else {
                System.out.println("Inventory reservation failed for order: " + event.getOrderId());
                InventoryEvent failedEvent = new InventoryEvent(
                    event.getOrderId(), 
                    InventoryEventType.INVENTORY_FAILED
                );
                orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_FAILED"));
            }
        } else if (InventoryEventType.COMPENSATE_INVENTORY.matches(event.getEventType())) {
            System.out.println("Compensating inventory reservation for order: " + event.getOrderId());
            // Implement inventory release logic here
            InventoryEvent compensatedEvent = new InventoryEvent(
                event.getOrderId(), 
                InventoryEventType.INVENTORY_COMPENSATED
            );
            orderKafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "INVENTORY_COMPENSATED"));
        }
    }
}
