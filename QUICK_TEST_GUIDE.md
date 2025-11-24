# 🚀 Quick Test Guide: Temporal Event-Driven Saga Pattern

This guide provides practical, step-by-step instructions to test the Saga Pattern implementation with both success and failure scenarios.

## 📋 Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Testing Success Scenarios](#testing-success-scenarios)
- [Testing Failure Scenarios](#testing-failure-scenarios)
- [Using the Test Script](#using-the-test-script)
- [Monitoring Tools](#monitoring-tools)
- [Test Matrix](#test-matrix)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before testing, ensure you have:
- ✅ Docker and Docker Compose installed
- ✅ curl or Postman for API testing
- ✅ Terminal access
- ✅ Web browser for Temporal UI

---

## Quick Start

### Step 1: Start All Services

```bash
# Navigate to project root
cd /path/to/temporal-event-driven-saga

# Start all services
docker-compose up -d --build
```

This will start:
- **PostgreSQL** (port 5432)
- **Zookeeper** (port 2181)
- **Kafka** (port 9092)
- **Temporal** (port 7233)
- **Temporal UI** (port 8233)
- **Order Service** (port 8081)
- **Payment Service** (port 8082)
- **Inventory Service** (port 8083)
- **Shipping Service** (port 8084)
- **Saga Orchestrator** (port 8085)

### Step 2: Verify Services

```bash
# Check all containers are running
docker-compose ps

# All services should show "Up" status
```

### Step 3: Wait for Services to be Ready

```bash
# Wait approximately 30-60 seconds for all health checks
# Monitor logs to confirm readiness
docker-compose logs -f
```

**Look for these messages:**
- `order-service` - "Started OrderServiceApplication"
- `payment-service` - "Started PaymentServiceApplication"
- `inventory-service` - "Started InventoryServiceApplication"
- `shipping-service` - "Started ShippingServiceApplication"
- `saga-orchestrator` - "Started SagaOrchestratorApplication"

---

## ✅ Testing Success Scenarios

### Test 1: Complete Success Flow (Happy Path)

**Objective:** All steps succeed, order completes successfully.

**Prerequisites:**
- All services configured for success (default configuration)

**Steps:**

1. **Send Order Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-SUCCESS-001
   ```

2. **Expected Response:**
   ```
   Order Creation Request is Initiated!
   ```

3. **Monitor Saga Orchestrator Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   ```

4. **Expected Log Output:**
   ```
   ✅ "Received ORDER_CREATED event for order: ORDER-SUCCESS-001"
   ✅ "onPaymentCompleted is called"
   ✅ "onInventoryReserved is called"
   ✅ "onShippingCompleted is called"
   ✅ "<<<< Order Confirmed and Shipped! >>>>"
   ```

5. **Verify in Temporal UI:**
   - Open: http://localhost:8233
   - Search for workflow ID: `ORDER-SUCCESS-001`
   - Status: **Completed** ✅
   - All workflow steps should show green checkmarks

**Flow Diagram:**
```
ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED 
→ RESERVE_INVENTORY → INVENTORY_RESERVED 
→ PROCESS_SHIPPING → SHIPPING_COMPLETED
→ Order Complete ✅
```

---

## ❌ Testing Failure Scenarios

### Test 2: Payment Failure (No Compensation Needed)

**Objective:** Payment fails at the first step, no compensation required.

**Configuration Required:**
- Payment Service: `payment.simulation.success=false` ❌

**Steps:**

1. **Update Payment Service Configuration:**
   ```bash
   # Edit payment-service/src/main/resources/application.properties
   # Change: payment.simulation.success=true
   # To: payment.simulation.success=false
   ```

   Or use sed:
   ```bash
   sed -i '' 's/payment.simulation.success=true/payment.simulation.success=false/' \
     payment-service/src/main/resources/application.properties
   ```

2. **Rebuild and Restart Payment Service:**
   ```bash
   docker-compose up -d --build payment-service
   
   # Wait for service to be ready (~10 seconds)
   sleep 10
   ```

3. **Send Order Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-PAYMENT-FAIL-001
   ```

4. **Monitor Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   ```

5. **Expected Log Output:**
   ```
   ✅ "Received ORDER_CREATED event"
   ❌ "onPaymentFailed is called"
   📝 "Payment failed for order: ORDER-PAYMENT-FAIL-001, no compensation needed"
   ```

6. **Verify in Temporal UI:**
   - Workflow ID: `ORDER-PAYMENT-FAIL-001`
   - Status: **Failed** or **Completed with errors**
   - No compensation steps executed

**Flow Diagram:**
```
ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_FAILED ❌
→ Workflow exits (no compensation needed)
```

**Restore Configuration:**
```bash
# Change back to: payment.simulation.success=true
docker-compose up -d --build payment-service
```

---

### Test 3: Inventory Failure (Partial Compensation)

**Objective:** Inventory fails after successful payment, triggering payment compensation.

**Configuration Required:**
- Payment Service: `payment.simulation.success=true` ✅
- Inventory Service: `inventory.simulation.success=false` ❌

**Steps:**

1. **Ensure Payment Service is Set to Success:**
   ```bash
   # payment.simulation.success=true
   docker-compose up -d --build payment-service
   ```

2. **Update Inventory Service Configuration:**
   ```bash
   # Edit inventory-service/src/main/resources/application.properties
   # Change: inventory.simulation.success=true
   # To: inventory.simulation.success=false
   
   sed -i '' 's/inventory.simulation.success=true/inventory.simulation.success=false/' \
     inventory-service/src/main/resources/application.properties
   ```

3. **Rebuild and Restart Inventory Service:**
   ```bash
   docker-compose up -d --build inventory-service
   sleep 10
   ```

4. **Send Order Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-INVENTORY-FAIL-001
   ```

5. **Monitor Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   ```

6. **Expected Log Output:**
   ```
   ✅ "onPaymentCompleted is called"
   ❌ "onInventoryFailed is called"
   📝 "Inventory reservation failed for order: ORDER-INVENTORY-FAIL-001"
   🔄 "Compensating payment only"
   🔄 "<<<< Compensating Payment >>>>"
   ```

7. **Verify in Temporal UI:**
   - Workflow ID: `ORDER-INVENTORY-FAIL-001`
   - Payment step: **Completed** ✅
   - Inventory step: **Failed** ❌
   - Compensation: **Payment Refund Executed** 🔄

**Flow Diagram:**
```
ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED ✅
→ RESERVE_INVENTORY → INVENTORY_FAILED ❌
→ Compensate Payment 🔄
→ Workflow completes with compensation
```

**Restore Configuration:**
```bash
# Change back to: inventory.simulation.success=true
docker-compose up -d --build inventory-service
```

---

### Test 4: Shipping Failure (Full Compensation)

**Objective:** Shipping fails after payment and inventory succeed, triggering full compensation chain.

**Configuration Required:**
- Payment Service: `payment.simulation.success=true` ✅
- Inventory Service: `inventory.simulation.success=true` ✅
- Shipping Service: `shipping.simulation.success=false` ❌ (default)

**Steps:**

1. **Ensure Payment and Inventory are Set to Success:**
   ```bash
   # Both should be set to true
   docker-compose up -d --build payment-service inventory-service
   ```

2. **Update Shipping Service Configuration:**
   ```bash
   # Edit shipping-service/src/main/resources/application.properties
   # Ensure: shipping.simulation.success=false (this is default)
   
   sed -i '' 's/shipping.simulation.success=true/shipping.simulation.success=false/' \
     shipping-service/src/main/resources/application.properties
   ```

3. **Rebuild and Restart Shipping Service:**
   ```bash
   docker-compose up -d --build shipping-service
   sleep 10
   ```

4. **Send Order Request:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-SHIPPING-FAIL-001
   ```

5. **Monitor Logs:**
   ```bash
   docker-compose logs -f saga-orchestrator
   ```

6. **Expected Log Output:**
   ```
   ✅ "onPaymentCompleted is called"
   ✅ "onInventoryReserved is called"
   ❌ "onShippingFailed is called"
   📝 "Shipping failed for order: ORDER-SHIPPING-FAIL-001"
   🔄 "Initiating compensation"
   🔄 "<<<< Compensating Inventory Reservation >>>>"
   🔄 "<<<< Compensating Payment >>>>"
   ```

7. **Verify in Temporal UI:**
   - Workflow ID: `ORDER-SHIPPING-FAIL-001`
   - Payment step: **Completed** ✅
   - Inventory step: **Completed** ✅
   - Shipping step: **Failed** ❌
   - Compensation: **Both Inventory Release and Payment Refund Executed** 🔄

**Flow Diagram:**
```
ORDER_CREATED → PROCESS_PAYMENT → PAYMENT_COMPLETED ✅
→ RESERVE_INVENTORY → INVENTORY_RESERVED ✅
→ PROCESS_SHIPPING → SHIPPING_FAILED ❌
→ Compensate Inventory 🔄
→ Compensate Payment 🔄
→ Workflow completes with full compensation
```

**This is the full Saga pattern in action!** ✨

---

## 🤖 Using the Test Script

### Make Script Executable

```bash
chmod +x test-saga.sh
```

### Run Interactive Test Script

```bash
./test-saga.sh
```

### Test Script Features

The script provides an interactive menu with these options:

**Test Scenarios:**
1. Complete Success Flow
2. Payment Failure (No Compensation)
3. Inventory Failure (Partial Compensation)
4. Shipping Failure (Full Compensation)
5. Run All Tests Sequentially

**Utilities:**
6. Check Services Status
7. Monitor Saga Orchestrator Logs
8. View Kafka Events (order-events)
9. View Kafka Events (payment-events)
10. Open Temporal UI
11. Restore All Configurations

**Benefits:**
- ✅ Automated configuration updates
- ✅ Automatic service rebuilding
- ✅ Built-in log monitoring
- ✅ Configuration backup and restore
- ✅ Color-coded output for easy reading

### Run Specific Test from Command Line

```bash
# Example: Run payment failure test
./test-saga.sh <<EOF
2
EOF
```

---

## 🔍 Monitoring Tools

### 1. Docker Compose Logs

**View all logs:**
```bash
docker-compose logs -f
```

**View specific service:**
```bash
docker-compose logs -f saga-orchestrator
docker-compose logs -f payment-service
docker-compose logs -f inventory-service
docker-compose logs -f shipping-service
```

**Search logs:**
```bash
docker-compose logs saga-orchestrator | grep -i "compensation"
docker-compose logs saga-orchestrator | grep -i "failed"
```

### 2. Temporal UI

**Access:** http://localhost:8233

**Features:**
- 📊 View all workflows
- 🔍 Search by workflow ID (order ID)
- 📜 See complete execution history
- 🎯 Track signals and events
- ✅ Monitor workflow status
- 🔄 View compensation flows

**How to Use:**
1. Open http://localhost:8233
2. Click **Workflows** in the left sidebar
3. Search for your order ID (e.g., `ORDER-SUCCESS-001`)
4. Click on the workflow to see detailed history
5. Expand each event to see details
6. Check for compensation activities

### 3. Kafka Event Monitoring

**List all topics:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-topics --list --bootstrap-server localhost:9092
```

**View order-events topic:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

**View payment-events topic:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning
```

**View inventory-events topic:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic inventory-events \
  --from-beginning
```

**View shipping-events topic:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic shipping-events \
  --from-beginning
```

---

## 📊 Test Matrix

| Test Scenario | Payment | Inventory | Shipping | Expected Result | Compensation Actions |
|--------------|---------|-----------|----------|-----------------|---------------------|
| **Complete Success** | ✅ Success | ✅ Success | ✅ Success | Order Completed | None |
| **Payment Failure** | ❌ Failed | - Skipped | - Skipped | Order Failed | None (early exit) |
| **Inventory Failure** | ✅ Success | ❌ Failed | - Skipped | Order Failed | Refund Payment |
| **Shipping Failure** | ✅ Success | ✅ Success | ❌ Failed | Order Failed | Release Inventory + Refund Payment |

### Event Flow Summary

```
┌─────────────────────────────────────────────────────────────┐
│                     SUCCESS PATH                             │
├─────────────────────────────────────────────────────────────┤
│ ORDER_CREATED → PAYMENT_COMPLETED → INVENTORY_RESERVED      │
│ → SHIPPING_COMPLETED → ORDER COMPLETE ✅                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  PAYMENT FAILURE PATH                        │
├─────────────────────────────────────────────────────────────┤
│ ORDER_CREATED → PAYMENT_FAILED ❌                            │
│ → No compensation needed                                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                INVENTORY FAILURE PATH                        │
├─────────────────────────────────────────────────────────────┤
│ ORDER_CREATED → PAYMENT_COMPLETED → INVENTORY_FAILED ❌     │
│ → COMPENSATE_PAYMENT 🔄                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 SHIPPING FAILURE PATH                        │
├─────────────────────────────────────────────────────────────┤
│ ORDER_CREATED → PAYMENT_COMPLETED → INVENTORY_RESERVED      │
│ → SHIPPING_FAILED ❌                                          │
│ → COMPENSATE_INVENTORY 🔄 → COMPENSATE_PAYMENT 🔄           │
└─────────────────────────────────────────────────────────────┘
```

---

## 📮 Using Postman Collection

### Import Collection

1. Open Postman
2. Click **Import** button
3. Select `POSTMAN_COLLECTION.json` from project root
4. Collection will be imported with all test scenarios

### Pre-configured Requests

**Order Service:**
- Create Order - Success Scenario (Basic)
- Create Order - With Customer ID
- Create Order - Premium Customer
- Create Order - Multiple Items Test
- Create Order - High Volume Test

**Testing Scenarios:**
- Scenario 1: Complete Success
- Scenario 2: Payment Failure
- Scenario 3: Inventory Failure with Compensation
- Scenario 4: Shipping Failure with Full Compensation

**Health Checks:**
- Check Order Service
- Temporal UI

### Variables

Configure these in Postman environment or collection:
- `base_url` = `http://localhost:8081`
- `order_id` = `ORDER-001`
- `customer_id` = `CUST-12345`

---

## 🛠️ Troubleshooting

### Issue: Services Won't Start

**Symptoms:**
- Containers keep restarting
- Services show as "Unhealthy"

**Solutions:**
```bash
# Check Docker logs
docker-compose logs

# Restart all services
docker-compose down -v
docker-compose up -d --build

# Check individual service
docker-compose logs -f <service-name>
```

### Issue: Kafka Connection Errors

**Symptoms:**
- "Connection refused" errors
- "Broker not available"

**Solutions:**
```bash
# Verify Kafka is healthy
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka and dependent services
docker-compose restart kafka
docker-compose restart order-service payment-service inventory-service shipping-service saga-orchestrator
```

### Issue: Temporal Connection Errors

**Symptoms:**
- "Failed to connect to Temporal"
- Workflows not starting

**Solutions:**
```bash
# Verify Temporal is running
docker-compose ps temporal

# Check Temporal logs
docker-compose logs temporal

# Access Temporal UI
open http://localhost:8233

# Restart Temporal and saga-orchestrator
docker-compose restart temporal saga-orchestrator
```

### Issue: Workflow Not Starting

**Check:**
1. Saga Orchestrator logs:
   ```bash
   docker-compose logs saga-orchestrator
   ```

2. Verify `ORDER_CREATED` event is published:
   ```bash
   docker exec -it temporal-event-driven-saga-kafka-1 \
     kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic order-events \
     --from-beginning
   ```

3. Check if workflow exists in Temporal UI

### Issue: Events Not Flowing

**Check:**
1. Service logs for Kafka consumer errors
2. Kafka topic consumers
3. Network connectivity - ensure all services are on `saga-network`

**Verify network:**
```bash
docker network inspect temporal-event-driven-saga_saga-network
```

### Issue: Compensation Not Triggering

**Check:**
1. Workflow execution in Temporal UI
2. Saga Orchestrator logs for compensation messages:
   ```bash
   docker-compose logs saga-orchestrator | grep -i "compensat"
   ```
3. Verify failure events are being published

### Issue: Port Already in Use

**Symptoms:**
- "Port is already allocated" error

**Solutions:**
```bash
# Check what's using the port
lsof -i :8081
lsof -i :9092

# Stop conflicting service or change port in docker-compose.yaml
```

### Complete Reset

If all else fails, perform a complete reset:

```bash
# Stop and remove all containers, volumes, and networks
docker-compose down -v

# Remove all images (optional)
docker-compose down --rmi all -v

# Rebuild from scratch
docker-compose up -d --build

# Wait for all services to be healthy
docker-compose ps
```

---

## 🎯 Quick Reference Commands

### Starting/Stopping

```bash
# Start all services
docker-compose up -d --build

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Restart specific service
docker-compose restart <service-name>

# Rebuild specific service
docker-compose up -d --build <service-name>
```

### Monitoring

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f saga-orchestrator

# Check service status
docker-compose ps

# Follow logs with grep filter
docker-compose logs -f saga-orchestrator | grep -i "order"
```

### Testing

```bash
# Send order request
curl -X POST http://localhost:8081/orders/ORDER-001

# Send order with customer ID
curl -X POST "http://localhost:8081/orders/ORDER-002?customerId=CUST-12345"

# Run test script
./test-saga.sh
```

### Kafka

```bash
# List topics
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-topics --list --bootstrap-server localhost:9092

# View events
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

---

## 📚 Additional Resources

- **Full Testing Guide:** See `TESTING_GUIDE.md` for comprehensive documentation
- **Developer Guide:** See `DEVELOPER_GUIDE.md` for architecture details
- **Postman Collection:** `POSTMAN_COLLECTION.json` for API testing
- **Temporal UI:** http://localhost:8233 for workflow monitoring

---

## ✅ Summary

This Saga Pattern implementation demonstrates:

1. **Distributed Transaction Management** - Coordinating multiple microservices
2. **Event-Driven Architecture** - Using Kafka for asynchronous communication
3. **Workflow Orchestration** - Using Temporal for durable workflow execution
4. **Compensation Handling** - Automatic rollback on failures in reverse order
5. **Resilience** - Workflows survive service restarts and failures

**Key Benefits:**
- ✅ Durable execution (survives crashes)
- ✅ Automatic compensation on failures
- ✅ Real-time monitoring via Temporal UI
- ✅ Event-driven, scalable architecture
- ✅ Clear separation of concerns
- ✅ Easy to test and debug

**Happy Testing!** 🚀

If you encounter any issues or have questions, refer to the troubleshooting section or check the logs for detailed error messages.

