package com.saga;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @PostMapping("/{orderId}")
    public String createOrder(@PathVariable String orderId) {
        OrderEvent event = new OrderEvent(orderId, "ORDER_CREATED");
        kafkaTemplate.send("order-events", event);
        return "Order Creation Request is Initiated!";
    }
}

