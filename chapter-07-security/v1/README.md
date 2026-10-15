# Chapter 7 - Security (v1): Spring Authorization Server

Adds OAuth2/OIDC security using a custom Spring Authorization Server. All services require JWT tokens for protected endpoints.

## Services

| Service | Port |
|---------|------|
| Vault | 8200 |
| Config Server | 8888 |
| Eureka Server | 8761 |
| Authorization Server | 9000 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Running

Start Vault and initialize secrets:

```bash
cd vault
docker-compose up -d
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'
./init-vault.sh
cd ..
```

Then start services in order, each in a separate terminal:

```bash
# Terminal 1 - Config Server
cd config-server && mvn spring-boot:run

# Terminal 2 - Eureka Server
cd eureka-server && mvn spring-boot:run

# Terminal 3 - Authorization Server
cd authorization-server && mvn spring-boot:run

# Terminal 4 - API Gateway
cd api-gateway && mvn spring-boot:run

# Terminals 5-8 - Business services
cd user-management-service && mvn spring-boot:run
cd charge-unit-management-service && mvn spring-boot:run
cd session-management-service && mvn spring-boot:run
cd notification-management-service && mvn spring-boot:run
```

## Testing

```bash
# Get a token via client credentials
TOKEN=$(curl -s -X POST 'http://localhost:9000/oauth2/token' \
  -u 'session-service-client:session-secret' \
  -d 'grant_type=client_credentials' | jq -r '.access_token')

# Call a protected endpoint through the gateway
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/user123

# Inspect the JWT
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# Health checks
curl http://localhost:8080/actuator/health
curl -u configuser:configpass http://localhost:8888/actuator/health
```
