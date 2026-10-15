# Chapter 6 - Configuration Management (v2): All Services with Git Backend

Scales configuration management to all ChargeRoute microservices with API Gateway integration and Git-backed configuration storage.

## Services

| Service | Port |
|---------|------|
| Config Server | 8888 |
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Config Repository

Git backend: `https://github.com/brianeno/sc-book-config-repo.git`

## Running

Start services in order, each in a separate terminal:

```bash
# Terminal 1 - Config Server (must start first)
cd config-server && mvn spring-boot:run

# Terminal 2 - Eureka Server (after Config Server is ready)
cd eureka-server && mvn spring-boot:run

# Terminal 3 - API Gateway (after Eureka is ready)
cd api-gateway && mvn spring-boot:run

# Terminals 4-7 - Business services (after infrastructure is ready)
cd user-management-service && mvn spring-boot:run
cd charge-unit-management-service && mvn spring-boot:run
cd session-management-service && mvn spring-boot:run
cd notification-management-service && mvn spring-boot:run
```

## Testing

```bash
# Config Server health
curl -u configuser:configpass http://localhost:8888/actuator/health

# Get configuration from Git (main and develop branches)
curl -u configuser:configpass http://localhost:8888/user-management-service/default/main
curl -u configuser:configpass http://localhost:8888/user-management-service/default/development
curl -u configuser:configpass http://localhost:8888/api-gateway/default/main
curl -u configuser:configpass http://localhost:8888/session-management-service/default/development

# Eureka registered services
curl -u chargeroute:Admin123! http://localhost:8761/eureka/apps

# Test services via API Gateway
curl http://localhost:8080/api/users/admin/health
curl http://localhost:8080/api/users/user123
curl http://localhost:8080/api/users/admin/config
curl http://localhost:8080/api/charge-units/admin/health
curl http://localhost:8080/api/charge-units/UNIT-001
curl http://localhost:8080/api/sessions/admin/health
curl http://localhost:8080/api/notifications/admin/health

# Refresh configuration after a Git change
curl -X POST -u configuser:configpass http://localhost:8888/actuator/refresh
```
