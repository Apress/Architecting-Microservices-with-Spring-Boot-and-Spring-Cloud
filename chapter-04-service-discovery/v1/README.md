# Chapter 4 - Service Discovery (v1): Skeleton Services

Four standalone Spring Boot microservices for the ChargeRoute EV charging platform, running on fixed ports without service discovery.

## Services

| Service | Port | Eureka Name |
|---------|------|-------------|
| Charge Unit Management | 8081 | CHARGE-UNIT-SERVICE |
| User Management | 8082 | USER-SERVICE |
| Session Management | 8083 | CHARGING-SESSION-SERVICE |
| Notification Management | 8084 | NOTIFICATION-SERVICE |

## Running

```bash
cd <service-directory>
mvn spring-boot:run
```

Each service uses in-memory storage and exposes RESTful APIs with Spring Boot Actuator health endpoints.
