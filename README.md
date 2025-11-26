# Temporal Event-Driven Saga Pattern Implementation

A production-ready microservices implementation of the Saga pattern using Spring Boot, Apache Kafka, and Temporal Workflow Engine. This project demonstrates distributed transaction management with automatic compensation across multiple services using an event-driven architecture.

## 🎯 Key Features

- ✅ **Orchestration-based Saga Pattern** using Temporal Workflows
- ✅ **Event-Driven Architecture** with Apache Kafka for inter-service communication
- ✅ **Automatic Compensation** in reverse order on failures
- ✅ **Durable Workflow Execution** that survives service restarts
- ✅ **Full Docker Compose Setup** for easy deployment
- ✅ **Comprehensive Testing Suite** with automated test scripts
- ✅ **Real-time Monitoring** via Temporal UI
- ✅ **Shared Common Module** for event models and constants

## 🏗️ Architecture Overview

The project consists of the following microservices:

### Core Services
- **Saga Orchestrator** (Port: 8085): Orchestrates distributed transactions using Temporal workflows
- **Order Service** (Port: 8081): Handles order creation and publishes order events
- **Payment Service** (Port: 8082): Processes payments and publishes payment events
- **Inventory Service** (Port: 8083): Manages inventory reservations and publishes inventory events
- **Shipping Service** (Port: 8084): Handles shipping operations and publishes shipping events

### Shared Module
- **Common Module**: Contains shared event models, constants, and utilities used across all services

### Infrastructure Services
- **Apache Kafka** (Port: 9092): Message broker for event-driven communication
- **Zookeeper** (Port: 2181): Kafka coordination service
- **Temporal Server** (Port: 7233): Workflow orchestration engine
- **Temporal UI** (Port: 8233): Web-based workflow monitoring dashboard
- **PostgreSQL** (Port: 5432): Temporal persistence layer

## 🛠️ Technology Stack

- **Java 21** - Modern Java features and performance
- **Spring Boot 3.4.2** - Enterprise-grade framework
- **Apache Kafka 7.5.1** - Distributed event streaming
- **Temporal 1.24.2** - Durable workflow orchestration
- **PostgreSQL 17** - Temporal database backend
- **Maven 3.9+** - Dependency management and build tool
- **Docker & Docker Compose** - Containerization and orchestration

## 📋 Prerequisites

- **JDK 21** or higher
- **Maven 3.9+** for building
- **Docker** and **Docker Compose** for running services
- **curl** or **Postman** for API testing (optional)
- **Git** for cloning the repository

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

This is the easiest way to run the entire system with all dependencies.

```bash
# Clone the repository
git clone <repository-url>
cd temporal-event-driven-saga

# Start all services (infrastructure + microservices)
docker-compose up -d --build

# Check services status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

**What gets started:**
- PostgreSQL database for Temporal
- Zookeeper for Kafka coordination
- Kafka broker for event streaming
- Temporal server for workflow orchestration
- Temporal UI for monitoring
- All 5 microservices (order, payment, inventory, shipping, saga-orchestrator)

**Service Readiness:**
Wait 30-60 seconds for all services to be fully ready. Check logs for:
- `Started OrderServiceApplication`
- `Started PaymentServiceApplication`
- `Started InventoryServiceApplication`
- `Started ShippingServiceApplication`
- `Started SagaOrchestratorApplication`

### Option 2: Local Development (Manual Setup)

For development purposes, you can run services locally.

#### Step 1: Start Infrastructure Services

```bash
# Start only infrastructure (Kafka, Temporal, PostgreSQL)
docker-compose up -d postgresql zookeeper kafka temporal temporal-ui
```

#### Step 2: Build Common Module

```bash
# Build the shared common module first
cd common
mvn clean install
cd ..
```

#### Step 3: Build All Services

```bash
# Build all services
mvn clean install

