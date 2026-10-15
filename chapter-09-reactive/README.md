# Chapter 9: Reactive Programming with Spring WebFlux

This chapter converts three of the four ChargeRoute microservices from blocking Spring MVC/JPA to reactive Spring WebFlux/R2DBC. The notification service intentionally remains blocking to demonstrate the challenges of mixing paradigms.

| Service | Stack | Purpose |
|---------|-------|---------|
| user-management-service | WebFlux + R2DBC | Basic reactive patterns |
| charge-unit-management-service | WebFlux + R2DBC | Server-Sent Events streaming |
| session-management-service | WebFlux + R2DBC | Parallel reactive service calls |
| notification-management-service | Spring MVC + JPA | Remains blocking (intentional) |

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop (for PostgreSQL, Keycloak, Vault, Redis, and TestContainers)

## Quick Start

### 1. Start Infrastructure (PostgreSQL, Keycloak, Vault, Redis)

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** (port 5433) - single instance hosting all service databases
- **Redis** (port 6379) - API Gateway rate limiting
- **Keycloak** (port 9000) - OAuth2/OIDC with auto-imported ev-charging realm
- **Vault** (port 8200) - secrets management (dev mode)
- **Vault Init** - automatically seeds database passwords into Vault (runs once and exits)
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

Start services in order (each needs the previous to be ready):

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

Alternatively, use the service manager script:

```bash
cd scripts
./service-manager.sh start
```

### 4. Verify

- Eureka Dashboard: http://localhost:8761
- Keycloak Admin: http://localhost:9000/admin (admin/admin)
- pgAdmin: http://localhost:5050 (admin@chargeroute.com / admin123)
- API Gateway: http://localhost:8080

## Project Structure

```
chapter-09-reactive/
├── docker-compose.yml              # PostgreSQL, Redis, Keycloak, Vault, pgAdmin
├── ev-charging-realm.json          # Keycloak realm (auto-imported)
├── config-server/                  # Spring Cloud Config Server
├── config-data/                    # Centralized configuration files
├── eureka-server/                  # Service discovery
├── api-gateway/                    # Spring Cloud Gateway
├── user-management-service/        # Reactive user CRUD (WebFlux + R2DBC)
├── charge-unit-management-service/ # Reactive charge units with SSE streaming
├── session-management-service/     # Reactive sessions with parallel calls
├── notification-management-service/# Blocking notifications (Spring MVC + JPA)
├── scripts/
│   ├── service-manager.sh          # Start/stop all services
│   ├── kill-orphans.sh             # Find and kill orphaned services
│   ├── init-databases.sh           # Creates per-service databases (runs in Docker)
│   ├── init-vault-secrets.sh       # Seeds Vault with secrets (manual alternative)
│   └── verify-infrastructure.sh    # Health checks
```

## Service Databases

All databases run on the single PostgreSQL instance (port 5433). The `init-databases.sh` script creates them automatically:

| Service | Database |
|---------|----------|
| User Management | userdb |
| Charge Unit Management | chargeunitdb |
| Session Management | sessiondb |
| Notification Management | notificationdb |
| Keycloak | keycloak |

## Testing with curl

All endpoints require an OAuth2 token from Keycloak. First, obtain a token, then use it in requests.

### Get an Admin Token

The `admin-client` has the `admin.all` scope and supports the password grant:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=admin-client' \
  -d 'client_secret=admin-secret' \
  -d 'grant_type=password' \
  -d 'username=admin@evcharging.com' \
  -d 'password=password' | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Get a Service-to-Service Token

The `session-service-client` has the `service.internal` scope:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=session-service-client' \
  -d 'client_secret=session-secret' \
  -d 'grant_type=client_credentials' | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### User Management Service

```bash
# Create a user
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

# List all users
curl -s http://localhost:8080/user-management-service/api/users \
  -H "Authorization: Bearer $TOKEN" | jq

# Get a specific user (replace USER_ID)
curl -s http://localhost:8080/user-management-service/api/users/USER_ID \
  -H "Authorization: Bearer $TOKEN" | jq

# Delete a user
curl -s -X DELETE http://localhost:8080/user-management-service/api/users/USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Charge Unit Management Service

```bash
# List charge units
curl -s http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" | jq

# Stream charge unit status updates (Server-Sent Events)
curl -N http://localhost:8080/charge-unit-management-service/api/charge-units/stream \
  -H "Authorization: Bearer $TOKEN"
```

### Session Management Service

```bash
# List sessions
curl -s http://localhost:8080/session-management-service/api/sessions \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Notification Management Service

```bash
# List notifications
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```

All requests go through the API Gateway on port 8080. The URL pattern is `http://localhost:8080/{service-name}/api/...`. The gateway strips the service name prefix and forwards the rest to the service.

## Automated Testing

Tests use TestContainers to spin up an isolated PostgreSQL instance automatically:

```bash
# Run all tests
mvn test

# Run a specific service's tests
cd user-management-service && mvn test

# Run a specific test class
mvn test -Dtest=UserServiceIntegrationTest
```

No manual database setup is required for tests.

## Stopping Everything

```bash
# Stop Spring Boot services
cd scripts && ./service-manager.sh stop

# Stop infrastructure containers
docker compose down
```

To also remove database volumes:

```bash
docker compose down -v
```
