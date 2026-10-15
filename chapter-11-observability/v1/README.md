# Chapter 11 v1 Observability Stack

This implementation demonstrates the three pillars of observability (logs, metrics, traces) for the ChargeRoute platform using industry-standard tools integrated with Spring Boot's unified observability model.

## Overview

Chapter 11 v1 adds comprehensive observability to the event-driven microservices from Chapter 10 using automatic instrumentation with **zero custom business code**. All observability is achieved through configuration and Spring Boot's built-in capabilities.

### Three Pillars Implemented

**1. Logs - Structured JSON Logging with Kibana**
- JSON-formatted logs with Logback
- Automatic trace ID and span ID inclusion
- Centralized log aggregation with ELK stack
- Log correlation across services

**2. Metrics - Prometheus and Grafana**
- Automatic HTTP, JVM, and database metrics
- Prometheus scraping from all services
- Grafana dashboards for visualization
- PostgreSQL exporter for database metrics

**3. Traces - Distributed Tracing with Zipkin**
- Automatic span creation for HTTP requests
- Database query spans (R2DBC)
- RabbitMQ messaging spans
- End-to-end request flow visualization

## Key Technologies

- **Spring Boot**: 3.5.6
- **Micrometer**: Metrics facade with Prometheus registry
- **Micrometer Tracing**: Replaces Spring Cloud Sleuth (deprecated)
- **Zipkin**: 2.24 (distributed tracing backend)
- **Prometheus**: 2.48 (metrics storage)
- **Grafana**: 10.2 (unified dashboards)
- **ELK Stack**: Elasticsearch 8.11, Logstash 8.11, Kibana 8.11
- **OpenTelemetry**: Vendor-neutral observability support

## Architecture

```
ChargeRoute Services (Host)          Observability Stack (Docker)
├── Config Server (8888)             ├── Prometheus (9090)
├── Eureka Server (8761)             ├── Zipkin (9411)
├── API Gateway (8080)               ├── Elasticsearch (9200)
├── User Service (8082) ─────────────┼── Logstash (5000)
├── Charge Unit Service (8081)       ├── Kibana (5601)
├── Session Service (8083)           └── Grafana (3000)
└── Notification Service (8084)
```

## What's New in v1

### Dependencies Added
All microservices now include:
- `micrometer-registry-prometheus` - Metrics export
- `micrometer-tracing-bridge-brave` - Distributed tracing
- `zipkin-reporter-brave` - Zipkin integration
- `logstash-logback-encoder` - Structured logging
- `context-propagation` - Trace context propagation

### Configuration Added
- **Actuator endpoints** exposed (`/actuator/prometheus`, `/actuator/health`)
- **Zipkin tracing** configured (100% sampling for development)
- **Structured logging** with JSON format and automatic trace IDs
- **Management properties** for metrics tags and health probes

### Infrastructure Added
- **Prometheus** for metrics scraping and storage
- **Zipkin** for trace collection and visualization
- **Elasticsearch** for log storage
- **Logstash** for log processing pipeline
- **Kibana** for log visualization
- **Grafana** for unified metric dashboards
- **PostgreSQL Exporter** for database metrics

### No Functional Changes
✅ Business logic unchanged
✅ API contracts unchanged
✅ Database schemas unchanged
✅ Event flows unchanged

## Running the Application

### Prerequisites

Ensure you have:
- Docker and Docker Compose
- Java 21
- Maven 3.9+
- 16GB RAM recommended

### Step 1: Build All Services

```bash
cd v1
mvn clean install
```

**Expected output**: BUILD SUCCESS with all tests passing (30 tests)

### Step 2: Start Infrastructure

```bash
docker compose up -d
```

**Services started**:
- PostgreSQL (5433)
- Redis (6379)
- Vault (8200) + Vault Init (auto-seeds database passwords)
- Keycloak (9000)
- RabbitMQ (5672, 15672)
- pgAdmin (5050)
- Prometheus (9090)
- Zipkin (9411)
- Elasticsearch (9200)
- Logstash (5000)
- Kibana (5601)
- Grafana (3000)
- PostgreSQL Exporter (9187)

Verify all containers are healthy:
```bash
docker compose ps
```

### Step 3: Initialize Observability

```bash
./scripts/setup-observability.sh
```

This script:
- Waits for Elasticsearch and Kibana to be ready
- Creates Kibana index pattern for logs
- Displays access URLs

### Step 4: Start Microservices

Use the service manager script:

```bash
cd scripts
./service-manager.sh start
```

Or start manually in order:

