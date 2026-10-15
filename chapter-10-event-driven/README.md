 # Chapter 10: Event-Driven Architecture with Spring Cloud Stream

This chapter demonstrates event-driven architecture for the ChargeRoute platform using Spring Cloud Stream and RabbitMQ. Services communicate asynchronously through events, achieving loose coupling, independent scaling, and graceful failure handling.

## Overview

Chapter 10 transforms the reactive microservices from Chapter 9 to use event-driven communication patterns. Instead of services calling each other directly via HTTP, they publish and consume events through RabbitMQ, breaking temporal, availability, and knowledge coupling.

### Event Flows Implemented

**1. User Registration Flow (Targeted Events)**
- **Publisher**: user-management-service
- **Consumer**: notification-management-service
- **Pattern**: One-to-one event consumption with consumer groups
- **Use Case**: Send welcome email when user registers

**2. Charge Unit Status Broadcasting (Broadcast Events)**
- **Publisher**: charge-unit-management-service
- **Consumer**: session-management-service (others can subscribe independently)
- **Pattern**: One-to-many event broadcasting with independent consumer groups
- **Use Case**: Track charge unit availability across multiple services

**3. Internal Events (ApplicationEventPublisher)**
- **Publisher**: user-management-service
- **Consumer**: UserPreferencesInitializer (same service)
- **Pattern**: In-process event handling without message broker
- **Use Case**: Initialize user preferences after account creation

## Key Technologies

- **Spring Boot**: 3.5.6
- **Spring Cloud Stream**: 4.x (functional programming model)
- **RabbitMQ**: 3.12 with management plugin
- **Spring WebFlux**: Reactive web framework (from Chapter 9)
- **Spring Data R2DBC**: Reactive database access (from Chapter 9)
- **Testcontainers**: Integration testing with PostgreSQL

## Architecture Benefits

### Breaking Coupling Chains

**Temporal Coupling (Performance)**:
- Synchronous: User registration waits for all downstream services (350ms+)
- Event-driven: User registration completes immediately (50ms database save)

**Availability Coupling (Resilience)**:
- Synchronous: If notification service is down, user registration fails
- Event-driven: Events queue in RabbitMQ, processed when service recovers

**Knowledge Coupling (Maintainability)**:
- Synchronous: User service must know about all consumers
- Event-driven: User service publishes event, consumers subscribe independently

**Deployment Coupling (Independence)**:
- Synchronous: Interface changes require coordinated deployments
- Event-driven: Services evolve independently, only event schema matters

## Running the Application

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop (for PostgreSQL, Keycloak, Vault, Redis, RabbitMQ, and TestContainers)

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** (port 5433) - single instance hosting all service databases
- **Redis** (port 6379) - API Gateway rate limiting
- **Keycloak** (port 9000) - OAuth2/OIDC with auto-imported ev-charging realm
- **Vault** (port 8200) - secrets management (dev mode)
- **Vault Init** - automatically seeds database passwords into Vault (runs once and exits)
- **RabbitMQ** (ports 5672, 15672) - message broker with management UI
- **pgAdmin** (port 5050) - database admin UI

> **Note:** Redis is required by the API Gateway for rate limiting, introduced in Chapter 5.

Wait for all containers to be healthy:

```bash
docker compose ps
```

### 2. Build All Services

```bash
mvn clean install -DskipTests
```

### 3. Start Services

Use the service manager script:

```bash
cd scripts
./service-manager.sh start
```

Or start manually in order:

```bash
# Terminal 1: Config Server (must start first)
cd config-server && mvn spring-boot:run

# Terminal 2: Eureka Server (wait for Config Server to be ready on port 8888)
cd eureka-server && mvn spring-boot:run

# Terminal 3: API Gateway (wait for Eureka to be ready on port 8761)
cd api-gateway && mvn spring-boot:run

# Terminals 4-7: Business services (can start in parallel after Eureka is ready)
cd user-management-service && mvn spring-boot:run
cd charge-unit-management-service && mvn spring-boot:run
cd session-management-service && mvn spring-boot:run
cd notification-management-service && mvn spring-boot:run
```

### 4. Verify

```bash
./service-manager.sh status
```

- Eureka Dashboard: http://localhost:8761
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Keycloak Admin: http://localhost:9000/admin (admin/admin)
- API Gateway: http://localhost:8080

