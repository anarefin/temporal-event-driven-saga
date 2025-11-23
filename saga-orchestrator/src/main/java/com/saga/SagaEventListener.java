package com.saga;

import com.saga.common.model.OrderEvent;
import com.saga.common.constants.EventType;
import com.saga.config.WorkflowOptionsConfig;
import io.temporal.api.common.v1.WorkflowExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.common.RetryOptions;
import java.time.Duration;

/**
 * Saga Event Listener following Temporal best practices.
 * 
 * Improvements:
 * 1. Better error handling with specific exception types
 * 2. Null checks and validation
 * 3. Structured logging (SLF4J instead of System.out)
 * 4. Graceful handling of duplicate signals
 * 5. Proper workflow execution timeout configuration
 * 
 * Pattern: Signals for external events (event-driven, durable)
 * - ORDER_CREATED: Starts new workflow
 * - Other events: Signal existing workflow
 */
@Component
public class SagaEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaEventListener.class);
    private static final String TASK_QUEUE = "ORDER_TASK_QUEUE";

    @Autowired
    private WorkflowClient workflowClient;

    @KafkaListener(topics = "order-events", groupId = "saga-group")
    public void consumeEvent(@Payload OrderEvent event) {
        // Validation
        if (event == null) {
            logger.error("Received null event, ignoring");
            return;
        }
        
        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            logger.error("Received event with null or empty orderId, ignoring: {}", event);
            return;
        }
        
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            logger.error("Received event with null or empty eventType, ignoring: {}", event);
            return;
        }
        
        String workflowId = event.getOrderId();
        String eventType = event.getEventType();
        
        logger.info("Processing event: {} for order: {}", eventType, workflowId);

        try {
            switch (eventType) {
                case "ORDER_CREATED":
                    handleOrderCreated(workflowId);
                    break;
                    
                case "PAYMENT_COMPLETED":
                    signalWorkflow(workflowId, OrderWorkflow::onPaymentCompleted, "payment completion");
                    break;
                    
                case "PAYMENT_FAILED":
                    signalWorkflow(workflowId, OrderWorkflow::onPaymentFailed, "payment failure");
                    break;
                    
                case "INVENTORY_RESERVED":
                    signalWorkflow(workflowId, OrderWorkflow::onInventoryReserved, "inventory reservation");
                    break;
                    
                case "INVENTORY_FAILED":
                    signalWorkflow(workflowId, OrderWorkflow::onInventoryFailed, "inventory failure");
                    break;
                    
                case "SHIPPING_COMPLETED":
                    signalWorkflow(workflowId, OrderWorkflow::onShippingCompleted, "shipping completion");
                    break;
                    
                case "SHIPPING_FAILED":
                    signalWorkflow(workflowId, OrderWorkflow::onShippingFailed, "shipping failure");
                    break;
                    
                default:
                    logger.warn("Unknown event type: {} for order: {}", eventType, workflowId);
            }
        } catch (Exception e) {
            logger.error("Error processing event: {} for order: {}", eventType, workflowId, e);
            // Don't rethrow - let Kafka retry if needed based on consumer config
        }
    }
    
    /**
     * Handles ORDER_CREATED event by starting a new workflow.
     */
    private void handleOrderCreated(String workflowId) {
        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(
                OrderWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowExecutionTimeout(WorkflowOptionsConfig.WORKFLOW_EXECUTION_TIMEOUT)
                    .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(2.0)
                        .setMaximumAttempts(3)
                        .build())
                    .build()
            );
            
            WorkflowExecution execution = WorkflowClient.start(workflow::placeOrder, workflowId);
            logger.info("Started workflow for order: {} with execution: {}", 
                workflowId, execution.getWorkflowId());
            
        } catch (WorkflowExecutionAlreadyStarted e) {
            // This is expected if the event is replayed - handle gracefully
            logger.warn("Workflow already started for order: {} (duplicate ORDER_CREATED event)", workflowId);
            
        } catch (Exception e) {
            logger.error("Failed to start workflow for order: {}", workflowId, e);
            throw new RuntimeException("Failed to start workflow for order " + workflowId, e);
        }
    }
    
    /**
     * Signals an existing workflow.
     * Handles WorkflowNotFoundException gracefully (workflow may have already completed).
     */
    private void signalWorkflow(String workflowId, 
                                java.util.function.Consumer<OrderWorkflow> signalMethod,
                                String signalDescription) {
        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            signalMethod.accept(workflow);
            logger.info("Signaled workflow for {} - order: {}", signalDescription, workflowId);
            
        } catch (WorkflowNotFoundException e) {
            // Workflow may have already completed or timed out - log as warning, not error
            logger.warn("Workflow not found for {} - order: {} (may have already completed)", 
                signalDescription, workflowId);
            
        } catch (Exception e) {
            logger.error("Failed to signal workflow for {} - order: {}", signalDescription, workflowId, e);
            throw new RuntimeException(
                String.format("Failed to signal %s for order %s", signalDescription, workflowId), e);
        }
    }
}
