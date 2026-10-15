# Chapter 5 - API Gateway (v1): Spring Cloud Gateway

Adds an API Gateway as the single entry point to the ChargeRoute microservices. All services from Chapter 4 v2 are included.

## Services

| Service | Port |
|---------|------|
| Eureka Discovery Service | 8761 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Gateway Routes

All client requests go through `http://localhost:8080`:

- `/api/users/**` - User Management Service
- `/api/charge-units/**` - Charge Unit Management Service
- `/api/sessions/**` - Session Management Service
- `/api/notifications/**` - Notification Management Service

## Running

Start Eureka first, then the gateway and microservices:

```bash
cd eureka-discovery-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd <service-directory> && mvn spring-boot:run
```
