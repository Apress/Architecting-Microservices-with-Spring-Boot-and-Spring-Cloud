# Chapter 5 - API Gateway (v2): Rate Limiting

Builds on v1 by adding Redis-backed rate limiting to the API Gateway.

## Key Changes from v1

- Redis-backed distributed rate limiting
- Tiered rate limits per user level (Basic, Silver, Gold)
- API key-based rate limiting for partners
- Rate limit headers in responses

## Infrastructure

Requires Redis. Start it before the services:

```bash
docker compose up -d
```

## Services

| Service | Port |
|---------|------|
| Redis | 6379 |
| Eureka Discovery Service | 8761 |
| API Gateway | 8080 |
| Charge Unit Management | dynamic |
| User Management | dynamic |
| Session Management | dynamic |
| Notification Management | dynamic |

## Running

```bash
docker compose up -d
cd eureka-discovery-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd <service-directory> && mvn spring-boot:run
```
