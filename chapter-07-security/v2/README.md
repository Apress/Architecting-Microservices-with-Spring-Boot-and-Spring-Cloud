# Chapter 7 - Security (v2): Keycloak + Vault

Replaces the custom Spring Authorization Server with Keycloak for production-ready OAuth2/OIDC. OAuth2 client secrets and Eureka credentials are stored in Vault.

## Services

| Service | Port |
|---------|------|
| Vault | 8200 |
| PostgreSQL (Keycloak) | 5433 |
| Keycloak | 9000 |
| Redis | 6379 |
| Config Server | 8888 |
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Keycloak Users

| Username | Password | Role |
|----------|----------|------|
| driver@evcharging.com | password | DRIVER |
| operator@evcharging.com | password | OPERATOR |
| admin@evcharging.com | password | ADMIN |

## Running

Start infrastructure containers:

```bash
# Start Vault
cd vault && docker-compose up -d && cd ..
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'
./vault/init-vault.sh

# Start Keycloak + PostgreSQL + Redis (realm auto-imports on first start)
cd keycloak && docker-compose up -d && cd ..
```

Then start services in order, each in a separate terminal:

```bash
# Terminal 1 - Config Server
cd config-server && mvn spring-boot:run

# Terminal 2 - Eureka Server
cd eureka-server && mvn spring-boot:run

# Terminal 3 - API Gateway
cd api-gateway && mvn spring-boot:run

# Terminals 4-7 - Business services
cd user-management-service && mvn spring-boot:run
cd charge-unit-management-service && mvn spring-boot:run
cd session-management-service && mvn spring-boot:run
cd notification-management-service && mvn spring-boot:run
```

## Testing

```bash
# Get a token via client credentials
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=session-service-client' \
  -d 'client_secret=session-secret' \
  -d 'grant_type=client_credentials' | jq -r '.access_token')

# Get a user token via password grant (testing only)
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=admin-client' \
  -d 'client_secret=admin-secret' \
  -d 'grant_type=password' \
  -d 'username=driver@evcharging.com' \
  -d 'password=password' | jq -r '.access_token')

# Call a protected endpoint through the gateway
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/user123

# Inspect the JWT
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# Keycloak Admin Console
# http://localhost:9000/admin (admin/admin, select ev-charging realm)

# Vault secrets
curl -H "X-Vault-Token: root-token" http://localhost:8200/v1/secret/data/ev-charging/session-management-service | jq

# Health checks
curl http://localhost:8080/actuator/health
curl -u configuser:configpass http://localhost:8888/actuator/health
curl -u chargeroute:Admin123! http://localhost:8761/eureka/apps
```