# Or build individual services
cd order-service && mvn clean install && cd ..
cd payment-service && mvn clean install && cd ..
cd inventory-service && mvn clean install && cd ..
cd shipping-service && mvn clean install && cd ..
cd saga-orchestrator && mvn clean install && cd ..
```

#### Step 4: Run Services

Start each service in a separate terminal:

```bash
# Terminal 1: Order Service
cd order-service && mvn spring-boot:run

# Terminal 2: Payment Service
cd payment-service && mvn spring-boot:run

# Terminal 3: Inventory Service
cd inventory-service && mvn spring-boot:run

# Terminal 4: Shipping Service
cd shipping-service && mvn spring-boot:run

# Terminal 5: Saga Orchestrator
cd saga-orchestrator && mvn spring-boot:run
```

**Local Configuration:**
Update `application.properties` in each service to use:
```properties
spring.kafka.bootstrap-servers=localhost:9092
```

## 🧪 Testing the System

### Quick Test (Success Scenario)

```bash
# Send an order request
curl -X POST http://localhost:8081/orders/ORDER-001

# Expected response
Order Creation Request is Initiated!

# Monitor saga orchestrator logs
docker-compose logs -f saga-orchestrator

# Open Temporal UI to see workflow
open http://localhost:8233
```

### Using the Automated Test Script

The project includes a comprehensive test script for testing various scenarios:

```bash
# Make script executable
chmod +x test-saga.sh

# Run interactive test menu
./test-saga.sh
```

**Available test scenarios:**
1. ✅ Complete Success Flow (Happy Path)
2. ❌ Payment Failure (No Compensation)
3. ❌ Inventory Failure (Partial Compensation - Refund Payment)
4. ❌ Shipping Failure (Full Compensation - Release Inventory + Refund Payment)

**For detailed testing instructions**, see [QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)

### Using Postman Collection

Import `POSTMAN_COLLECTION.json` into Postman for pre-configured API requests and test scenarios.

## 📡 API Endpoints

### Order Service (Port 8081)

**Create Order:**
```bash
# Basic order creation
POST http://localhost:8081/orders/{orderId}

# Example
curl -X POST http://localhost:8081/orders/ORDER-123

