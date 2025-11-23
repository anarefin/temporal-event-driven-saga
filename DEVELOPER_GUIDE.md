# Developer Guide: Temporal Event-Driven Saga Pattern

## Project Overview

This project implements a **distributed transaction management system** using the **Saga Pattern** with **Temporal Workflows** and **Apache Kafka** for event-driven communication. It demonstrates how to handle complex, multi-step business processes across microservices while ensuring consistency and handling failures gracefully.

## Architecture

### System Components

The system consists of 5 microservices:

1. **Order Service** (Port 8081) - Entry point for order creation
2. **Payment Service** - Processes payment transactions
3. **Inventory Service** - Manages product inventory reservations
4. **Shipping Service** - Handles shipping logistics
5. **Saga Orchestrator** (Port 8085) - Coordinates the entire saga workflow using Temporal

### Communication Flow

```
┌─────────────┐
│ Order       │ POST /orders/{orderId}
│ Service     │──────────────────────┐
└─────────────┘                       │
                                      ▼
                            ┌──────────────────┐
                            │ Kafka Topic:     │
                            │ order-events     │
                            └──────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ Saga             │
                            │ Orchestrator     │◄─── Temporal Workflow
                            │ (Temporal)       │
                            └──────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
            ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
            │ Payment      │  │ Inventory    │  │ Shipping     │
            │ Service      │  │ Service      │  │ Service      │
            └──────────────┘  └──────────────┘  └──────────────┘
```

## How Temporal Works in This Project

### What is Temporal?

**Temporal** is a workflow orchestration platform that provides:
- **Durable execution**: Workflows survive service restarts and failures
- **State management**: Maintains workflow state automatically
- **Signal handling**: Allows external events to trigger workflow state changes
- **Compensation support**: Built-in Saga pattern support for distributed transactions

### Temporal Architecture in This Project

**IMPORTANT: Hybrid Activities + Signals Pattern**

This implementation follows Temporal best practices by using a **hybrid approach**:

1. **Activities**: For all outbound actions (side effects)
   - Publishing events to Kafka
   - Compensations (refunds, inventory release, etc.)
   - Why: Keeps workflow code deterministic and replayable
   
2. **Signals**: For all inbound events (external responses)
   - Service responses (PAYMENT_COMPLETED, INVENTORY_RESERVED, etc.)
   - Why: Durable, won't be lost even if workflow isn't actively running
   
3. **Queries**: For external status checking
   - Get workflow state without affecting execution
   - Why: Non-intrusive monitoring and debugging

**Why This Pattern Matters:**