## Demonstrating Event-Driven Flows

All endpoints require an OAuth2 token from Keycloak. First, obtain a token:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=admin-client' \
  -d 'client_secret=admin-secret' \
  -d 'grant_type=password' \
  -d 'username=admin@evcharging.com' \
  -d 'password=password' | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Demo 1: User Registration Event Flow

Creating a user triggers a chain of events: the user service saves the user, publishes an internal `UserCreatedEvent` (which initializes user preferences in the same service), and publishes an external `UserRegisteredEvent` to RabbitMQ. The notification service consumes the event and creates a welcome notification.

**Step 1: Create a user (triggers events)**

```bash
curl -s -X POST http://localhost:8080/user-management-service/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "phoneNumber": "+1234567890",
    "password": "securepass123"
  }' | jq
```

**Step 2: Check the notification service received the external event**

```bash
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```
You should see a welcome notification created automatically - no direct HTTP call between the services.

**Step 3: Verify the user preference is created from internal event**

```bash
curl -s http://localhost:8080/user-management-service/api/users/USER_ID/preferences \
-H "Authorization: Bearer $TOKEN" | jq
```
You should see a user preference created automatically - no direct HTTP call between the services.

**Step 4: Verify in RabbitMQ Management UI**

Open http://localhost:15672 (guest/guest) and check:
- Exchanges: `user-events` (topic exchange)
- Queues: `notification-service.user-events` (bound consumer group)
- Message rates show the published and consumed event

### Demo 2: Charge Unit Status Broadcast

Updating a charge unit's status publishes a `ChargeUnitStatusChangedEvent` as a broadcast. Multiple services can subscribe independently without the publisher knowing about them.

**Step 1: Create a charge unit**

```bash
curl -s -X POST http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stationName": "Downtown Station A1",
    "address": "123 Main St, Downtown",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "connectorType": "CCS",
    "maxPowerKw": 150,
    "pricePerKwh": 0.35
  }' | jq
```

Save the `chargeUnitId` from the response for the next step.

**Step 2: Update the charge unit status (triggers broadcast event)**

```bash
curl -s -X PUT http://localhost:8080/charge-unit-management-service/api/charge-units/CHARGE_UNIT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "AVAILABLE"
  }' | jq
```

Replace `CHARGE_UNIT_ID` with the ID from step 1. The status change publishes a `ChargeUnitStatusChangedEvent` to RabbitMQ, which the session service consumes to track charge unit availability.

**Step 3: Verify in RabbitMQ**

Check the `charge-unit-events` exchange and `session-service.charge-unit-events` queue in the RabbitMQ Management UI.

### Demo 3: Viewing Events in Logs

Watch the service logs to see events flow between services in real time:

```bash
# In separate terminals, watch each service's logs:
./scripts/service-manager.sh logs user-management-service
./scripts/service-manager.sh logs notification-management-service
./scripts/service-manager.sh logs charge-unit-management-service
./scripts/service-manager.sh logs session-management-service
```

When you create a user, you should see:
1. **user-management-service**: "Published external UserRegisteredEvent to message broker"
2. **notification-management-service**: "Received UserRegisteredEvent for user" followed by "Created welcome notification"

### Other API Endpoints

```bash
# List all users
curl -s http://localhost:8080/user-management-service/api/users \
  -H "Authorization: Bearer $TOKEN" | jq

# List all charge units
curl -s http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" | jq

# List all sessions
curl -s http://localhost:8080/session-management-service/api/sessions \
  -H "Authorization: Bearer $TOKEN" | jq

# List all notifications
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```

All requests go through the API Gateway on port 8080 using the pattern `http://localhost:8080/{service-name}/api/...`.

## Testing

```bash
# Run all tests
mvn test

# Run a specific service's tests
cd user-management-service && mvn test
```

## Stopping Everything

```bash
# Stop Spring Boot services
cd scripts && ./service-manager.sh stop

# Stop infrastructure containers
docker compose down

# To also remove database volumes:
docker compose down -v
```

## Event Patterns Demonstrated

### 1. Publishing Events (User Service)