# With customer ID
curl -X POST "http://localhost:8081/orders/ORDER-123?customerId=CUST-456"
```

**Response:**
```
Order Creation Request is Initiated!
```

## 🔄 Event Flow & Saga Pattern

### Success Flow (Happy Path)

```
┌─────────────────────────────────────────────────────────────┐
│                     SUCCESS PATH                             │
├─────────────────────────────────────────────────────────────┤
│ 1. ORDER_CREATED                                             │
│    ↓                                                         │
│ 2. PROCESS_PAYMENT → PAYMENT_COMPLETED ✅                   │
│    ↓                                                         │
│ 3. RESERVE_INVENTORY → INVENTORY_RESERVED ✅                │
│    ↓                                                         │
│ 4. PROCESS_SHIPPING → SHIPPING_COMPLETED ✅                 │
│    ↓                                                         │
│ 5. ORDER COMPLETE ✅                                         │
└─────────────────────────────────────────────────────────────┘
```

### Failure Flow with Compensation

```
┌─────────────────────────────────────────────────────────────┐
│                  SHIPPING FAILURE PATH                       │
│              (Full Compensation Example)                     │
├─────────────────────────────────────────────────────────────┤
│ 1. ORDER_CREATED                                             │
│    ↓                                                         │
│ 2. PROCESS_PAYMENT → PAYMENT_COMPLETED ✅                   │
│    ↓                                                         │
│ 3. RESERVE_INVENTORY → INVENTORY_RESERVED ✅                │
│    ↓                                                         │
│ 4. PROCESS_SHIPPING → SHIPPING_FAILED ❌                    │
│    ↓                                                         │
│ 5. COMPENSATE INVENTORY (Release) 🔄                        │
│    ↓                                                         │
│ 6. COMPENSATE PAYMENT (Refund) 🔄                           │
│    ↓                                                         │
│ 7. ORDER FAILED (Fully Compensated) ✅                      │
└─────────────────────────────────────────────────────────────┘
```

### Event Types by Service

**Order Service → `order-events` topic:**
- `ORDER_CREATED`: Initial order creation event

**Payment Service → `payment-events` topic:**
- `PAYMENT_COMPLETED`: Payment processed successfully
- `PAYMENT_FAILED`: Payment processing failed
- `PAYMENT_REFUNDED`: Payment compensation completed

**Inventory Service → `inventory-events` topic:**
- `INVENTORY_RESERVED`: Inventory successfully reserved
- `INVENTORY_FAILED`: Inventory reservation failed
- `INVENTORY_RELEASED`: Inventory compensation completed

**Shipping Service → `shipping-events` topic:**
- `SHIPPING_COMPLETED`: Shipping arranged successfully
- `SHIPPING_FAILED`: Shipping arrangement failed

## 📂 Project Structure

```
temporal-event-driven-saga/
├── common/                                 # Shared module
│   ├── src/main/java/com/saga/common/
│   │   ├── constants/                      # Event type constants
│   │   │   ├── EventType.java
│   │   │   ├── OrderEventType.java
│   │   │   ├── PaymentEventType.java
│   │   │   ├── InventoryEventType.java
│   │   │   └── ShippingEventType.java
│   │   └── model/                          # Shared event models
│   │       ├── OrderEvent.java
│   │       ├── PaymentEvent.java
│   │       ├── InventoryEvent.java
│   │       └── ShippingEvent.java
│   └── pom.xml
├── saga-orchestrator/                      # Workflow orchestrator
│   ├── src/main/java/com/saga/
│   │   ├── activities/
│   │   │   ├── OrderActivities.java        # Activity interface
│   │   │   └── OrderActivitiesImpl.java    # Activity implementation
│   │   ├── config/
│   │   │   └── WorkflowOptionsConfig.java  # Workflow configuration
│   │   ├── exceptions/                     # Custom exceptions
│   │   ├── model/
│   │   │   └── OrderWorkflowState.java     # Workflow state tracking
│   │   ├── OrderWorkflow.java              # Workflow interface
│   │   ├── OrderWorkflowImpl.java          # Workflow implementation
│   │   ├── SagaEventListener.java          # Kafka event listener
│   │   ├── SagaOrchestratorApplication.java
│   │   └── TemporalConfig.java             # Temporal client config
│   └── pom.xml
├── order-service/                          # Order management service
│   ├── src/main/java/com/saga/
│   │   ├── OrderController.java            # REST API controller
│   │   └── OrderServiceApplication.java
│   └── pom.xml
├── payment-service/                        # Payment processing service
│   ├── src/main/java/com/saga/
│   │   ├── PaymentService.java             # Payment logic & Kafka listener
│   │   └── PaymentServiceApplication.java
│   └── pom.xml
├── inventory-service/                      # Inventory management service
│   ├── src/main/java/com/saga/
│   │   ├── InventoryService.java           # Inventory logic & Kafka listener
│   │   └── InventoryServiceApplication.java
│   └── pom.xml
├── shipping-service/                       # Shipping service
│   ├── src/main/java/com/saga/
│   │   ├── ShippingService.java            # Shipping logic & Kafka listener
│   │   └── ShippingServiceApplication.java
│   └── pom.xml
├── docker-compose.yaml                     # Docker orchestration
├── test-saga.sh                            # Automated test script
├── POSTMAN_COLLECTION.json                 # Postman API collection
├── QUICK_TEST_GUIDE.md                     # Comprehensive testing guide
└── README.md
```

## ⚙️ Configuration

### Kafka Topics

All services communicate via dedicated Kafka topics:

| Topic | Purpose | Publishers | Consumers |
|-------|---------|------------|-----------|
| `order-events` | Order lifecycle events | Order Service | Saga Orchestrator |
| `payment-events` | Payment status updates | Payment Service | Saga Orchestrator |
| `inventory-events` | Inventory status updates | Inventory Service | Saga Orchestrator |
| `shipping-events` | Shipping status updates | Shipping Service | Saga Orchestrator |

### Temporal Workflow Configuration

**Saga Orchestrator Temporal Settings:**
- **Server Address**: `temporal:7233` (Docker) or `localhost:7233` (local)
- **Namespace**: `default`
- **Task Queue**: `ORDER_TASK_QUEUE`
- **Workflow ID**: Uses Order ID (e.g., `ORDER-123`)
- **Signal Wait Timeout**: 5 minutes per step
- **Activity Timeouts**: 
  - Start-to-Close: 1 minute
  - Schedule-to-Close: 2 minutes
  - Retry Policy: Exponential backoff, max 3 attempts

### Service Configuration Files

Each service has an `application.properties` file:

**Saga Orchestrator** (`saga-orchestrator/src/main/resources/application.properties`):
```properties
spring.application.name=saga-orchestrator
server.port=8085
spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:29092}
spring.kafka.consumer.group-id=saga-group
```

**Payment Service** (`payment-service/src/main/resources/application.properties`):
```properties
spring.application.name=payment-service
server.port=8082
payment.simulation.success=true  # Set to false to simulate payment failures
```

**Inventory Service** (`inventory-service/src/main/resources/application.properties`):
```properties
spring.application.name=inventory-service
server.port=8083
inventory.simulation.success=true  # Set to false to simulate inventory failures
```

**Shipping Service** (`shipping-service/src/main/resources/application.properties`):
```properties
spring.application.name=shipping-service
server.port=8084
shipping.simulation.success=false  # Default to false for testing compensation
```

### Simulating Failures

To test compensation flows, modify the simulation flags:
1. Update the service's `application.properties`
2. Rebuild the service: `docker-compose up -d --build <service-name>`
3. Send test request

**Or use the automated test script:**
```bash
./test-saga.sh  # Handles configuration changes automatically
```

## 📊 Monitoring & Observability

### Temporal UI Dashboard

Access the Temporal Web UI for comprehensive workflow monitoring:

```bash
# Open Temporal UI
open http://localhost:8233

