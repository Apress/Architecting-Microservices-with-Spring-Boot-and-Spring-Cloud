# Chapter 6 - Configuration Management (v1): Spring Cloud Config Basics

Introduces centralized configuration management with Spring Cloud Config Server using file-based native storage. Only includes Config Server, Eureka, and User Management Service.

## Services

| Service | Port |
|---------|------|
| Config Server | 8888 |
| Eureka Server | 8761 |
| User Management | dynamic |

## Running

Start services in order, each in a separate terminal:

```bash
# Terminal 1 - Config Server (must start first)
cd config-server && mvn spring-boot:run

# Terminal 2 - Eureka Server (after Config Server is ready)
cd eureka-server && mvn spring-boot:run

# Terminal 3 - User Management Service (after both are ready)
cd user-management-service && mvn spring-boot:run
```

## Testing

```bash
# Verify Config Server is serving configuration
curl -u configuser:configpass http://localhost:8888/user-management-service/default
curl -u configuser:configpass http://localhost:8888/user-management-service/dev
curl -u configuser:configpass http://localhost:8888/user-management-service/prod

# Config Server health
curl -u configuser:configpass http://localhost:8888/actuator/health

# Find User Management dynamic port from Eureka
USER_PORT=$(curl -s http://localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
echo "User Management Service port: $USER_PORT"

# Test User Management Service
curl http://localhost:$USER_PORT/users/health
curl http://localhost:$USER_PORT/users/config
curl http://localhost:$USER_PORT/users/user123
curl http://localhost:$USER_PORT/users/user123/validate
```