```bash
# Terminal 1: Config Server (must start first)
cd config-server && mvn spring-boot:run

# Terminal 2: Eureka Server (wait for Config Server on port 8888)
cd eureka-server && mvn spring-boot:run

# Terminal 3: API Gateway (wait for Eureka on port 8761)
cd api-gateway && mvn spring-boot:run

# Terminals 4-7: Business services (can start in parallel after Eureka is ready)
cd user-management-service && mvn spring-boot:run
cd charge-unit-management-service && mvn spring-boot:run
cd session-management-service && mvn spring-boot:run
cd notification-management-service && mvn spring-boot:run
```

## Access Points

### Observability Tools
| Tool | URL | Credentials | Purpose |
|------|-----|-------------|---------|
| Prometheus | http://localhost:9090 | - | Metrics query and exploration |
| Grafana | http://localhost:3000 | admin/admin | Unified dashboards |
| Kibana | http://localhost:5601 | - | Log visualization |
| Zipkin | http://localhost:9411 | - | Distributed tracing |

### Infrastructure
| Service | URL | Credentials |
|---------|-----|-------------|
| RabbitMQ | http://localhost:15672 | guest/guest |
| Keycloak | http://localhost:9000 | admin/admin |
| pgAdmin | http://localhost:5050 | admin@chargeroute.com/admin123 |

### Microservices
| Service | URL |
|---------|-----|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| Config Server | http://localhost:8888/actuator |
| User Service | http://localhost:8082/actuator |
| Charge Unit Service | http://localhost:8081/actuator |
| Session Service | http://localhost:8083/actuator |
| Notification Service | http://localhost:8084/actuator |

## Verifying Observability

### 1. Check Metrics in Prometheus

Navigate to http://localhost:9090

> **Note:** Unlike previous chapters where business services used dynamic ports (`server.port: 0`), Chapter 11 assigns fixed ports so that Prometheus can scrape metrics from each service. The API Gateway and Eureka continue to work exactly the same - routing still goes through Eureka service discovery, not hardcoded ports.

**Verify targets are UP**:
- Status → Targets
- All ChargeRoute services should show "UP"

**Run test queries**:
```
# Total HTTP requests
http_server_requests_seconds_count

# Request rate (last 5 minutes)
rate(http_server_requests_seconds_count[5m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Database connections
hikaricp_connections_active
```

### 2. View Traces in Zipkin

Navigate to http://localhost:9411

**Find traces**:
1. Click "Run Query" (shows recent traces)
2. Select a trace to see waterfall diagram
3. Observe spans across services (Gateway → Session → User → ChargeUnit)

**Search traces**:
- By service name: `serviceName=session-management-service`
- By duration: `minDuration=100ms`
- By tags: `http.method=POST`

### 3. View Logs in Kibana

Navigate to http://localhost:5601

**Discover logs**:
1. Go to "Discover"
2. Select index pattern: `chargeroute-logs-*`
3. Search queries:
   ```
   application:"session-management-service"
   level:"ERROR"
   trace_id:"abc123def456"
   message:*session*
   ```

**Correlate logs with traces**:
1. Find a log entry
2. Copy the `trace_id` field
3. Search in Zipkin for that trace ID
4. See the complete request flow

### 4. View Dashboards in Grafana

Navigate to http://localhost:3000 (login: admin/admin)

**Import pre-built dashboards**:
1. Dashboards → Import
2. Import by ID:
   - **4701**: JVM Micrometer
   - **11378**: Spring Boot Dashboard
3. Select Prometheus as data source

**Explore metrics**:
- JVM memory, GC, threads
- HTTP request rates and latencies
- Database connection pool usage
- Service health status

## Demonstrating Observability

All endpoints require an OAuth2 token from Keycloak:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=admin-client' \
  -d 'client_secret=admin-secret' \
  -d 'grant_type=password' \
  -d 'username=admin@evcharging.com' \
  -d 'password=password' | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Demo 1: Generate Traffic and Observe All Three Pillars

**Step 1: Create a user (generates metrics, traces, and logs)**

```bash
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
```

**Step 2: Create a charge unit**

```bash
curl -s -X POST http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stationName": "Downtown Station A1",
    "address": "123 Main St, Downtown",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "connectorType": "CCS",
    "maxPowerKw": 150,
    "pricePerKwh": 0.35
  }' | jq
```

**Step 3: Query some data (generates more traces)**

```bash
# List all users
curl -s http://localhost:8080/user-management-service/api/users \
  -H "Authorization: Bearer $TOKEN" | jq

# List all charge units
curl -s http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" | jq

# List sessions
curl -s http://localhost:8080/session-management-service/api/sessions \
  -H "Authorization: Bearer $TOKEN" | jq

# List notifications (should show welcome notification from event)
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Demo 2: Verify Metrics in Prometheus

Navigate to http://localhost:9090 and verify targets are UP under Status - Targets.

Run these queries to see the traffic you just generated:

```promql
# Total HTTP requests across all services
http_server_requests_seconds_count

# Request rate for the last 5 minutes
rate(http_server_requests_seconds_count[5m])