# Or manually navigate to:
http://localhost:8233
```

**Features:**
- 📊 View all workflow executions
- 🔍 Search by workflow ID (order ID)
- 📜 Complete execution history with event timeline
- 🎯 Track signals and activity executions
- ✅ Monitor workflow status (Running, Completed, Failed, Compensated)
- 🔄 View compensation flows in detail
- 📈 Workflow performance metrics

**How to Use:**
1. Open http://localhost:8233
2. Click **Workflows** in sidebar
3. Search for your order ID (e.g., `ORDER-001`)
4. Click workflow to see detailed execution history
5. Expand events to see inputs, outputs, and errors

### Docker Compose Logs

**View all service logs:**
```bash
docker-compose logs -f
```

**View specific service:**
```bash
docker-compose logs -f saga-orchestrator
docker-compose logs -f payment-service
docker-compose logs -f inventory-service
docker-compose logs -f shipping-service
docker-compose logs -f order-service
```

**Search logs for specific patterns:**
```bash
# Search for compensation events
docker-compose logs saga-orchestrator | grep -i "compensat"

# Search for failures
docker-compose logs saga-orchestrator | grep -i "failed"

# Search for specific order
docker-compose logs saga-orchestrator | grep "ORDER-123"
```

### Kafka Event Monitoring

**List all topics:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-topics --list --bootstrap-server localhost:9092
```

**Monitor order events:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

**Monitor payment events:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning
```

**Monitor inventory events:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic inventory-events \
  --from-beginning
```

**Monitor shipping events:**
```bash
docker exec -it temporal-event-driven-saga-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic shipping-events \
  --from-beginning
```

### Service Health Checks

**Check all services status:**
```bash
docker-compose ps
```