```java
@Service
public class UserService {
    private final StreamBridge streamBridge;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<User> createUser(UserRegistrationRequest request) {
        String correlationId = UUID.randomUUID().toString();

        return userRepository.save(userEntity)
            .doOnNext(savedUser -> {
                // Publish internal event (same service)
                eventPublisher.publishEvent(
                    new UserCreatedEvent(this, savedUser.getUserId(), savedUser.getEmail())
                );

                // Publish external event (cross-service)
                UserRegisteredEvent event = new UserRegisteredEvent(
                    correlationId, savedUser.getUserId(), savedUser.getEmail(),
                    fullName, savedUser.getMembershipType()
                );
                streamBridge.send("userRegistered-out-0", event);
            })
            .map(userMapper::toDto);
    }
}
```

### 2. Consuming Events (Notification Service)

```java
@Component
public class UserEventConsumer {
    private final NotificationRepository notificationRepository;

    @Bean
    public Consumer<UserRegisteredEvent> handleUserRegistered() {
        return event -> {
            // Idempotency check
            String notificationId = "notif-" + event.getEventId();
            if (notificationRepository.existsById(notificationId)) {
                log.warn("Event {} already processed, skipping", event.getEventId());
                return;
            }

            // Create welcome notification
            NotificationEntity notification = new NotificationEntity();
            notification.setNotificationId(notificationId);
            notification.setUserId(event.getUserId());
            notification.setSubject("Welcome to ChargeRoute!");
            notification.setMessage(buildWelcomeMessage(event));
            notification.setStatus(NotificationStatus.PENDING);

            notificationRepository.save(notification);
        };
    }
}
```

### 3. Internal Event Handling (UserPreferencesInitializer)

```java
@Component
public class UserPreferencesInitializer {
    private final UserPreferencesRepository preferencesRepository;

    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        UserPreferencesEntity preferences = new UserPreferencesEntity();
        preferences.setUserId(event.getUserId());
        preferences.setNotificationsEnabled(true);
        preferences.setNotificationChannel("EMAIL");
        preferences.setAutoChargeEnabled(false);

        preferencesRepository.save(preferences).subscribe();
    }
}
```

### 4. Broadcast Events (Charge Unit Service)

```java
@Service
public class ChargeUnitService {
    private final StreamBridge streamBridge;

    public Mono<ChargeUnit> updateStatus(String chargeUnitId, ChargeUnitStatus newStatus) {
        return chargeUnitRepository.findById(chargeUnitId)
            .flatMap(chargeUnit -> {
                ChargeUnitStatus previousStatus = chargeUnit.getStatus();
                if (previousStatus == newStatus) {
                    return Mono.just(chargeUnit);
                }

                chargeUnit.setStatus(newStatus);
                return chargeUnitRepository.save(chargeUnit)
                    .doOnNext(updated -> {
                        ChargeUnitStatusChangedEvent event = new ChargeUnitStatusChangedEvent(
                            correlationId, updated.getChargeUnitId(),
                            previousStatus.name(), newStatus.name(),
                            currentPower, location
                        );
                        streamBridge.send("chargeUnitStatusChanged-out-0", event);
                    });
            })
            .map(chargeUnitMapper::toDto);
    }
}
```

## Configuration

### Spring Cloud Stream Bindings

**Producer Configuration** (user-management-service.yml):
```yaml
spring:
  cloud:
    stream:
      bindings:
        userRegistered-out-0:
          destination: user-events
          content-type: application/json
      rabbit:
        bindings:
          userRegistered-out-0:
            producer:
              declare-exchange: true
              exchange-type: topic
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**Consumer Configuration** (notification-management-service.yml):
```yaml
spring:
  cloud:
    stream:
      bindings:
        handleUserRegistered-in-0:
          destination: user-events
          group: notification-service
          content-type: application/json
      function:
        definition: handleUserRegistered
      rabbit:
        bindings:
          handleUserRegistered-in-0:
            consumer:
              auto-bind-dlq: true
              republish-to-dlq: true
              max-attempts: 3
              initial-interval: 1000
              multiplier: 2.0
```

## Key Implementation Details

### 1. Event Metadata for Idempotency and Tracing

All events extend BaseEvent with standard metadata:

```java
public abstract class BaseEvent {
    private String eventId = UUID.randomUUID().toString();  // For idempotency
    private LocalDateTime occurredAt = LocalDateTime.now();
    private String correlationId;  // For distributed tracing
    private String eventType;

