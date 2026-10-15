# Chapter 4 - Service Discovery (v2): Eureka Integration

Adds Netflix Eureka service discovery to the four ChargeRoute microservices. Services register with Eureka and use logical service names for inter-service communication instead of hardcoded URLs.

## Services

| Service | Port | Eureka Name |
|---------|------|-------------|
| Eureka Discovery Service | 8761 | - |
| Charge Unit Management | dynamic (0) | CHARGE-UNIT-SERVICE |
| User Management | dynamic (0) | USER-SERVICE |
| Session Management | dynamic (0) | CHARGING-SESSION-SERVICE |
| Notification Management | dynamic (0) | NOTIFICATION-SERVICE |

## Key Changes from v1

- Added Eureka Discovery Service (port 8761, credentials: eureka/password)
- All services register with Eureka using dynamic port assignment
- Inter-service calls use `@LoadBalanced` RestTemplate with logical names (e.g., `http://user-management-service`)
- Hardcoded localhost URLs replaced with service discovery

## Running

Start Eureka first, then the microservices:

```bash
cd eureka-discovery-service && mvn spring-boot:run
cd <service-directory> && mvn spring-boot:run
```

Eureka dashboard: http://localhost:8761