❌ **Anti-pattern** (Don't do this):
```java
@Override
public void placeOrder(String orderId) {
    // Publishing directly in workflow code - BAD!
    kafkaTemplate.send("payment-events", event);  
    // This gets replayed on every workflow event
    // Causes duplicate Kafka messages
}
```

✅ **Best Practice** (What we do):
```java
@Override
public void placeOrder(String orderId) {
    // Use Activity for side effect - GOOD!
    activities.publishPaymentRequest(orderId);
    // Only executed once, not replayed
    // Activity handles retries independently
    
    // Wait for signal with timeout
    Workflow.await(Duration.ofMinutes(5),
        () -> paymentCompleted.isCompleted() || paymentFailed.isCompleted());
}
```

#### 1. **Temporal Configuration** (`TemporalConfig.java`)

The `TemporalConfig` class sets up the Temporal infrastructure:

```java
// Creates connection to Temporal server (localhost:7233)
WorkflowServiceStubs workflowServiceStubs()

// Creates a workflow client with namespace "default"
WorkflowClient workflowClient()

// Creates a worker factory that listens to "ORDER_TASK_QUEUE"
WorkerFactory workerFactory()

// Registers workflow implementations
worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class)

// Registers activity implementations (CRITICAL for best practices!)
worker.registerActivitiesImplementations(orderActivities)
```

**Key Concepts:**
- **Task Queue**: `ORDER_TASK_QUEUE` - A queue where workflow tasks are dispatched
- **Worker**: Processes workflow tasks from the task queue and executes activities
- **Workflow Client**: Used to start and signal workflows
- **Activity Registration**: Activities must be registered for the worker to execute them

#### 2. **Activities Interface** (`OrderActivities.java`)

Activities handle all side effects (Kafka publishing, external calls):

```java
@ActivityInterface
public interface OrderActivities {
    @ActivityMethod
    void publishPaymentRequest(String orderId);
    
    @ActivityMethod
    void publishInventoryRequest(String orderId);
    
    @ActivityMethod
    void publishShippingRequest(String orderId);
    
    @ActivityMethod
    void compensatePayment(String orderId);
    
    @ActivityMethod
    void compensateInventory(String orderId);
    
    @ActivityMethod
    void compensateShipping(String orderId);
}
```

**Key Concepts:**
- **@ActivityInterface**: Marks this as an activity interface
- **@ActivityMethod**: Each method represents a single unit of work
- **Activities** can retry independently with their own retry policies
- **Side effects** (Kafka publishing) are isolated from workflow logic

**Activity Implementation** (`OrderActivitiesImpl.java`):
- Uses Spring's `KafkaTemplate` to publish events
- Proper error handling and logging
- Throws exceptions on failure (triggers Temporal's retry logic)

#### 3. **Workflow Interface** (`OrderWorkflow.java`)

Defines the contract for the order workflow:

```java
@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod
    void placeOrder(String orderId);  // Entry point
    
    @SignalMethod
    void onPaymentCompleted();        // External signal
    
    @SignalMethod
    void onPaymentFailed();          // External signal
    
    // ... more signal methods
    
    @QueryMethod
    OrderWorkflowState getState();    // Query workflow state
    
    @QueryMethod
    String getStatus();               // Query order status
}
```

**Key Concepts:**
- **@WorkflowMethod**: Marks the main entry point of the workflow
- **@SignalMethod**: Allows external services to send events to the workflow (durable)
- **@QueryMethod**: Allows external queries without affecting workflow execution
- **Workflow ID**: Unique identifier (uses `orderId` in this case)

#### 4. **Workflow Implementation** (`OrderWorkflowImpl.java`)

This is where the **Saga orchestration logic** lives with best practices:

**How It Works:**

1. **Activity Stubs for Side Effects**:
   ```java
   // Regular activities for actions
   private final OrderActivities activities = Workflow.newActivityStub(
       OrderActivities.class,
       WorkflowOptionsConfig.getDefaultActivityOptions()
   );
   
   // Separate stub for compensations with more retries
   private final OrderActivities compensationActivities = Workflow.newActivityStub(
       OrderActivities.class,
       WorkflowOptionsConfig.getCompensationActivityOptions()
   );
   ```
   - Activity stubs are proxies for executing activities
   - Different retry policies for regular vs compensation activities
   - Keeps workflow code deterministic

2. **Promise-Based State Management**:
   ```java
   CompletablePromise<Void> paymentCompleted = Workflow.newPromise();
   CompletablePromise<Void> inventoryReserved = Workflow.newPromise();
   // ... more promises
   ```
   - These promises represent future events that the workflow is waiting for
   - They are completed when external signals arrive

3. **Saga Pattern Implementation with Real Compensations**:
   ```java
   Saga saga = new Saga(sagaOptions);
   
   // Use Activities for actual compensation (not just logs!)
   saga.addCompensation(() -> compensationActivities.compensatePayment(orderId));
   ```
   - Temporal's `Saga` class provides automatic compensation handling
   - Compensations invoke actual Activities (publish to Kafka)
   - If any step fails, compensations execute automatically in reverse order

4. **Workflow Execution Flow with Timeouts**:
   ```java
   public void placeOrder(String orderId) {
       // Step 1: Publish payment request via Activity
       activities.publishPaymentRequest(orderId);
       
       // Step 2: Wait for signal WITH TIMEOUT
       boolean received = Workflow.await(
           Duration.ofMinutes(5),  // Timeout!
           () -> paymentCompleted.isCompleted() || paymentFailed.isCompleted()
       );
       
       if (!received) {
           throw new PaymentFailedException(orderId, "Payment timeout");
       }
       
       if (paymentFailed.isCompleted()) {
           return; // Exit early, no compensation needed
       }
       
       // Step 3: Publish inventory request via Activity
       activities.publishInventoryRequest(orderId);
       
       // Step 4: Wait with timeout...
       // ... similar pattern
   }
   ```

**Key Temporal Features Used:**

- **`Workflow.newActivityStub()`**: Creates proxy for executing activities
- **`activities.publishPaymentRequest()`**: Executes activity (side effect)
- **`Workflow.await(timeout, condition)`**: Pauses workflow with timeout protection
- **`CompletablePromise`**: Represents future completion of async operations
- **`Saga.compensate()`**: Triggers all registered compensation handlers in reverse order
- **`Workflow.getLogger()`**: Structured logging that survives replays

**5. Structured State Tracking**:
   ```java
   private final OrderWorkflowState state = new OrderWorkflowState();
   
   state.setCurrentStep(WorkflowStep.PAYMENT);
   state.addCompletedStep(WorkflowStep.PAYMENT);
   state.setStatus(OrderStatus.COMPLETED);
   ```
   - Clean state model for queryability
   - Tracks progress through workflow
   - Accessible via query methods

#### 5. **Event Listener** (`SagaEventListener.java`)

This component bridges **Kafka events** with **Temporal workflows** with improved error handling:

**How It Works:**

1. **Listens to Kafka Events**:
   ```java
   @KafkaListener(topics = "order-events", groupId = "saga-group")
   public void consumeEvent(@Payload OrderEvent event)
   ```

2. **Validates Events** (Best Practice):
   ```java
   if (event == null || event.getOrderId() == null) {
       logger.error("Invalid event, ignoring");
       return;
   }
   ```

3. **Starts Workflows** (for `ORDER_CREATED`):
   ```java
   workflow = workflowClient.newWorkflowStub(
       OrderWorkflow.class,
       WorkflowOptions.newBuilder()
           .setWorkflowId(workflowId)
           .setTaskQueue("ORDER_TASK_QUEUE")
           .setWorkflowExecutionTimeout(Duration.ofMinutes(30))  // Timeout!
           .setRetryOptions(retryOptions)
           .build()
   );
   WorkflowClient.start(workflow::placeOrder, workflowId);
   ```

4. **Signals Existing Workflows** (for other events):
   ```java
   try {
       workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
       workflow.onPaymentCompleted();  // Sends signal to workflow
   } catch (WorkflowNotFoundException e) {
       logger.warn("Workflow not found (may have completed)");
       // Graceful handling - not an error!
   }
   ```

**Key Concepts:**
- **Workflow Stub**: A proxy to interact with a workflow instance
- **Workflow ID**: Must be unique and stable (uses `orderId`)
- **Signal**: Asynchronous, durable message sent to a running workflow
- **Graceful Error Handling**: Handles duplicate events, missing workflows, etc.
- **Structured Logging**: SLF4J instead of System.out for production-ready logging

## Complete Order Flow (With Best Practices)

### Happy Path (Success Scenario)

1. **Order Created**:
   ```
   POST /orders/123
   → Order Service publishes ORDER_CREATED to Kafka
   → SagaEventListener receives event
   → Starts Temporal workflow with ID "123"
   → Workflow begins execution
   ```

2. **Payment Step** (Activities + Signals):
   ```
   → Workflow: activities.publishPaymentRequest("123")  [Activity executes]
   → Activity publishes PROCESS_PAYMENT to payment-events topic
   → Workflow: Workflow.await(timeout, condition)  [Waits for signal]
   → Payment Service processes payment
   → Payment Service publishes PAYMENT_COMPLETED to order-events topic
   → SagaEventListener receives event
   → SagaEventListener: workflow.onPaymentCompleted()  [Sends signal]
   → Workflow's paymentCompleted promise completes
   → Workflow continues to next step
   ```

3. **Inventory Step** (Activities + Signals):
   ```
   → Workflow: activities.publishInventoryRequest("123")  [Activity executes]
   → Activity publishes RESERVE_INVENTORY to inventory-events topic
   → Workflow: Workflow.await(timeout, condition)  [Waits for signal]
   → Inventory Service reserves inventory
   → Inventory Service publishes INVENTORY_RESERVED to order-events topic
   → SagaEventListener: workflow.onInventoryReserved()  [Sends signal]
   → Workflow continues to next step
   ```

4. **Shipping Step** (Activities + Signals):
   ```
   → Workflow: activities.publishShippingRequest("123")  [Activity executes]
   → Activity publishes PROCESS_SHIPPING to shipping-events topic
   → Workflow: Workflow.await(timeout, condition)  [Waits for signal]
   → Shipping Service processes shipping
   → Shipping Service publishes SHIPPING_COMPLETED to order-events topic
   → SagaEventListener: workflow.onShippingCompleted()  [Sends signal]
   → Workflow completes successfully
   → State set to COMPLETED
   ```

### Failure Scenario (Compensation with Real Actions)

If **inventory reservation fails**:

1. Inventory Service publishes `INVENTORY_FAILED`
2. SagaEventListener signals workflow: `workflow.onInventoryFailed()`
3. Workflow detects failure and calls `saga.compensate()`
4. Temporal automatically executes compensation handlers (in reverse order):
   - `compensationActivities.compensatePayment("123")` [Activity executes]
   - Activity publishes `COMPENSATE_PAYMENT` to payment-events topic
   - Payment Service processes refund
5. Workflow completes with state COMPENSATED

**Key Difference from Before:**
- ✅ Compensations are **real Activities** that publish to Kafka
- ✅ Payment Service actually receives COMPENSATE_PAYMENT event
- ✅ Proper retry policies if compensation fails
- ✅ Not just placeholder logs!

## Key Temporal Benefits in This Project

### 1. **Durability**
- If the Saga Orchestrator service crashes, the workflow state is preserved
- When the service restarts, workflows resume from where they left off
- No need to manually track workflow state in a database

### 2. **Reliability**
- Temporal handles retries automatically (configured in `WorkflowOptions`)
- Workflows are guaranteed to execute at least once
- Signals are reliably delivered

### 3. **State Management**
- Workflow state (promises, saga compensations) is managed by Temporal
- No need for complex state machines or database tracking
- State survives service restarts

### 4. **Compensation Handling**
- Temporal's `Saga` class provides automatic compensation execution
- Compensations run in reverse order automatically
- Ensures data consistency across services

## Kafka Topics Used

- **`order-events`**: Central event bus for order-related events
  - Events: `ORDER_CREATED`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `INVENTORY_RESERVED`, `INVENTORY_FAILED`, `SHIPPING_COMPLETED`, `SHIPPING_FAILED`
  
- **`payment-events`**: Payment service events
  - Events: `PROCESS_PAYMENT`
  
- **`inventory-events`**: Inventory service events
  - Events: `RESERVE_INVENTORY`
  
- **`shipping-events`**: Shipping service events
  - Events: `PROCESS_SHIPPING`

## Running the Project

### Prerequisites
- JDK 21
- Maven 3.9+
- Docker & Docker Compose

### Steps

1. **Start Infrastructure**:
   ```bash
   docker-compose up -d  # Starts Kafka and Zookeeper
   # Note: Temporal server needs to be started separately
   # See Temporal documentation for setup
   ```

2. **Start Temporal Server**:
   ```bash
   # Download Temporal CLI and run:
   temporal server start-dev
   ```

3. **Build Services**:
   ```bash
   mvn clean install
   ```

4. **Start Services** (in separate terminals):
   ```bash
   cd saga-orchestrator && mvn spring-boot:run
   cd order-service && mvn spring-boot:run
   cd payment-service && mvn spring-boot:run
   cd inventory-service && mvn spring-boot:run
   cd shipping-service && mvn spring-boot:run
   ```

5. **Test**:
   ```bash
   curl -X POST http://localhost:8081/orders/123
   ```

## Debugging Temporal Workflows

### Temporal Web UI

Access the Temporal Web UI at `http://localhost:8088` (when running locally) to:
- View running workflows
- See workflow execution history
- Inspect workflow state
- View signals and events

### Key Things to Check

1. **Workflow Started?**: Check Temporal UI for workflow with ID matching your orderId
2. **Signals Received?**: Check workflow history in Temporal UI
3. **Kafka Events?**: Monitor Kafka topics using `kafka-console-consumer`
4. **Service Logs**: Each service logs its actions

## Common Patterns

### Adding a New Step

1. Add a new `CompletablePromise` in `OrderWorkflowImpl`
2. Add signal methods to `OrderWorkflow` interface
3. Implement signal handlers in `OrderWorkflowImpl`
4. Add `Workflow.await()` logic in `placeOrder()` method
5. Add compensation handler if needed
6. Update `SagaEventListener` to handle new event types

### Modifying Compensation Logic

Compensation handlers are in `OrderWorkflowImpl`:
- `compensatePayment()`: Called when payment needs to be refunded
- `compensateInventoryReservation()`: Called when inventory needs to be released

Currently, these are placeholders. In production, they should:
- Call the respective service APIs
- Publish compensation events to Kafka
- Handle compensation failures

## Temporal Best Practices Applied

### 1. **Activities for Side Effects** ✅

**Problem**: Workflow code is replayed on every event. If you publish to Kafka directly in workflow code, you'll send duplicate messages.

**Solution**: All Kafka publishing is in Activities.
```java
// ❌ BAD: Direct Kafka call in workflow
kafkaTemplate.send("payment-events", event);

// ✅ GOOD: Use Activity
activities.publishPaymentRequest(orderId);
```

### 2. **Signals for External Events** ✅

**Problem**: How do external services notify the workflow?

**Solution**: Use Signals - they're durable and won't be lost.
```java
// External service publishes to Kafka
// EventListener receives and signals workflow
workflow.onPaymentCompleted();  // Signal is durable
```

### 3. **Timeouts Everywhere** ✅

**Problem**: Workflows could wait forever if a service never responds.

**Solution**: Add timeouts to all waits.
```java
// ❌ BAD: Wait forever
Workflow.await(() -> paymentCompleted.isCompleted());

// ✅ GOOD: Wait with timeout
Workflow.await(Duration.ofMinutes(5), 
    () -> paymentCompleted.isCompleted());
```

### 4. **Real Compensations** ✅

**Problem**: Placeholder compensations don't actually fix failures.

**Solution**: Compensations use Activities to publish actual compensation events.
```java
// ❌ BAD: Just logging
saga.addCompensation(() -> System.out.println("Compensating..."));

// ✅ GOOD: Real compensation via Activity
saga.addCompensation(() -> compensationActivities.compensatePayment(orderId));
```

### 5. **Query Methods for Observability** ✅

**Problem**: How do you check workflow status without affecting execution?

**Solution**: Use Query methods.
```java
@QueryMethod
OrderWorkflowState getState();

// External code can query without side effects
OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, orderId);
OrderWorkflowState state = workflow.getState();
```

### 6. **Structured State Tracking** ✅

**Problem**: No clean way to track workflow progress.

**Solution**: Use a proper state model.
```java
private final OrderWorkflowState state = new OrderWorkflowState();
state.setCurrentStep(WorkflowStep.PAYMENT);
state.addCompletedStep(WorkflowStep.PAYMENT);
```

### 7. **Proper Error Handling** ✅

**Problem**: Generic exceptions make debugging hard.

**Solution**: Custom exception types with context.
```java
throw new PaymentFailedException(orderId, "Payment timeout");
```

### 8. **Separate Retry Policies** ✅

**Problem**: Compensations are more critical than regular operations.

**Solution**: Different Activity stubs with different retry policies.
```java
// Regular activities: 3 retries
private final OrderActivities activities = ...getDefaultActivityOptions();

// Compensations: 5 retries (more critical!)
private final OrderActivities compensationActivities = ...getCompensationActivityOptions();
```

### 9. **Graceful Degradation** ✅

**Problem**: Duplicate events or missing workflows cause errors.

**Solution**: Handle edge cases gracefully.
```java
catch (WorkflowExecutionAlreadyStarted e) {
    logger.warn("Duplicate ORDER_CREATED event");
    // Don't fail - this is expected
}
catch (WorkflowNotFoundException e) {
    logger.warn("Workflow already completed");
    // Don't fail - this is fine
}
```

### 10. **Production-Ready Logging** ✅

**Problem**: System.out.println doesn't work well in production.

**Solution**: Use SLF4J with structured logging.
```java
// ❌ BAD
System.out.println("Processing order: " + orderId);

// ✅ GOOD
logger.info("Processing payment for order: {}", orderId);
```

## Implementation Best Practices

1. **Workflow IDs**: Always use stable, unique identifiers (orderId in this case)
2. **Signal Methods**: Keep them idempotent - calling multiple times should be safe
3. **Compensation**: Always register compensations after successful operations
4. **Error Handling**: Use `Workflow.await()` with timeout and failure conditions
5. **Activity Registration**: Always register activities in TemporalConfig
6. **Testing**: Use Temporal's test framework for unit testing workflows
7. **Workflow Versioning**: Consider versioning strategy for production deployments

## Further Reading

- [Temporal Documentation](https://docs.temporal.io/)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [Temporal Java SDK](https://docs.temporal.io/dev-guide/java)

