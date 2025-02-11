# Saga Pattern Microservices Implementation

A microservices-based implementation of the Saga pattern using Spring Boot, Apache Kafka, and Temporal Workflow Engine. This project demonstrates distributed transaction management across multiple services.

## Architecture Overview

The project consists of the following microservices:

- **Saga Orchestrator** (Port: 8085): Coordinates the distributed transactions
- **Order Service** (Port: 8081): Handles order creation and management
- **Payment Service**: Processes payment transactions
- **Inventory Service**: Manages product inventory
- **Shipping Service**: Handles shipping logistics

## Technology Stack

- Java 21
- Spring Boot 3.4.2
- Apache Kafka
- Temporal Workflow Engine
- Maven

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker and Docker Compose
- Kafka
- Temporal Server

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <project-directory>
```

### 2. Start Infrastructure Services

Start Kafka and Temporal using Docker Compose:

```bash
docker-compose up -d
```

### 3. Build Services

Build all services using Maven:

```bash
mvn clean install
```

Or build individual services:

```bash
cd order-service && mvn clean install
cd ../payment-service && mvn clean install
cd ../inventory-service && mvn clean install
cd ../shipping-service && mvn clean install
cd ../saga-orchestrator && mvn clean install
```

### 4. Start Services

Start each service in a separate terminal:

```bash
# Saga Orchestrator
cd saga-orchestrator && mvn spring-boot:run

# Order Service
cd order-service && mvn spring-boot:run

# Payment Service
cd payment-service && mvn spring-boot:run

# Inventory Service
cd inventory-service && mvn spring-boot:run

# Shipping Service
cd shipping-service && mvn spring-boot:run
```

Alternatively, use the provided VS Code launch configurations in `.vscode/launch.json`.

## API Endpoints

### Order Service
- Create Order: `POST /orders/{orderId}`
  ```bash
  curl -X POST http://localhost:8081/orders/{orderId}
  ```

## Event Flow

1. Order Creation (`ORDER_CREATED`)
2. Inventory Check
3. Payment Processing
4. Shipping Arrangement
5. Order Completion

## Development

### Project Structure
```
├── saga-orchestrator/
├── order-service/
├── payment-service/
├── inventory-service/
├── shipping-service/
├── docker-compose.yaml
└── README.md
```

### Configuration

Each service has its own `application.properties` file with configurations for:
- Service port
- Kafka settings
- Application-specific properties

### Kafka Topics

- `order-events`: Order-related events
- Additional topics as needed for service communication

### Temporal Workflow

The Saga orchestrator uses Temporal for workflow management:
- Task Queue: `ORDER_TASK_QUEUE`
- Namespace: `default`
- Server: `localhost:7233`

## Testing

To test the complete flow:

1. Start all services
2. Create an order using the Order Service API
3. Monitor the logs of each service
4. Check Kafka topics for event flow
5. Verify Temporal workflows

## Troubleshooting

### Common Issues

1. **Services Won't Start**
   - Check if Kafka is running
   - Verify port availability
   - Check Java version

2. **Kafka Connection Issues**
   - Verify Kafka is running: `docker ps`
   - Check Kafka logs: `docker logs kafka`

3. **Temporal Connection Issues**
   - Verify Temporal server is running
   - Check connection settings in `TemporalConfig.java`

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

[Add License Information]

## Contact

[Add Contact Information]