package com.saga;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Random;

@Component
public class PaymentService {
    
    @Autowired 
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Value("${payment.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void consumeEvent(@Payload OrderEvent event) {
        if ("ORDER_CREATED".equals(event.getEventType())) {
            System.out.println("Processing payment for order: " + event.getOrderId());
            
            // Simulate payment processing with configurable success/failure
            if (simulateSuccess) {
                System.out.println("Payment successful for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "PAYMENT_COMPLETED"));
            } else {
                System.out.println("Payment failed for order: " + event.getOrderId());
                kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "PAYMENT_FAILED"));
            }
        } else if ("COMPENSATE_PAYMENT".equals(event.getEventType())) {
            System.out.println("Compensating payment for order: " + event.getOrderId());
            // Implement payment refund logic here
            kafkaTemplate.send("order-events", new OrderEvent(event.getOrderId(), "PAYMENT_COMPENSATED"));
        }
    }
}

