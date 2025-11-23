# Testing Guide: Temporal Event-Driven Saga Pattern

This guide provides comprehensive steps to test the Saga Pattern implementation using Temporal workflows.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Starting the Project](#starting-the-project)
3. [Postman Setup](#postman-setup)
4. [Testing Scenarios](#testing-scenarios)
5. [Monitoring the Saga Workflow](#monitoring-the-saga-workflow)
6. [Understanding the Flow](#understanding-the-flow)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Docker and Docker Compose installed
- Postman (or any REST client)
- Access to Docker logs
- Web browser for Temporal UI

---

## Starting the Project

### Step 1: Start All Services

```bash
# Navigate to project root
cd /path/to/temporal-event-driven-saga

# Start all services using Docker Compose
docker-compose up -d --build
```

This will start:
- **Zookeeper** (port 2181)
- **Kafka** (port 9092)
- **Temporal** (ports 7233, 8088)
- **Order Service** (port 8081)
- **Payment Service** (port 8082)
- **Inventory Service** (port 8083)
- **Shipping Service** (port 8084)
- **Saga Orchestrator** (port 8085)

### Step 2: Verify Services are Running

```bash
# Check all containers are running
docker-compose ps

# Check logs for any errors
docker-compose logs -f
```

Expected output: All services should show as "Up" and healthy.

### Step 3: Access Temporal Web UI

Open your browser and navigate to:
```
http://localhost:8088
```

This is the Temporal Web UI where you can monitor workflows in real-time.

---

## Postman Setup

### Option 1: Import Collection

1. Open Postman
2. Click **Import** button
3. Select the file `POSTMAN_COLLECTION.json` from the project root
4. The collection will be imported with pre-configured requests

### Option 2: Manual Setup

**Request Details:**
- **Method:** `POST`
- **URL:** `http://localhost:8081/orders/{orderId}`
- **Headers:** None required
- **Body:** None required (orderId is in the path)

**Example URLs:**
- `http://localhost:8081/orders/ORDER-001`
- `http://localhost:8081/orders/ORDER-002`
- `http://localhost:8081/orders/ORDER-003`

**Expected Response:**
```
Order Creation Request is Initiated!
```

---

## Testing Scenarios

### Scenario 1: Complete Success Flow (Happy Path)

**Objective:** Test the complete Saga workflow where all steps succeed.

**Configuration:**
- Payment Service: `payment.simulation.success=true` ✅
- Inventory Service: `inventory.simulation.success=true` ✅
- Shipping Service: `shipping.simulation.success=true` (needs to be changed)

**Steps:**

1. **Update Shipping Service Configuration:**
   ```bash
   # Edit shipping-service/src/main/resources/application.properties
   # Change: shipping.simulation.success=false
   # To: shipping.simulation.success=true
   
   # Rebuild and restart
   docker-compose up -d --build shipping-service
   ```

2. **Send Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-001
   ```
   Or use Postman with URL: `http://localhost:8081/orders/ORDER-001`

3. **Expected Flow:**
   ```
   ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED 
   → RESERVE_INVENTORY → INVENTORY_RESERVED 
   → PROCESS_SHIPPING → SHIPPING_COMPLETED
   → Order Confirmed and Shipped!
   ```

4. **Verify in Logs:**
   ```bash
   # Check Saga Orchestrator logs
   docker-compose logs -f saga-orchestrator
   
   # Look for:
   # - "onPaymentCompleted is called"
   # - "onInventoryReserved is called"
   # - "onShippingCompleted is called"
   # - "<<<< Order Confirmed and Shipped! >>>>"
   ```

5. **Verify in Temporal UI:**
   - Go to `http://localhost:8088`
   - Search for workflow ID: `ORDER-001`
   - Check workflow history shows all steps completed
   - Verify workflow status is "Completed"

---

### Scenario 2: Payment Failure (Early Exit)

**Objective:** Test Saga compensation when payment fails early.

**Configuration:**
- Payment Service: `payment.simulation.success=false` ❌
- No compensation needed (payment failed before any operations completed)

**Steps:**

1. **Update Payment Service Configuration:**
   ```bash
   # Edit payment-service/src/main/resources/application.properties
   # Change: payment.simulation.success=true
   # To: payment.simulation.success=false
   
   # Rebuild and restart
   docker-compose up -d --build payment-service
   ```

2. **Send Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-002
   ```

3. **Expected Flow:**
   ```
   ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_FAILED
   → Workflow exits (no compensation needed)
   ```

4. **Verify in Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   
   # Look for:
   # - "onPaymentFailed is called"
   # - "Payment failed for order: ORDER-002, no compensation needed"
   ```

5. **Verify in Temporal UI:**
   - Search for workflow ID: `ORDER-002`
   - Check workflow shows payment failed
   - Verify no compensation was triggered

---

### Scenario 3: Inventory Failure (Compensation Triggered)

**Objective:** Test Saga compensation when inventory reservation fails after successful payment.

**Configuration:**
- Payment Service: `payment.simulation.success=true` ✅
- Inventory Service: `inventory.simulation.success=false` ❌
- **Compensation Expected:** Payment refund

**Steps:**

1. **Update Configurations:**
   ```bash
   # Set payment to succeed
   # payment.simulation.success=true (already set)
   
   # Set inventory to fail
   # Edit inventory-service/src/main/resources/application.properties
   # Change: inventory.simulation.success=true
   # To: inventory.simulation.success=false
   
   # Rebuild and restart
   docker-compose up -d --build inventory-service payment-service
   ```

2. **Send Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-003
   ```

3. **Expected Flow:**
   ```
   ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED
   → RESERVE_INVENTORY → INVENTORY_FAILED
   → <<<< Compensating Payment >>>>
   → Workflow completes with compensation
   ```

4. **Verify in Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   
   # Look for:
   # - "onPaymentCompleted is called"
   # - "onInventoryFailed is called"
   # - "Inventory reservation failed for order: ORDER-003, compensating payment only"
   # - "<<<< Compensating Payment >>>>"
   ```

5. **Verify in Temporal UI:**
   - Search for workflow ID: `ORDER-003`
   - Check workflow history shows:
     - Payment completed ✅
     - Inventory failed ❌
     - Compensation executed (payment refund)

---

### Scenario 4: Shipping Failure (Full Compensation)

**Objective:** Test Saga compensation when shipping fails after successful payment and inventory.

**Configuration:**
- Payment Service: `payment.simulation.success=true` ✅
- Inventory Service: `inventory.simulation.success=true` ✅
- Shipping Service: `shipping.simulation.success=false` ❌ (default)
- **Compensation Expected:** Payment refund + Inventory release

**Steps:**

1. **Ensure Configurations:**
   ```bash
   # Payment: success=true
   # Inventory: success=true
   # Shipping: success=false (default)
   ```

2. **Send Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-004
   ```

3. **Expected Flow:**
   ```
   ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED
   → RESERVE_INVENTORY → INVENTORY_RESERVED
   → PROCESS_SHIPPING → SHIPPING_FAILED
   → <<<< Compensating Inventory Reservation >>>>
   → <<<< Compensating Payment >>>>
   → Workflow completes with full compensation
   ```

4. **Verify in Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   
   # Look for:
   # - "onPaymentCompleted is called"
   # - "onInventoryReserved is called"
   # - "onShippingFailed is called"
   # - "Shipping failed for order: ORDER-004, initiating compensation"
   # - "<<<< Compensating Inventory Reservation >>>>"
   # - "<<<< Compensating Payment >>>>"
   ```

5. **Verify in Temporal UI:**
   - Search for workflow ID: `ORDER-004`
   - Check workflow history shows:
     - Payment completed ✅
     - Inventory reserved ✅
     - Shipping failed ❌
     - Both compensations executed (in reverse order)

---

## Monitoring the Saga Workflow

### 1. Service Logs

Monitor individual service logs:

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f saga-orchestrator
docker-compose logs -f order-service
docker-compose logs -f payment-service
docker-compose logs -f inventory-service
docker-compose logs -f shipping-service
```

### 2. Temporal Web UI

**Access:** `http://localhost:8088`

**Features:**
- View all workflows
- See workflow execution history
- Monitor workflow state
- View signals and events
- Check workflow status (Running, Completed, Failed)

**How to Use:**
1. Open `http://localhost:8088`
2. Click on **Workflows** in the left sidebar
3. Search for your order ID (e.g., `ORDER-001`)
4. Click on the workflow to see detailed history
5. Expand each event to see details

### 3. Kafka Topics Monitoring

Monitor Kafka events:

```bash
# List all topics
docker exec -it temporal-event-driven-saga-kafka-1 kafka-topics --list --bootstrap-server localhost:9092

# Consume order-events topic
docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning

# Consume payment-events topic
docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning

# Consume inventory-events topic
docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic inventory-events \
  --from-beginning

# Consume shipping-events topic
docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic shipping-events \
  --from-beginning
```

---

## Understanding the Flow

### Complete Saga Flow Diagram

```
┌─────────────────┐
│  POST /orders/  │
│   {orderId}     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Order Service  │ Publishes ORDER_CREATED
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Kafka Topic:    │
│  order-events   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Saga            │ Starts Temporal Workflow
│ Orchestrator     │─────────────────────────┐
└────────┬────────┘                         │
         │                                   │
         ▼                                   │
┌─────────────────┐                         │
│ Publishes       │                         │
│ PROCESS_PAYMENT │                         │
└────────┬────────┘                         │
         │                                   │
         ▼                                   │
┌─────────────────┐                         │
│ Payment Service │                         │
│ Processes       │                         │
│ Payment         │                         │
└────────┬────────┘                         │
         │                                   │
    ┌────┴────┐                              │
    │         │                              │
    ▼         ▼                              │
SUCCESS    FAILURE                           │
    │         │                              │
    │         └──► PAYMENT_FAILED           │
    │              Signal Workflow           │
    │              Workflow Exits           │
    │                                        │
    ▼                                        │
PAYMENT_COMPLETED                            │
    │                                        │
    ▼                                        │
┌─────────────────┐                         │
│ Publishes       │                         │
│ RESERVE_INVENTORY│                        │
└────────┬────────┘                         │
         │                                   │
         ▼                                   │
┌─────────────────┐                         │
│ Inventory       │                         │
│ Service         │                         │
│ Reserves        │                         │
│ Inventory       │                         │
└────────┬────────┘                         │
         │                                   │
    ┌────┴────┐                              │
    │         │                              │
    ▼         ▼                              │
SUCCESS    FAILURE                           │
    │         │                              │
    │         └──► INVENTORY_FAILED         │
    │              Signal Workflow           │
    │              Compensate Payment        │
    │              Workflow Exits            │
    │                                        │
    ▼                                        │
INVENTORY_RESERVED                           │
    │                                        │
    ▼                                        │
┌─────────────────┐                         │
│ Publishes       │                         │
│ PROCESS_SHIPPING│                         │
└────────┬────────┘                         │
         │                                   │
         ▼                                   │
┌─────────────────┐                         │
│ Shipping        │                         │
│ Service         │                         │
│ Processes       │                         │
│ Shipping        │                         │
└────────┬────────┘                         │
         │                                   │
    ┌────┴────┐                              │
    │         │                              │
    ▼         ▼                              │
SUCCESS    FAILURE                           │
    │         │                              │
    │         └──► SHIPPING_FAILED          │
    │              Signal Workflow            │
    │              Compensate Inventory       │
    │              Compensate Payment         │
    │              Workflow Exits            │
    │                                        │
    ▼                                        │
SHIPPING_COMPLETED                           │
    │                                        │
    ▼                                        │
┌─────────────────┐                         │
│ Temporal        │                         │
│ Workflow        │                         │
│ Completes       │                         │
│ Successfully    │                         │
└─────────────────┘                         │
```

### Event Types

**Order Events Topic (`order-events`):**
- `ORDER_CREATED` - Initial order creation
- `PAYMENT_COMPLETED` - Payment processed successfully
- `PAYMENT_FAILED` - Payment processing failed
- `INVENTORY_RESERVED` - Inventory reserved successfully
- `INVENTORY_FAILED` - Inventory reservation failed
- `SHIPPING_COMPLETED` - Shipping arranged successfully
- `SHIPPING_FAILED` - Shipping arrangement failed

**Service-Specific Topics:**
- `payment-events`: `PROCESS_PAYMENT`
- `inventory-events`: `RESERVE_INVENTORY`
- `shipping-events`: `PROCESS_SHIPPING`

### Compensation Order

When compensation is triggered, it executes in **reverse order** of operations:

1. **Shipping fails** → Compensate Inventory → Compensate Payment
2. **Inventory fails** → Compensate Payment
3. **Payment fails** → No compensation needed

---

## Troubleshooting

### Issue: Services Not Starting

**Solution:**
```bash
# Check logs
docker-compose logs

# Restart specific service
docker-compose restart <service-name>

# Rebuild and restart
docker-compose up -d --build <service-name>
```

### Issue: Kafka Connection Errors

**Solution:**
```bash
# Verify Kafka is healthy
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Issue: Temporal Connection Errors

**Solution:**
```bash
# Verify Temporal is running
docker-compose ps temporal

# Check Temporal logs
docker-compose logs temporal

# Access Temporal UI
# Open http://localhost:8088
```

### Issue: Workflow Not Starting

**Check:**
1. Saga Orchestrator logs: `docker-compose logs saga-orchestrator`
2. Kafka topics: Verify `ORDER_CREATED` event is published
3. Temporal UI: Check if workflow exists

### Issue: Events Not Flowing

**Check:**
1. Service logs for Kafka consumer errors
2. Kafka topic consumers: Use `kafka-console-consumer` commands
3. Network connectivity: Ensure all services are on `saga-network`

### Issue: Compensation Not Triggering

**Check:**
1. Workflow logs in Temporal UI
2. Saga Orchestrator logs for compensation messages
3. Verify failure events are being published correctly

---

## Quick Test Commands

### Test Success Flow
```bash
# 1. Ensure all services succeed
# Edit application.properties files accordingly

# 2. Send request
curl -X POST http://localhost:8081/orders/ORDER-TEST-001

# 3. Monitor logs
docker-compose logs -f saga-orchestrator
```

### Test Failure Flow
```bash
# 1. Set shipping to fail (default)
# shipping.simulation.success=false

# 2. Send request
curl -X POST http://localhost:8081/orders/ORDER-TEST-002

# 3. Check compensation in logs
docker-compose logs -f saga-orchestrator | grep -i "compensat"
```

### Monitor All Events
```bash
# Watch all Kafka events
docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

---

## Summary

This Saga Pattern implementation demonstrates:

1. **Distributed Transaction Management** - Coordinating multiple microservices
2. **Event-Driven Architecture** - Using Kafka for asynchronous communication
3. **Workflow Orchestration** - Using Temporal for durable workflow execution
4. **Compensation Handling** - Automatic rollback on failures
5. **Resilience** - Workflows survive service restarts

**Key Benefits:**
- ✅ Durable execution (survives crashes)
- ✅ Automatic compensation on failures
- ✅ Real-time monitoring via Temporal UI
- ✅ Event-driven, scalable architecture
- ✅ Clear separation of concerns

Happy Testing! 🚀

