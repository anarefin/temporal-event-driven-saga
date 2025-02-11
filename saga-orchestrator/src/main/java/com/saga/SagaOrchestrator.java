// package com.saga;

// import org.springframework.stereotype.Component;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.json.JSONObject;
// import io.temporal.client.WorkflowClient;
// import io.temporal.client.WorkflowOptions;

// @Component
// public class SagaOrchestrator {
//     @Autowired private WorkflowClient workflowClient;

//     @KafkaListener(topics = "order-events", groupId = "saga-group")
//     public void processSaga(String message) {
//         JSONObject event = new JSONObject(message);
//         String orderId = event.getString("orderId");
//         String eventType = event.getString("eventType");

//         OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class,
//                 WorkflowOptions.newBuilder()
//                         .setWorkflowId(orderId)
//                         .build());

//         switch (eventType) {
//             case "OrderCreated":
//                 workflow.startOrder(orderId);
//                 break;
//             case "InventoryReserved":
//                 workflow.inventoryReserved(orderId);
//                 break;
//             case "PaymentProcessed":
//                 workflow.paymentProcessed(orderId);
//                 break;
//             case "OrderFailed":
//                 workflow.orderFailed(orderId, "Failure reason");
//                 break;
//         }
//     }
// }

