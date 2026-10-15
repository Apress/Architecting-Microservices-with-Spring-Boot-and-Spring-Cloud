# Chapter 8: Data Persistence with Spring Data JPA

This chapter demonstrates production-ready data persistence for the ChargeRoute platform using Spring Data JPA, PostgreSQL, Flyway migrations, MapStruct for entity-DTO mapping, and TestContainers for integration testing.

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
- **pgAdmin** (port 5050) - database admin UI

> **Note:** Redis is required by the API Gateway for rate limiting, introduced in Chapter 5.

Wait for all containers to be healthy:

```bash
docker compose ps
```

### 2. Seed Vault Secrets

```bash
./scripts/init-vault-secrets.sh
```

### 3. Build All Services

```bash
mvn clean install -DskipTests
```

### 4. Start Services

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

### 5. Verify

- Eureka Dashboard: http://localhost:8761 (chargeroute / Admin123!)
- Keycloak Admin: http://localhost:9000/admin (admin/admin)
- pgAdmin: http://localhost:5050 (admin@chargeroute.com / admin123)
- API Gateway: http://localhost:8080

## Project Structure

```
chapter-08-persistence/
├── docker-compose.yml              # PostgreSQL, Redis, Keycloak, Vault, pgAdmin
├── ev-charging-realm.json          # Keycloak realm (auto-imported)
├── config-server/                  # Spring Cloud Config Server
├── config-data/                    # Centralized configuration files
├── eureka-server/                  # Service discovery
├── api-gateway/                    # Spring Cloud Gateway
├── user-management-service/        # User CRUD with JPA
├── charge-unit-management-service/ # Charge unit management
├── session-management-service/     # Charging sessions with metrics
├── notification-management-service/# Notifications
├── scripts/
│   ├── service-manager.sh          # Start/stop all services
│   ├── init-databases.sh           # Creates per-service databases
│   ├── init-vault-secrets.sh       # Seeds Vault with secrets
│   └── verify-infrastructure.sh    # Health checks
└── vault/                          # Vault configuration
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
```

### Session Management Service

```bash
# List sessions
curl -s http://localhost:8080/session-management-service/api/sessions \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Notification Management Service

```bash
# List notifications
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
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
