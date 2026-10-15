# Chapter 6 - Configuration Management (v3): HashiCorp Vault Integration

Builds on v2 by adding HashiCorp Vault for secrets management. Eureka and Config Server credentials are stored in Vault; non-sensitive configuration remains in Config Server.

## Services

| Service | Port |
|---------|------|
| Vault | 8200 |
| Config Server | 8888 |
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Prerequisites

Vault CLI (for testing commands, not required to run the services):

```bash
brew tap hashicorp/tap
brew install hashicorp/tap/vault
```

## Running

Start Vault and initialize secrets first:

```bash
docker-compose -f vault/docker-compose.yml up -d
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'
./vault/init-vault.sh
```

Then start services in order, each in a separate terminal:

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
# Vault health
curl http://localhost:8200/v1/sys/health | jq

# Read secrets from Vault
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'
vault kv get secret/infrastructure/eureka/admin
vault kv get secret/infrastructure/config-server/auth

# Config Server health
curl -u configuser:configpass http://localhost:8888/actuator/health

# Eureka registered services
curl -u chargeroute:Admin123! http://localhost:8761/eureka/apps
curl -u chargeroute:Admin123! http://localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE

# Test services via API Gateway
curl http://localhost:8080/api/users/admin/health
curl http://localhost:8080/api/users/user123
curl http://localhost:8080/api/charge-units/admin/health
curl http://localhost:8080/api/charge-units/UNIT-001
curl http://localhost:8080/api/sessions/admin/health
curl http://localhost:8080/api/notifications/admin/health

# Rotate a secret in Vault
vault kv put secret/infrastructure/eureka/admin username=chargeroute password=NewPassword123!
```