**Check specific service health:**
```bash
# Check if service is running
docker-compose ps | grep order-service

# View service resource usage
docker stats temporal-event-driven-saga-saga-orchestrator-1
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Services Won't Start

**Symptoms:**
- Containers keep restarting
- Services show as "Unhealthy"
- Exit code errors in logs

**Solutions:**
```bash
# Check Docker logs for errors
docker-compose logs

# Restart all services
docker-compose down -v
docker-compose up -d --build

# Check individual service
docker-compose logs -f <service-name>

# Verify Docker has enough resources (Memory: 4GB+, CPU: 2+ cores)
```

#### 2. Kafka Connection Errors

**Symptoms:**
- "Connection refused" errors
- "Broker not available" messages
- Services can't publish/consume events

**Solutions:**
```bash
# Verify Kafka is healthy
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka and dependent services
docker-compose restart kafka
sleep 10
docker-compose restart order-service payment-service inventory-service shipping-service saga-orchestrator
```

#### 3. Temporal Connection Errors

**Symptoms:**
- "Failed to connect to Temporal" errors
- Workflows not starting
- Worker registration failures

**Solutions:**
```bash
# Verify Temporal is running and healthy
docker-compose ps temporal

# Check Temporal logs
docker-compose logs temporal

# Verify PostgreSQL is running (Temporal dependency)
docker-compose ps postgresql

# Restart Temporal and saga-orchestrator
docker-compose restart temporal
sleep 20
docker-compose restart saga-orchestrator
```

#### 4. Workflow Not Starting

**Check:**
1. Verify saga-orchestrator is running:
   ```bash
   docker-compose logs saga-orchestrator | grep "Started SagaOrchestratorApplication"
   ```

2. Check if ORDER_CREATED event was published:
   ```bash
   docker exec -it temporal-event-driven-saga-kafka-1 \
     kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic order-events \
     --from-beginning
   ```

3. Verify workflow in Temporal UI: http://localhost:8233

#### 5. Events Not Flowing Between Services

**Check:**
1. Verify Kafka consumer groups:
   ```bash
   docker exec -it temporal-event-driven-saga-kafka-1 \
     kafka-consumer-groups --list --bootstrap-server localhost:9092
   ```

2. Check consumer lag:
   ```bash
   docker exec -it temporal-event-driven-saga-kafka-1 \
     kafka-consumer-groups --describe --group saga-group \
     --bootstrap-server localhost:9092
   ```

3. Verify network connectivity:
   ```bash
   docker network inspect temporal-event-driven-saga_saga-network
   ```

#### 6. Port Already in Use

**Symptoms:**
- "Port is already allocated" error
- "Address already in use" message

**Solutions:**
```bash
# Check what's using the port
lsof -i :8081  # Order Service
lsof -i :8082  # Payment Service
lsof -i :8083  # Inventory Service
lsof -i :8084  # Shipping Service
lsof -i :8085  # Saga Orchestrator
lsof -i :9092  # Kafka

# Kill the process or change port in docker-compose.yaml
```

#### 7. Compensation Not Triggering

**Check:**
1. Verify failure event was published:
   ```bash
   docker-compose logs <service-name> | grep -i "failed"
   ```

2. Check saga-orchestrator logs:
   ```bash
   docker-compose logs saga-orchestrator | grep -i "compensat"
   ```

3. View workflow execution in Temporal UI

### Complete System Reset

If all else fails, perform a complete reset:

```bash
# Stop and remove all containers, volumes, and networks
docker-compose down -v

# Remove all images (optional, but ensures clean rebuild)
docker-compose down --rmi all -v

# Clean up orphaned containers
docker container prune -f

# Rebuild and start from scratch
docker-compose up -d --build

