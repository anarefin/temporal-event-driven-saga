package com.saga;

import com.saga.common.model.OrderEvent;
import com.saga.common.constants.OrderEventType;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @PostMapping("/{orderId}")
    public String createOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String customerId) {
        OrderEvent event = new OrderEvent(orderId, OrderEventType.ORDER_CREATED, customerId);
        kafkaTemplate.send("order-events", event);
        return "Order Creation Request is Initiated for order: " + orderId + 
               (customerId != null ? " (Customer: " + customerId + ")" : "");
    }
}

