package com.saga;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.common.RetryOptions;
import java.time.Duration;

@Component
public class SagaEventListener {

    @Autowired
    private WorkflowClient workflowClient;

    @KafkaListener(topics = "order-events", groupId = "saga-group")
    public void consumeEvent(@Payload OrderEvent event) {
        String workflowId = event.getOrderId();
        
        OrderWorkflow workflow = workflowClient.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("ORDER_TASK_QUEUE")
                .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
                .build()
        );

        switch (event.getEventType()) {
            case "ORDER_CREATED":
                try {
                    // Start the workflow
                    WorkflowClient.start(workflow::placeOrder, workflowId);
                } catch (WorkflowExecutionAlreadyStarted e) {
                    // Workflow already exists, get the existing one
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                }
                break;
                
            case "PAYMENT_COMPLETED":
                try {
                    // Get existing workflow and send signal
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onPaymentCompleted();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal payment completion for order " + workflowId, e);
                }
                break;
                
            case "PAYMENT_FAILED":
                try {
                    // Get existing workflow and send signal
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onPaymentFailed();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal payment failure for order " + workflowId, e);
                }
                break;
                
            case "INVENTORY_RESERVED":
                try {
                    // Get existing workflow and send signal
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onInventoryReserved();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal inventory reservation for order " + workflowId, e);
                }
                break;
                
            case "INVENTORY_FAILED":
                try {
                    // Get existing workflow and send signal
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onInventoryFailed();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal inventory failure for order " + workflowId, e);
                }
                break;
                
            case "SHIPPING_COMPLETED":
                try {
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onShippingCompleted();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal shipping completion for order " + workflowId, e);
                }
                break;
                
            case "SHIPPING_FAILED":
                try {
                    workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
                    workflow.onShippingFailed();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to signal shipping failure for order " + workflowId, e);
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }
}