# Wait for all services to be healthy (60 seconds)
sleep 60
docker-compose ps
```

### Debug Mode

Enable debug logging for troubleshooting:

**Add to any service's `application.properties`:**
```properties
logging.level.com.saga=DEBUG
logging.level.org.springframework.kafka=DEBUG
logging.level.io.temporal=DEBUG
```

Then rebuild:
```bash
docker-compose up -d --build <service-name>
```

## 💡 Key Concepts & Implementation Details

### What is the Saga Pattern?

The **Saga Pattern** is a design pattern for managing distributed transactions across microservices. Instead of traditional ACID transactions, it breaks down a business transaction into a series of local transactions, each with a corresponding compensation (rollback) action.

### Why Temporal?

**Temporal** provides:
- ✅ **Durable Execution**: Workflows survive process crashes and restarts
- ✅ **Automatic Retries**: Built-in retry logic with exponential backoff
- ✅ **State Management**: Workflow state is automatically persisted
- ✅ **Event History**: Complete audit trail of all workflow events
- ✅ **Timeouts**: Built-in timeout handling for activities and signals
- ✅ **Versioning**: Support for workflow versioning and migration

### Event-Driven Architecture

This implementation uses **Kafka** for asynchronous, event-driven communication:
- **Loose Coupling**: Services don't directly depend on each other
- **Scalability**: Easy to scale individual services independently
- **Resilience**: Message delivery guarantees ensure no events are lost
- **Real-time**: Immediate event propagation across services

### Compensation Strategy

Compensations are executed in **reverse order** of successful operations:

```
Success Order:      Payment → Inventory → Shipping
Compensation Order: Shipping → Inventory → Payment (reverse)
```

**Example: Shipping Failure**
1. ✅ Payment successful → Register compensation
2. ✅ Inventory successful → Register compensation
3. ❌ Shipping fails
4. 🔄 Compensate Inventory (release reservation)
5. 🔄 Compensate Payment (refund)

## 🎓 Learning Resources

### Understanding the Code

**Key Files to Study:**
1. `OrderWorkflowImpl.java` - Core workflow logic with Temporal patterns
2. `SagaEventListener.java` - Kafka event consumption and Temporal signaling
3. `OrderActivitiesImpl.java` - Activity implementations for side effects
4. `PaymentService.java` - Example service with Kafka integration

### Temporal Concepts Used

- **Workflows**: Durable functions that orchestrate business logic
- **Activities**: Individual units of work with retry/timeout policies
- **Signals**: External events that workflows can wait for
- **Queries**: Read-only operations to inspect workflow state
- **Saga API**: Built-in compensation management
- **Promises**: Asynchronous coordination primitives

### Testing Patterns

The project demonstrates:
- **Happy Path Testing**: All operations succeed
- **Early Failure**: First operation fails (no compensation needed)
- **Mid-Flow Failure**: Partial compensation required
- **Late Failure**: Full compensation chain execution

## 🛠️ Development Workflow

### Adding a New Service

1. Create new service directory with Spring Boot structure
2. Add dependency on `common` module in `pom.xml`
3. Create Kafka listener for relevant events
4. Implement business logic with success/failure simulation
5. Add service to `docker-compose.yaml`
6. Update saga workflow to include new step

### Modifying the Workflow

1. Update `OrderWorkflow.java` interface (add signals/methods)
2. Update `OrderWorkflowImpl.java` implementation
3. Update `OrderActivities.java` for new activities
4. Add compensation logic if needed
5. Test with both success and failure scenarios

### Testing Changes Locally

```bash
# Build specific service
cd <service-name>
mvn clean install

# Run locally (infrastructure via Docker)
mvn spring-boot:run

# Or rebuild Docker container
cd ..
docker-compose up -d --build <service-name>

