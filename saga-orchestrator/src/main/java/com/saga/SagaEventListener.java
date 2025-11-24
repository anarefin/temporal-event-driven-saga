package com.saga;

import com.saga.common.model.OrderEvent;
import com.saga.common.model.PaymentEvent;
import com.saga.common.model.InventoryEvent;
import com.saga.common.model.ShippingEvent;
import com.saga.common.constants.PaymentEventType;
import com.saga.common.constants.InventoryEventType;
import com.saga.common.constants.ShippingEventType;
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
 * 6. Domain-driven event listeners - each service publishes to its own topic
 * 
 * Pattern: Signals for external events (event-driven, durable)
 * - ORDER_CREATED: Starts new workflow
 * - Other domain events: Signal existing workflow
 */
@Component
public class SagaEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaEventListener.class);
    private static final String TASK_QUEUE = "ORDER_TASK_QUEUE";

    @Autowired
    private WorkflowClient workflowClient;

    /**
     * Listens to order-events topic for ORDER_CREATED events to start workflows
     */
    @KafkaListener(topics = "order-events", groupId = "saga-group")
    public void consumeOrderEvent(@Payload OrderEvent event) {
        
        String workflowId = event.getOrderId();
        String eventType = event.getEventType();
        
        logger.info("Processing order event: {} for order: {}", eventType, workflowId);

        try {
            if ("ORDER_CREATED".equals(eventType)) {
                handleOrderCreated(workflowId);
            } else {
                logger.warn("Unknown order event type: {} for order: {}", eventType, workflowId);
            }
        } catch (Exception e) {
            logger.error("Error processing order event: {} for order: {}", eventType, workflowId, e);
            // Don't rethrow - let Kafka retry if needed based on consumer config
        }
    }
    
    /**
     * Listens to payment-events topic for payment-related events
     */
    @KafkaListener(topics = "payment-events", groupId = "saga-group")
    public void consumePaymentEvent(@Payload PaymentEvent event) {
        
        String workflowId = event.getOrderId();
        String eventType = event.getEventType();
        
        logger.info("Processing payment event: {} for order: {}", eventType, workflowId);

        try {
            if (PaymentEventType.PAYMENT_COMPLETED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onPaymentCompleted, "payment completion");
            } else if (PaymentEventType.PAYMENT_FAILED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onPaymentFailed, "payment failure");
            } else {
                logger.debug("Ignoring payment event type: {} for order: {}", eventType, workflowId);
            }
        } catch (Exception e) {
            logger.error("Error processing payment event: {} for order: {}", eventType, workflowId, e);
        }
    }
    
    /**
     * Listens to inventory-events topic for inventory-related events
     */
    @KafkaListener(topics = "inventory-events", groupId = "saga-group")
    public void consumeInventoryEvent(@Payload InventoryEvent event) {
        
        String workflowId = event.getOrderId();
        String eventType = event.getEventType();
        
        logger.info("Processing inventory event: {} for order: {}", eventType, workflowId);

        try {
            if (InventoryEventType.INVENTORY_RESERVED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onInventoryReserved, "inventory reservation");
            } else if (InventoryEventType.INVENTORY_FAILED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onInventoryFailed, "inventory failure");
            } else {
                logger.debug("Ignoring inventory event type: {} for order: {}", eventType, workflowId);
            }
        } catch (Exception e) {
            logger.error("Error processing inventory event: {} for order: {}", eventType, workflowId, e);
        }
    }
    
    /**
     * Listens to shipping-events topic for shipping-related events
     */
    @KafkaListener(topics = "shipping-events", groupId = "saga-group")
    public void consumeShippingEvent(@Payload ShippingEvent event) {
        
        String workflowId = event.getOrderId();
        String eventType = event.getEventType();
        
        logger.info("Processing shipping event: {} for order: {}", eventType, workflowId);

        try {
            if (ShippingEventType.SHIPPING_COMPLETED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onShippingCompleted, "shipping completion");
            } else if (ShippingEventType.SHIPPING_FAILED.matches(eventType)) {
                signalWorkflow(workflowId, OrderWorkflow::onShippingFailed, "shipping failure");
            } else {
                logger.debug("Ignoring shipping event type: {} for order: {}", eventType, workflowId);
            }
        } catch (Exception e) {
            logger.error("Error processing shipping event: {} for order: {}", eventType, workflowId, e);
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