# P95 response latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap memory usage per service
jvm_memory_used_bytes{area="heap"}
```

You can also check metrics directly from a service's actuator endpoint:

```bash
curl -s http://localhost:8080/user-management-service/actuator/prometheus \
  -H "Authorization: Bearer $TOKEN" | head -30
```

### Demo 3: View Distributed Traces in Zipkin

Navigate to http://localhost:9411:

1. Click "Run Query" to see recent traces
2. Select a trace from the user creation request
3. View the waterfall diagram showing spans across services: Gateway - User Service - Database - RabbitMQ
4. Click individual spans to see timing and metadata

Search for specific traces:
- By service: select `user-management-service` from the dropdown
- By duration: set `minDuration=100ms` to find slow requests

### Demo 4: Search Logs in Kibana

Navigate to http://localhost:5601:

1. Go to "Discover"
2. Select the `chargeroute-logs-*` index pattern
3. Search for your requests:

```
application:"user-management-service"
level:"ERROR"
message:*session*
```

**Correlate logs with traces**: Find a log entry, copy the `trace_id` field, then search for that trace ID in Zipkin to see the complete request flow across all services.

### Demo 5: View Dashboards in Grafana

Navigate to http://localhost:3000 (admin/admin):

1. Go to Dashboards - Import
2. Import by ID: **4701** (JVM Micrometer) or **11378** (Spring Boot Dashboard)
3. Select Prometheus as the data source
4. View JVM memory, GC, threads, HTTP request rates, and database connection pool usage

### Demo 6: End-to-End Correlation Across All Three Pillars

This is the key value of observability - correlating metrics, traces, and logs for a single request:

1. Make a request (e.g., create a user with the curl command above)
2. In Grafana, observe the metric spike in request count
3. In Zipkin, find the trace for that request and copy the trace ID
4. In Kibana, search for `trace_id:"<your-trace-id>"` to see all log entries across every service that participated in handling that request
5. The same trace ID links all three pillars together

## What Gets Instrumented Automatically

v1 achieves comprehensive observability with zero custom business code - everything is automatic through Spring Boot configuration and dependencies.

**Metrics** (Micrometer + Prometheus): HTTP request counts/durations/percentiles, JVM heap/GC/threads/CPU, database connection pools, RabbitMQ message rates, actuator health indicators.

**Traces** (Micrometer Tracing + Zipkin): HTTP client and server spans, R2DBC database query spans, RabbitMQ producer/consumer spans, WebClient inter-service calls, automatic context propagation via HTTP headers.

**Logs** (Logback + Logstash Encoder): Structured JSON format, automatic trace ID and span ID fields, centralized aggregation via ELK stack, cross-service correlation by trace ID.

## Configuration Files

### Observability Dependencies (pom.xml)
```xml
<!-- Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>

<!-- Structured Logging -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Context Propagation -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>context-propagation</artifactId>
</dependency>
```

### Actuator Configuration (application.yml)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,env
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: development
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### Structured Logging (logback-spring.xml)
All services include JSON-formatted logging with automatic trace context.

## Troubleshooting

### Services not showing in Prometheus

**Check**:
```bash
# Verify actuator endpoint works
curl http://localhost:8082/actuator/prometheus

# Check Prometheus targets
# Navigate to http://localhost:9090/targets
```

**Fix**: Ensure service is running on its expected port and actuator is configured correctly

### Traces not in Zipkin

**Check**:
```bash
# Verify Zipkin is running
curl http://localhost:9411/health

# Check service logs for Zipkin connection errors
```

**Fix**: Verify `management.zipkin.tracing.endpoint` in config

### Logs not in Kibana

**Check**:
```bash
# Verify log files are created
ls -la logs/

# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices

# Check Logstash is running
docker-compose logs logstash
```

**Fix**: Ensure Logstash pipeline is correctly configured

### High Memory Usage

**Reduce resource usage**:
```yaml
# In docker-compose.yml
elasticsearch:
  environment:
    - ES_JAVA_OPTS=-Xms256m -Xmx256m  # Reduce from 512m

logstash:
  environment:
    - LS_JAVA_OPTS=-Xmx128m -Xms128m  # Reduce from 256m
```

## Useful Commands

### Docker Management
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f elasticsearch

# Restart a service
docker-compose restart prometheus

# Check health
docker-compose ps
```

### Prometheus Queries
```
# Service health
up{job="user-management-service"}

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Database performance
hikaricp_connections_acquire_seconds_sum / hikaricp_connections_acquire_seconds_count
```

### Kibana Queries
```
# All errors
level:"ERROR"

# Specific service
application:"session-management-service"

# By trace ID
trace_id:"abc123"

# Combined query
application:"user-management-service" AND level:"ERROR" AND message:*database*
```