# View logs
docker-compose logs -f <service-name>
```

## 📚 Additional Documentation

- **[QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)** - Comprehensive testing guide with step-by-step instructions
- **[POSTMAN_COLLECTION.json](POSTMAN_COLLECTION.json)** - Postman collection with pre-configured API requests
- **[test-saga.sh](test-saga.sh)** - Automated test script for all scenarios

## 🔍 Example Workflow Execution

**Step-by-Step: Successful Order**

1. **User Action:**
   ```bash
   curl -X POST http://localhost:8081/orders/ORDER-001
   ```

2. **Order Service:**
   - Publishes `ORDER_CREATED` event to `order-events` topic

3. **Saga Orchestrator:**
   - Kafka listener receives `ORDER_CREATED` event
   - Starts Temporal workflow with ID `ORDER-001`
   - Workflow publishes `PROCESS_PAYMENT` via Activity

4. **Payment Service:**
   - Consumes `PROCESS_PAYMENT` event
   - Processes payment (simulated)
   - Publishes `PAYMENT_COMPLETED` event

5. **Saga Orchestrator:**
   - Receives `PAYMENT_COMPLETED`
   - Signals workflow: `onPaymentCompleted()`
   - Workflow continues to inventory step
   - Publishes `RESERVE_INVENTORY` via Activity

6. **Inventory Service:**
   - Consumes `RESERVE_INVENTORY` event
   - Reserves inventory (simulated)
   - Publishes `INVENTORY_RESERVED` event

7. **Saga Orchestrator:**
   - Receives `INVENTORY_RESERVED`
   - Signals workflow: `onInventoryReserved()`
   - Workflow continues to shipping step
   - Publishes `PROCESS_SHIPPING` via Activity

8. **Shipping Service:**
   - Consumes `PROCESS_SHIPPING` event
   - Arranges shipping (simulated)
   - Publishes `SHIPPING_COMPLETED` event

9. **Saga Orchestrator:**
   - Receives `SHIPPING_COMPLETED`
   - Signals workflow: `onShippingCompleted()`
   - Workflow completes successfully ✅

10. **Temporal UI:**
    - Shows completed workflow with green status
    - Full event history available for audit

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Reporting Issues

1. Check existing issues first
2. Provide detailed description with:
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs and error messages
   - Environment details (OS, Docker version, etc.)

### Submitting Changes

1. Fork the repository
2. Create your feature branch:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. Make your changes:
   - Follow existing code style
   - Add tests if applicable
   - Update documentation
4. Commit your changes:
   ```bash
   git commit -m 'Add amazing feature'
   ```
5. Push to the branch:
   ```bash
   git push origin feature/amazing-feature
   ```
6. Open a Pull Request with:
   - Clear description of changes
   - Link to related issues
   - Test results

### Development Guidelines

- Use Java 21 features appropriately
- Follow Spring Boot best practices
- Write meaningful commit messages
- Keep Temporal workflows deterministic
- Add logging for debugging
- Include error handling
- Update tests and documentation

## 📄 License

This project is provided as-is for educational and demonstration purposes.

## 📧 Contact & Support

### Getting Help

- 📖 Review the [QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md) for detailed testing instructions
- 🐛 Open an issue on GitHub for bugs or questions
- 💡 Check Temporal documentation: https://docs.temporal.io
- 📚 Spring Boot guides: https://spring.io/guides
- 🎯 Kafka documentation: https://kafka.apache.org/documentation

### Useful Links

- **Temporal IO**: https://temporal.io
- **Temporal Java SDK**: https://github.com/temporalio/sdk-java
- **Spring Kafka**: https://spring.io/projects/spring-kafka
- **Saga Pattern**: https://microservices.io/patterns/data/saga.html

## ⭐ Project Highlights

This implementation showcases:

✨ **Production-Ready Patterns**
- Proper error handling and compensation
- Timeout management for resilience
- Retry policies with exponential backoff
- Comprehensive logging and monitoring

✨ **Best Practices**
- Separation of concerns (workflow vs activities)
- Event sourcing with Kafka
- Idempotent operations
- Configuration externalization

✨ **Modern Technologies**
- Java 21 with latest features
- Spring Boot 3.4.2
- Temporal 1.24.2
- Kafka 7.5.1
- Docker containerization

✨ **Developer Experience**
- Full Docker Compose setup
- Automated test scripts
- Comprehensive documentation
- Postman collection included
- Easy-to-understand code structure

---

**Made with ❤️ using Spring Boot, Temporal, and Kafka**

*For detailed testing instructions and examples, see [QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)*