    protected BaseEvent(String correlationId, String eventType) {
        this.correlationId = correlationId;
        this.eventType = eventType;
    }
}
```

### 2. Idempotency Pattern

Consumers use eventId to prevent duplicate processing:

```java
String notificationId = "notif-" + event.getEventId();
if (notificationRepository.existsById(notificationId)) {
    return;  // Already processed
}
```

### 3. Dead Letter Queue (DLQ)

Failed events automatically move to DLQ after retry exhaustion:

```yaml
consumer:
  auto-bind-dlq: true
  republish-to-dlq: true
  max-attempts: 3
```

Monitor DLQ in RabbitMQ Management UI at http://localhost:15672

### 4. Consumer Groups

Multiple instances share load within a consumer group:

```yaml
bindings:
  handleUserRegistered-in-0:
    group: notification-service  # All instances share this group
```

Without a group, every instance processes every event (broadcast behavior).

### 5. Testing Without RabbitMQ

Test configuration excludes RabbitMQ autoconfiguration:

```yaml
# application-test.yml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
      - org.springframework.cloud.stream.binder.rabbit.config.RabbitServiceAutoConfiguration
```

Tests use @MockBean for StreamBridge:

```java
@MockBean
private StreamBridge streamBridge;

@Test
void shouldPublishEvent() {
    userService.createUser(request).block();
    verify(streamBridge).send(eq("userRegistered-out-0"), any(UserRegisteredEvent.class));
}
```

## RabbitMQ Management

### Access Management UI

```
URL: http://localhost:15672
Username: guest
Password: guest
```

### Key Monitoring Points

- **Exchanges**: user-events, charge-unit-events
- **Queues**: notification-service.user-events, session-service.charge-unit-events
- **Message Rates**: Publishing and consumption throughput
- **Queue Depth**: Backlog of unprocessed messages
- **DLQ Messages**: Failed messages requiring attention

### Message Persistence

Enable durable queues and persistent messages for critical events:

```yaml
rabbit:
  bindings:
    userRegistered-out-0:
      producer:
        delivery-mode: PERSISTENT
    handleUserRegistered-in-0:
      consumer:
        durable: true
```

## Troubleshooting

### Issue: Events not being consumed

**Check**:
1. Consumer function name matches binding: `handleUserRegistered-in-0`
2. Function defined in `spring.cloud.stream.function.definition`
3. Destination matches: `user-events`
4. RabbitMQ connection successful (check logs)

### Issue: Duplicate event processing

**Solution**: Implement idempotency using eventId:
```java
if (repository.existsById("prefix-" + event.getEventId())) {
    return;  // Skip duplicate
}
```

### Issue: Messages in DLQ

**Investigate**:
1. Check RabbitMQ Management UI for error details
2. Review application logs for processing errors
3. Fix bug and redeploy
4. Replay messages from DLQ (manual or automated)

### Issue: Tests fail with RabbitMQ connection refused

**Solution**: Exclude RabbitMQ autoconfiguration in test profile:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

And mock StreamBridge in tests:
```java
@MockBean
private StreamBridge streamBridge;
```

## Event Flow Examples

### User Registration Complete Flow

1. Client POSTs to `/api/users/register`
2. User service creates user in database (50ms)
3. User service publishes UserCreatedEvent (internal, in-process)
4. UserPreferencesInitializer handles event, creates preferences
5. User service publishes UserRegisteredEvent (external, RabbitMQ)
6. Response returned to client (total: ~50ms)
7. RabbitMQ delivers event to notification service queue
8. Notification service consumes event (async)
9. Notification service creates welcome email notification
10. Email sent asynchronously

**Result**: User registration completes in 50ms, all side effects happen asynchronously.

### Charge Unit Status Change Flow

1. Charge unit goes AVAILABLE → OCCUPIED
2. Charge unit service publishes ChargeUnitStatusChangedEvent
3. RabbitMQ broadcasts to all interested consumer groups:
   - Session service (tracks availability)
   - Notification service (alerts nearby users)
   - Analytics service (records utilization)
4. Each service processes independently at its own pace

**Result**: One status change triggers multiple independent reactions without coupling.
