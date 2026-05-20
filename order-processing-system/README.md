# Event-Driven Order Processing System

A production-style microservices architecture implementing an order processing pipeline using **Java 17**, **Spring Boot 3**, **Apache Kafka**, and **PostgreSQL**. Three decoupled services communicate asynchronously via Kafka events, following event-driven design principles.

---

## Architecture

```
┌─────────────────┐         ┌─────────────────────────┐
│   REST Client   │────────▶│      Order Service       │
└─────────────────┘  HTTP   │  (Spring Boot : 8081)    │
                            │  - Place / cancel orders  │
                            │  - Persists to PostgreSQL │
                            └────────────┬─────────────┘
                                         │ publishes
                                         ▼
                            ┌─────────────────────────┐
                            │    Kafka Topic:          │
                            │    order-events          │
                            └────────┬────────┬────────┘
                                     │        │ subscribes
                            subscribes│        ▼
                                     │  ┌─────────────────────────┐
                                     │  │   Inventory Service      │
                                     │  │  (Spring Boot : 8082)    │
                                     │  │  - Reserves stock        │
                                     │  │  - Releases on cancel    │
                                     │  └─────────────────────────┘
                                     ▼
                            ┌─────────────────────────┐
                            │  Notification Service    │
                            │  (Spring Boot : 8083)    │
                            │  - Logs order messages   │
                            │  - Ready for email/SMS   │
                            └─────────────────────────┘
```

### Event Flow

```
Client POST /api/v1/orders
    → Order Service creates order (status: CREATED)
    → Publishes ORDER_PLACED event to Kafka
        → Inventory Service reserves stock
        → Notification Service logs "Order received" message

Client PATCH /api/v1/orders/{id}/cancel
    → Order Service updates order (status: CANCELLED)
    → Publishes ORDER_CANCELLED event to Kafka
        → Inventory Service releases reserved stock
        → Notification Service logs "Order cancelled" message
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka (Confluent 7.4) |
| Database | PostgreSQL 15 |
| Containerization | Docker, Docker Compose |
| Build | Maven |

---

## Project Structure

```
order-processing-system/
├── docker-compose.yml
├── docker/
│   └── init.sql
├── order-service/          # REST API + Kafka producer
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/java/com/ecom/
│           ├── controller/OrderController.java
│           ├── service/OrderService.java
│           ├── entity/Order.java
│           ├── repository/OrderRepository.java
│           ├── event/OrderEvent.java
│           ├── dto/OrderDTO.java
│           └── config/KafkaProducerConfig.java
├── inventory-service/      # Kafka consumer — stock management
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/java/com/ecom/
│           ├── service/InventoryService.java
│           ├── controller/InventoryController.java
│           ├── event/OrderEvent.java
│           └── config/KafkaConsumerConfig.java
└── notification-service/   # Kafka consumer — customer messaging
    ├── Dockerfile
    ├── pom.xml
    └── src/
        └── main/java/com/ecom/
            ├── service/NotificationService.java
            ├── controller/NotificationController.java
            ├── event/OrderEvent.java
            └── config/KafkaConsumerConfig.java
```

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for running services locally without Docker)
- Maven 3.8+

### Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/order-processing-system.git
cd order-processing-system

# Start all services (Zookeeper, Kafka, PostgreSQL, all 3 microservices)
docker-compose up --build

# Verify all services are healthy
curl http://localhost:8081/api/v1/orders/health
curl http://localhost:8082/api/v1/inventory/health
curl http://localhost:8083/api/v1/notifications/health
```

### Run Locally (without Docker)

Start Kafka and PostgreSQL via Docker, then run each Spring Boot service:

```bash
# Start infrastructure only
docker-compose up zookeeper kafka postgres

# In separate terminals:
cd order-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

---

## API Reference

### Order Service (port 8081)

#### Place an order
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "productId": "PROD-001",
    "quantity": 2,
    "totalAmount": 199.99
  }'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-123",
  "productId": "PROD-001",
  "quantity": 2,
  "totalAmount": 199.99,
  "status": "CREATED",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get order by ID
```bash
curl http://localhost:8081/api/v1/orders/{orderId}
```

#### Get orders by customer
```bash
curl http://localhost:8081/api/v1/orders/customer/CUST-123
```

#### Cancel an order
```bash
curl -X PATCH http://localhost:8081/api/v1/orders/{orderId}/cancel
```

### Inventory Service (port 8082)

#### Check current stock
```bash
curl http://localhost:8082/api/v1/inventory/stock
```

Response:
```json
{
  "PROD-001": 98,
  "PROD-002": 50,
  "PROD-003": 25
}
```

### Notification Service (port 8083)

#### View notification log
```bash
curl http://localhost:8083/api/v1/notifications
```

Response:
```json
[
  "[ORDER_PLACED] Hi customer CUST-123! Your order 550e8400... for product PROD-001 (qty: 2) has been received.",
  "[ORDER_CANCELLED] Order 550e8400... has been cancelled as requested."
]
```

---

## Key Design Decisions

**Idempotent Kafka producer** — configured with `enable.idempotence=true` and `acks=all` to prevent duplicate events during retries.

**Decoupled consumers** — each service has its own Kafka consumer group (`inventory-group`, `notification-group`), so both receive every event independently. Adding a new service (e.g. analytics) requires zero changes to existing services.

**Transactional order creation** — `@Transactional` on `placeOrder()` ensures the DB write and Kafka publish either both succeed or both roll back, keeping data consistent.

**Order lifecycle** — orders transition through `CREATED → CONFIRMED → FAILED / CANCELLED`. Status transitions are validated server-side before publishing events.

---

## Extending This Project

Some natural next steps if you want to go deeper:

- **Saga pattern** — implement a compensation flow where the inventory service publishes a `STOCK_UNAVAILABLE` event and the order service marks the order as `FAILED`
- **Payment service** — add a third consumer that processes payments and publishes `PAYMENT_SUCCESS` / `PAYMENT_FAILED` events
- **Dead letter queue** — configure a DLQ topic for events that fail processing after N retries
- **Observability** — add Micrometer + Prometheus + Grafana for end-to-end latency tracking across services
- **Kubernetes deployment** — Helm charts for deploying to a K8s cluster with HPA for auto-scaling consumers

---

## License

MIT
