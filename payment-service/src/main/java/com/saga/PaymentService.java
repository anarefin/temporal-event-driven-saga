package com.saga;

import com.saga.common.model.PaymentEvent;
import com.saga.common.constants.PaymentEventType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.core.KafkaTemplate;

@Component
public class PaymentService {
    
    @Autowired 
    private KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate;
    
    @Value("${payment.simulation.success}")
    private boolean simulateSuccess;

    @KafkaListener(topics = "payment-events", groupId = "payment-group")
    public void consumeEvent(@Payload PaymentEvent event) {
        if (PaymentEventType.PROCESS_PAYMENT.matches(event.getEventType())) {
            System.out.println("Processing payment for order: " + event.getOrderId());
            System.out.println("Payment amount: " + event.getAmount() + " " + event.getCurrency());
            
            // Simulate payment processing with configurable success/failure
            if (simulateSuccess) {
                System.out.println("Payment successful for order: " + event.getOrderId());
                PaymentEvent successEvent = new PaymentEvent(
                    event.getOrderId(), 
                    PaymentEventType.PAYMENT_COMPLETED,
                    event.getAmount(),
                    event.getCurrency(),
                    event.getPaymentMethod()
                );
                paymentKafkaTemplate.send("payment-events", successEvent);
            } else {
                System.out.println("Payment failed for order: " + event.getOrderId());
                PaymentEvent failureEvent = new PaymentEvent(
                    event.getOrderId(), 
                    PaymentEventType.PAYMENT_FAILED
                );
                paymentKafkaTemplate.send("payment-events", failureEvent);
            }
        } else if (PaymentEventType.COMPENSATE_PAYMENT.matches(event.getEventType())) {
            System.out.println("Compensating payment for order: " + event.getOrderId());
            // Implement payment refund logic here
            PaymentEvent compensatedEvent = new PaymentEvent(
                event.getOrderId(), 
                PaymentEventType.PAYMENT_COMPENSATED
            );
            paymentKafkaTemplate.send("payment-events", compensatedEvent);
        }
    }
}

