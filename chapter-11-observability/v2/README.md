# Chapter 11 v2 Observability

This implementation builds on v1's automatic observability by adding **custom business instrumentation** to the ChargeRoute platform. While v1 provided comprehensive observability through Spring Boot's automatic instrumentation, v2 demonstrates how to add domain-specific metrics, traces, and health indicators that align with business requirements.

## v2 Implementation

### 1. Ten New Observability Classes

v2 introduces custom observability components across all services (2 per service):

**Session Management Service**:
- `SessionObservabilityMetrics.java` - Tracks active sessions, session lifecycle, duration timers
- `SessionHealthIndicator.java` - Reports session service capacity and database health

**User Management Service**:
- `UserObservabilityMetrics.java` - Monitors user registrations, logins, active users
- `UserHealthIndicator.java` - Validates user service database and cache health

**Charge Unit Management Service**:
- `ChargeUnitObservabilityMetrics.java` - Measures availability, utilization, power delivery
- `ChargeUnitHealthIndicator.java` - Assesses charging infrastructure status

**Notification Management Service**:
- `NotificationObservabilityMetrics.java` - Counts notifications sent, failed, by type
- `NotificationHealthIndicator.java` - Monitors notification delivery capabilities

**API Gateway**:
- `GatewayObservabilityMetrics.java` - Tracks request routing, circuit breaker states
- `GatewayHealthIndicator.java` - Validates gateway routing and downstream service health

### 2. Modified Service Methods

All business service methods now include observability annotations:

```java
@NewSpan("session-create")
@Observed(name = "session.create", contextualName = "create-charging-session")
public Mono<ChargingSessionDTO> createSession(CreateSessionRequest request) {
    return tracer.currentSpan()
        .tag("user.id", request.getUserId().toString())
        .tag("chargeUnit.id", request.getChargeUnitId().toString())
        // ... business logic
        .doOnSuccess(session -> sessionMetrics.recordSessionStarted());
}
```

**Key Enhancements**:
- `@NewSpan` creates custom trace spans for business operations
- `@Observed` enables both metrics and traces via Micrometer Observation API
- Business context tags (`user.id`, `session.id`) added to spans for correlation
- Automatic metric recording on success/failure paths

### 3. Custom Business Metrics

Each service exposes domain-specific metrics accessible via `/actuator/prometheus`:

**Session Management Metrics**:
```
chargeroute_sessions_active_total          # Current active charging sessions
chargeroute_sessions_started_total         # Cumulative sessions started
chargeroute_sessions_completed_total       # Cumulative sessions completed
chargeroute_sessions_failed_total          # Cumulative session failures
chargeroute_sessions_duration_seconds      # Session duration histogram
```

**Charge Unit Metrics**:
```
chargeroute_chargeunits_available_total    # Available charging units
chargeroute_chargeunits_inuse_total        # Charging units currently in use
chargeroute_chargeunits_offline_total      # Offline/unavailable units
chargeroute_chargeunits_utilization_total  # Utilization percentage (0-100)
chargeroute_chargeunits_power_kw           # Total power delivery capacity
```

**User Management Metrics**:
```
chargeroute_users_active_total             # Active user sessions
chargeroute_users_registered_total         # Cumulative user registrations
chargeroute_users_logins_total             # Total login attempts
```

**Notification Metrics**:
```
chargeroute_notifications_sent_total       # Notifications successfully sent
chargeroute_notifications_failed_total     # Notification delivery failures
chargeroute_notifications_by_type          # Breakdown by type (email, SMS, push)
```

**Gateway Metrics**:
```
chargeroute_gateway_requests_routed_total  # Requests routed by service
chargeroute_gateway_requests_failed_total  # Routing failures
chargeroute_gateway_circuit_breaker_state  # Circuit breaker status (0=closed, 1=open)
```

### 4. Custom Distributed Tracing

v2 adds business-aware spans with rich contextual information:

**Custom Span Example**:
```
Session Creation Flow:
 gateway-route [span: api-gateway]
    tags: http.method=POST, uri=/api/sessions
 session-create [span: session-management-service]
    tags: user.id=123, chargeUnit.id=456, session.id=789
    validate-user [span: user-service-call]
       tags: user.id=123, validation.result=success
    check-chargeunit [span: chargeunit-service-call]
       tags: chargeUnit.id=456, availability=true
    persist-session [span: database]
        tags: db.operation=insert, db.table=charging_sessions
 notification-send [span: notification-service]
     tags: notification.type=session_started, user.id=123
```

**Business Tags Added**:
- `user.id` - User performing the operation
- `session.id` - Charging session identifier
- `chargeUnit.id` - Charge unit involved
- `notification.type` - Type of notification sent
- `validation.result` - Validation outcome
- `availability` - Resource availability status

### 5. Custom Health Indicators

Each service provides detailed health information via `/actuator/health`:

**Session Service Health Response**:
```json
{
  "status": "UP",
  "components": {
    "sessionServiceHealth": {
      "status": "UP",
      "details": {
        "database": "UP",
        "totalSessions": 1247,
        "activeSessions": 42,
        "warning": null
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

**Health Check Features**:
- Custom health status based on business rules (e.g., warn if >1000 active sessions)
- Reactive health checks with timeout protection (5 seconds)
- Detailed error reporting with error type classification
- Integration with Spring Boot Actuator health aggregation

### 6. Three New Grafana Dashboards

Located in `/grafana/dashboards/`:

**1. ChargeRoute Business Metrics Dashboard** (`chargeroute-business-metrics.json`)
- **Session Analytics**: Active sessions, start/completion rates, average duration
- **Charge Unit Status**: Availability, utilization trends, power delivery
- **User Activity**: Registrations, logins, active users over time
- **Notification Performance**: Delivery success rate, failures by type
- **Business SLA Panels**: Session success rate, average response time, availability percentage

**2. ChargeRoute Infrastructure Dashboard** (`chargeroute-infrastructure.json`)
- **Service Health**: UP/DOWN status for all services
- **JVM Metrics**: Heap usage, GC rate, thread count per service
- **Database Performance**: Connection pool usage, query latency, cache hit ratio
- **Message Broker**: RabbitMQ queue depth, consumer lag, message rates
- **Network**: HTTP request rates, error rates, latency percentiles

**3. ChargeRoute Unified Dashboard** (`chargeroute-unified.json`)
- **Top Row**: High-level business KPIs (active sessions, available units, error rate)
- **Middle Row**: Service health matrix, request flow diagram
- **Bottom Row**: Detailed metrics drill-down with variable filters
- **Service Selector**: Drop-down to filter by specific microservice
- **Time Range Controls**: Customizable observation window

**Dashboard Features**:
- Auto-refresh every 30 seconds
- Variable-based filtering (service, environment, time range)
- Panel links to drill down from overview to details
- Alert annotations overlaid on graphs
- PromQL query templates for custom panels

### 7. Five Kibana Saved Searches

Located in `/kibana/saved-searches/`:

**1. Error Logs** (`error-logs.ndjson`)
- Filters: `level:"ERROR"`
- Columns: timestamp, application, message, trace_id, stack_trace
- Use case: Identify and investigate service errors

**2. Session Lifecycle Logs** (`session-lifecycle-logs.ndjson`)
- Filters: `message:*session* AND (message:*created* OR message:*completed* OR message:*failed*)`
- Columns: timestamp, application, message, trace_id, session.id, user.id
- Use case: Track session state transitions end-to-end

**3. User Actions** (`user-actions.ndjson`)
- Filters: `application:"user-management-service" AND message:*user*`
- Columns: timestamp, message, trace_id, user.id, operation
- Use case: Audit user registrations, logins, profile changes

**4. Notification Delivery** (`notification-delivery.ndjson`)
- Filters: `application:"notification-management-service"`
- Columns: timestamp, message, trace_id, notification.type, delivery.status
- Use case: Monitor notification delivery success and failures

**5. Trace Correlation** (`trace-correlation.ndjson`)
- Filters: None (search by trace_id in query bar)
- Columns: timestamp, application, level, message, trace_id, span_id
- Use case: Follow a single request across all microservices

**Search Features**:
- Pre-configured filters and column layouts
- Saved visualizations for common queries
- Quick access to correlated logs via trace ID
- Export capability for offline analysis

### 8. Prometheus Alert Rules (30+ Rules in 4 Groups)

Located in `/prometheus/alerts.yml`:

**Group 1: Service Availability Alerts** (6 rules)
```yaml
- alert: ServiceDown
  expr: up{job=~".*-service"} == 0
  for: 1m
  severity: critical

- alert: ServiceInstanceDown
  expr: up{job=~"eureka-server|config-server|api-gateway"} == 0
  for: 30s
  severity: critical
```

**Group 2: Business Operations Alerts** (10 rules)
```yaml
- alert: HighSessionFailureRate
  expr: rate(chargeroute_sessions_failed_total[5m]) > 0.1
  for: 5m
  severity: warning

- alert: NoChargeUnitsAvailable
  expr: chargeroute_chargeunits_available_total == 0
  for: 5m
  severity: critical

- alert: HighNotificationFailureRate
  expr: (sum(rate(chargeroute_notifications_failed_total[5m])) /
         sum(rate(chargeroute_notifications_sent_total[5m]))) > 0.10
  for: 5m
  severity: warning
```

**Group 3: Performance Alerts** (4 rules)
```yaml
- alert: HighResponseTime
  expr: histogram_quantile(0.95, sum by (service, le)
        (rate(http_server_requests_seconds_bucket[5m]))) > 2.0
  for: 5m
  severity: warning

- alert: CriticalResponseTime
  expr: histogram_quantile(0.99, sum by (service, le)
        (rate(http_server_requests_seconds_bucket[5m]))) > 5.0
  for: 2m
  severity: critical
```

**Group 4: Infrastructure Alerts** (12 rules)
```yaml
- alert: PostgreSQLDown
  expr: up{job="postgres-exporter"} == 0
  for: 1m
  severity: critical

- alert: HighJVMMemoryUsage
  expr: (jvm_memory_used_bytes{area="heap"} /
         jvm_memory_max_bytes{area="heap"}) > 0.90
  for: 5m
  severity: warning

- alert: RabbitMQNoConsumers
  expr: rabbitmq_queue_consumers == 0 and rabbitmq_queue_messages > 0
  for: 5m
  severity: critical
```

**Alert Severity Levels**:
- `critical` - Immediate response required, platform functionality impaired
- `warning` - Attention needed, potential degradation or SLA risk
- `info` - Informational, unusual patterns detected

**Alert Categories**:
- `availability` - Service up/down status
- `performance` - Response time, latency, throughput
- `business` - Session failures, unit availability, user impact
- `capacity` - Resource saturation, scaling triggers
- `integration` - External system connectivity
- `resources` - Memory, CPU, disk usage

### 9. All Tests Passing

v2 maintains backward compatibility with v1 while adding new functionality:

```
Total Tests by Service:
- Eureka Server:               14 tests
- API Gateway:                  1 test
- User Management:             29 tests
- Charge Unit Management:      19 tests
- Session Management:          22 tests
- Notification Management:     30 tests
---------------------------------
Total:                        115 tests  ALL PASSING
```

**Test Coverage**:
- Unit tests for custom metrics classes
- Integration tests with Testcontainers (PostgreSQL, RabbitMQ)
- Health indicator validation
- Mapper tests for DTO conversions
- Service layer business logic tests
- Event publishing and consuming tests

## Running v2

### Prerequisites

Ensure you have:
- Docker and Docker Compose (16GB RAM recommended)
- Java 21
- Maven 3.9+
- All ports available (see Access Points section)

### Step 1: Build All Services

```bash
cd /Users/brianenochson/clcode/book/ai/sc_book/code/chapter-11-observability/v2
mvn clean install
```

**Expected output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 2-3 minutes
[INFO] Tests run: 115, Failures: 0, Errors: 0, Skipped: 0
```

### Step 2: Start Infrastructure Stack

```bash
docker compose up -d
```

**Services started** (13 containers):
- PostgreSQL (5433)
- Vault (8200) + Vault Init (auto-seeds database passwords, runs once and exits)
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

Expected: All containers show `Up` status and `healthy` health check. The `vault-init` container will show `Exited (0)` which is normal.

### Step 3: Initialize Observability

```bash
./scripts/setup-observability.sh
```

This script:
- Waits for Elasticsearch cluster to be green
- Creates Kibana index pattern `chargeroute-logs-*`
- Imports Kibana saved searches
- Validates Grafana datasource configuration
- Displays access URLs and credentials

### Step 4: Start Microservices

Use the service manager script:

```bash
cd scripts
./service-manager.sh start
```

Or start manually in dependency order:

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

**Startup Validation**:
- Each service logs `Custom metrics initialized successfully`
- Check Eureka Dashboard: http://localhost:8761 (all services registered)
- Check Prometheus Targets: http://localhost:9090/targets (all UP)

## Access Points

### Observability Tools

| Tool | URL | Credentials | Purpose |
|------|-----|-------------|---------|
| **Prometheus** | http://localhost:9090 | - | Metrics query, alerts, targets |
| **Grafana** | http://localhost:3000 | admin/admin | Business dashboards, visualization |
| **Kibana** | http://localhost:5601 | - | Log search, saved queries |
| **Zipkin** | http://localhost:9411 | - | Distributed tracing, dependency graph |

### Infrastructure Services

| Service | URL | Credentials |
|---------|-----|-------------|
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| Keycloak Admin | http://localhost:9000 | admin/admin |
| pgAdmin | http://localhost:5050 | admin@chargeroute.com/admin123 |
| Vault | http://localhost:8200 | Token: see docker-compose.yml |

### Microservices

| Service | Base URL | Actuator | Prometheus Metrics |
|---------|----------|----------|-------------------|
| Eureka Server | http://localhost:8761 | /actuator | /actuator/prometheus |
| Config Server | http://localhost:8888 | /actuator | /actuator/prometheus |
| API Gateway | http://localhost:8080 | /actuator | /actuator/prometheus |
| User Service | http://localhost:8082 | /actuator | /actuator/prometheus |
| Charge Unit Service | http://localhost:8081 | /actuator | /actuator/prometheus |
| Session Service | http://localhost:8083 | /actuator | /actuator/prometheus |
| Notification Service | http://localhost:8084 | /actuator | /actuator/prometheus |

## Demonstrating Custom Observability

All endpoints require an OAuth2 token from Keycloak:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:9000/realms/ev-charging/protocol/openid-connect/token' \
  -d 'client_id=admin-client' \
  -d 'client_secret=admin-secret' \
  -d 'grant_type=password' \
  -d 'username=admin@evcharging.com' \
  -d 'password=password' | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Generate Traffic to Populate Custom Metrics

**Create a user** (triggers `chargeroute_users_registered_total` counter and `UserRegisteredEvent`):

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

**Create a charge unit** (populates `chargeroute_chargeunits_available_total`):

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

**Query services** (generates HTTP metrics and traces):

```bash
# List users
curl -s http://localhost:8080/user-management-service/api/users \
  -H "Authorization: Bearer $TOKEN" | jq

# List charge units
curl -s http://localhost:8080/charge-unit-management-service/api/charge-units \
  -H "Authorization: Bearer $TOKEN" | jq

# List sessions
curl -s http://localhost:8080/session-management-service/api/sessions \
  -H "Authorization: Bearer $TOKEN" | jq

# List notifications (welcome notification from event)
curl -s http://localhost:8080/notification-management-service/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```

### View Custom Metrics in Prometheus

Navigate to http://localhost:9090 and run these queries:

**Active Sessions Gauge**:
```promql
chargeroute_sessions_active_total
```

**Session Start Rate (per second)**:
```promql
rate(chargeroute_sessions_started_total[5m])
```

**Session Success Rate (percentage)**:
```promql
(
  rate(chargeroute_sessions_completed_total[5m])
  /
  rate(chargeroute_sessions_started_total[5m])
) * 100
```

**Charge Unit Utilization**:
```promql
chargeroute_chargeunits_utilization_total
```

**Available vs In-Use Charge Units**:
```promql
chargeroute_chargeunits_available_total
chargeroute_chargeunits_inuse_total
```

**Notification Failure Rate**:
```promql
(
  sum(rate(chargeroute_notifications_failed_total[5m]))
  /
  (sum(rate(chargeroute_notifications_sent_total[5m])) +
   sum(rate(chargeroute_notifications_failed_total[5m])))
) * 100
```

**Average Session Duration (minutes)**:
```promql
rate(chargeroute_sessions_duration_seconds_sum[5m])
/
rate(chargeroute_sessions_duration_seconds_count[5m])
/ 60
```

### View Custom Traces in Zipkin

Navigate to http://localhost:9411:

**Find Session Creation Traces**:
1. Select service: `session-management-service`
2. Add tag filter: `http.method=POST`
3. Click "Run Query"
4. Select a trace to see the waterfall view

**Trace Analysis**:
```
Trace: POST /api/sessions
 gateway-route (25ms)
    tags: http.method=POST, uri=/api/sessions
 session-create (450ms)
    tags: user.id=123, chargeUnit.id=456
    validate-user (120ms)
       tags: user.id=123, validation.result=success
    check-chargeunit-availability (90ms)
       tags: chargeUnit.id=456, availability=true
    database-insert (180ms)
       tags: db.operation=insert, db.table=charging_sessions
    publish-event (35ms)
        tags: event.type=session_started
 notification-send (75ms)
     tags: notification.type=session_started, user.id=123
```

**Search by Business Context**:
- By user: `user.id=123`
- By session: `session.id=789`
- By charge unit: `chargeUnit.id=456`
- By duration: `minDuration=500ms` (slow requests)

**Dependency Graph**:
1. Click "Dependencies" tab
2. Select time range
3. View service interaction diagram
4. Identify bottlenecks and call patterns

### View Custom Logs in Kibana

Navigate to http://localhost:5601:

**Load Saved Searches**:
1. Go to "Discover"
2. Click "Open" in top menu
3. Select one of the five saved searches

**Search Examples**:

**All Session Operations**:
```
application:"session-management-service" AND message:*session*
```

**Failed Sessions with Trace ID**:
```
level:"ERROR" AND message:*session* AND message:*failed*
```

**User Registrations**:
```
application:"user-management-service" AND message:*registered*
```

**Notification Failures**:
```
application:"notification-management-service" AND
message:*failed* AND
notification.type:*
```

**Correlate Logs by Trace ID**:
1. Find a log entry with a trace_id
2. Copy the trace_id value
3. Search: `trace_id:"abc123def456"`
4. See all logs across all services for that request

**Create Custom Visualizations**:
1. From Discover, click "Visualize" button
2. Select visualization type (e.g., "Pie Chart")
3. Configure:
   - Slice by: `application.keyword`
   - Metric: Count
4. Save as "Logs by Service"

### View Business Dashboards in Grafana

Navigate to http://localhost:3000 (login: admin/admin):

**Import ChargeRoute Dashboards**:
1. Dashboards � Browse
2. You should see three dashboards:
   - ChargeRoute Business Metrics
   - ChargeRoute Infrastructure
   - ChargeRoute Unified

**Business Metrics Dashboard**:
- **Sessions Panel**: Line graph of active sessions over time
- **Session Rate Panel**: Start/completion rates side-by-side
- **Success Rate Panel**: Percentage of successfully completed sessions
- **Charge Unit Panel**: Stacked area chart of available/in-use/offline units
- **Utilization Panel**: Gauge showing current utilization percentage
- **User Activity Panel**: Registrations and logins per minute
- **Notification Panel**: Sent vs failed notifications

**Infrastructure Dashboard**:
- **Service Status Matrix**: Green/red indicators for all services
- **JVM Heap Usage**: Memory consumption per service
- **Database Connections**: Pool usage across all services
- **HTTP Error Rate**: 4xx and 5xx errors by service
- **Response Time P95**: 95th percentile latency

**Unified Dashboard**:
- Top row: Key business metrics (KPIs)
- Middle row: Service health and request flow
- Bottom row: Detailed metric drill-down
- Variables: Service selector, time range picker

**Create Custom Panels**:
1. Click "Add" � "Visualization"
2. Select Prometheus datasource
3. Enter query: `chargeroute_sessions_active_total`
4. Configure:
   - Panel title: "Active Charging Sessions"
   - Visualization: Time series
   - Unit: sessions
5. Save to dashboard

## Observability Correlation Workflow

This section demonstrates how to use all three pillars together to debug a real-world scenario.

### Scenario: Session Failure Investigation

**Problem**: Users report charging sessions are failing to start.

### Step 1: Identify the Issue in Grafana

1. Open "ChargeRoute Business Metrics" dashboard
2. Observe spike in **Session Failure Rate** panel at 14:23:00
3. Note: **Active Sessions** dropped from 42 to 38
4. Conclusion: Multiple sessions failed around 14:23

### Step 2: Check Metrics in Prometheus

Query to identify failure rate:
```promql
rate(chargeroute_sessions_failed_total[5m])
```

Result: 0.5 failures/second spike at 14:23:00

Query to identify affected service:
```promql
up{job="charge-unit-management-service"}
```

Result: Shows 0 (DOWN) from 14:22:30 to 14:24:15

**Root cause hypothesis**: Charge unit service was down, preventing session validation.

### Step 3: Find Related Traces in Zipkin

1. Navigate to http://localhost:9411
2. Set time range: 14:22:00 - 14:25:00
3. Select service: `session-management-service`
4. Add tag: `error=true`
5. Run Query

**Sample Failed Trace**:
```
Trace ID: a1b2c3d4e5f6
Duration: 5002ms (TIMEOUT)

 gateway-route (12ms) 
 session-create (5000ms) 
    validate-user (145ms) 
    check-chargeunit-availability (4850ms)  TIMEOUT
 (notification not sent)

Tags:
  user.id: 123
  chargeUnit.id: 456
  error: true
  error.type: ServiceUnavailableException
  error.message: "Charge unit service unavailable"
```

**Finding**: Session creation failed because charge unit validation timed out after 5 seconds.

### Step 4: Correlate Logs in Kibana

1. Copy trace ID from Zipkin: `a1b2c3d4e5f6`
2. Navigate to http://localhost:5601
3. Open "Trace Correlation" saved search
4. Search: `trace_id:"a1b2c3d4e5f6"`

**Log Timeline**:
```json
14:23:01.234 | session-management-service | INFO
"Creating session for user 123 on charge unit 456"

14:23:01.245 | session-management-service | INFO
"Calling charge-unit-service to validate availability"

14:23:06.250 | session-management-service | ERROR
"Timeout calling charge-unit-service: Read timed out after 5000ms"
"stack_trace": "java.net.SocketTimeoutException: Read timed out..."

14:23:06.255 | session-management-service | WARN
"Session creation failed. User: 123, ChargeUnit: 456"

14:23:06.260 | notification-management-service | INFO
"Skipping notification - session creation failed"
```

**Finding**: Confirms charge unit service was unreachable, causing timeout.

### Step 5: Verify Infrastructure Health

Check Charge Unit Service logs directly:
```
application:"charge-unit-management-service" AND
timestamp:[2024-11-18T14:22:00 TO 2024-11-18T14:25:00]
```

**Logs show**:
```json
14:22:28.123 | ERROR | "Database connection pool exhausted"
14:22:28.456 | ERROR | "Health check failed: Unable to acquire connection"
14:22:30.000 | WARN  | "Service marked DOWN by Eureka health check"
14:24:12.789 | INFO  | "Database connection pool recovered"
14:24:15.000 | INFO  | "Service marked UP - registered with Eureka"
```

**Root Cause Identified**: Database connection pool exhaustion in charge-unit-management-service caused service downtime from 14:22:30 to 14:24:15, resulting in session creation failures.

### Step 6: Validate Alert Fired

Check Prometheus Alerts: http://localhost:9090/alerts

**Active Alerts during incident**:
```
Alert: ServiceDown
Service: charge-unit-management-service
Fired: 14:22:30
Resolved: 14:24:15
Message: "Charge unit service has been down for more than 1 minute"

Alert: HighSessionFailureRate
Fired: 14:23:00
Resolved: 14:24:30
Message: "Session failure rate is 0.5 failures per second"
```

### Step 7: Remediation Actions

Based on findings:
1. **Immediate**: Service auto-recovered when connection pool reset
2. **Short-term**: Increase database connection pool size in config
3. **Long-term**:
   - Add connection pool metrics to infrastructure dashboard
   - Create alert for connection pool saturation (>80% usage)
   - Implement circuit breaker for charge unit validation
   - Add graceful degradation (queue sessions when service down)

### Step 8: Post-Incident Verification

After configuration changes:

**Prometheus Query** (verify pool has headroom):
```promql
hikaricp_connections_active{application="charge-unit-management-service"}
/
hikaricp_connections_max{application="charge-unit-management-service"}
```

Result: 0.42 (42% utilization - healthy)

**Custom Health Check** (verify service reports healthy):
```bash
curl http://localhost:8081/actuator/health | jq .
```

Response:
```json
{
  "status": "UP",
  "components": {
    "chargeUnitServiceHealth": {
      "status": "UP",
      "details": {
        "database": "UP",
        "connectionPool": "HEALTHY",
        "availableChargeUnits": 156
      }
    }
  }
}
```

**Generate Load Test**:
```bash
./scripts/generate-load.sh --duration 2m --rps 20
```

Monitor Business Metrics Dashboard during load test:
- Session failure rate: 0%
- Active sessions: Stable ramp-up to 65
- Charge unit utilization: 41% (healthy)
- P95 response time: 345ms (within SLA)

## Key Differences: v1 vs v2

| Feature | v1 | v2 |
|---------|----|----|
| **Custom Metrics Classes** | 0 | 10 (2 per service) |
| **Business Metrics** | 0 | 15+ custom counters/gauges |
| **Custom Spans** | 0 | All service methods |
| **Span Tags** | Auto-generated | Business context (user.id, session.id) |
| **Health Indicators** | Default only | 5 custom health checks |
| **Grafana Dashboards** | Generic templates | 3 ChargeRoute dashboards |
| **Kibana Saved Searches** | 0 | 5 domain-specific searches |
| **Prometheus Alerts** | 0 | 30+ rules in 4 groups |
| **Alert Categories** | N/A | Business, performance, infrastructure |
| **Dashboard Focus** | JVM/HTTP | Sessions, revenue, availability |
| **Observability Code** | 0 lines | ~1,000 lines |
| **Service Annotations** | None | @NewSpan, @Observed on all methods |
| **Trace Context** | HTTP headers only | Business entity IDs in tags |
| **Health Detail** | UP/DOWN | Service-specific capabilities |
| **Metric Namespacing** | Spring Boot default | `chargeroute.*` prefix |
| **Alert Severity** | N/A | critical, warning, info |
| **Correlation Workflow** | Manual | Documented scenario-based |
| **Production Readiness** | Basic stack | Business SLA monitoring |

## Troubleshooting

### Custom Metrics Not Appearing in Prometheus

**Symptom**: `chargeroute.*` metrics missing from `/actuator/prometheus`

**Check**:
```bash
# Verify service started and metrics initialized
curl http://localhost:8083/actuator/prometheus | grep chargeroute
```

**Expected output**:
```
chargeroute_sessions_active_total 0.0
chargeroute_sessions_started_total 0.0
...
```

**Fix**:
- Check service logs for "Initializing custom session metrics" message
- Verify `SessionObservabilityMetrics` bean is created
- Ensure `@Component` annotation is present on metrics class

### Custom Spans Not in Zipkin

**Symptom**: Traces exist but custom spans missing or business tags not present

**Check**:
```bash
# Verify tracing is enabled in config
curl http://localhost:8083/actuator/env | jq '.propertySources[] |
  select(.name | contains("applicationConfig")) |
  .properties |
  to_entries[] |
  select(.key | contains("tracing"))'
```

**Expected**:
```json
{
  "key": "management.tracing.sampling.probability",
  "value": "1.0"
}
```

**Fix**:
- Add `@NewSpan("span-name")` annotation to service methods
- Ensure `micrometer-tracing-bridge-brave` dependency is present
- Verify Zipkin endpoint in configuration is correct
- Check that `Tracer` bean is autowired in service class

### Grafana Dashboards Not Loading

**Symptom**: Dashboards folder empty or shows "No data"

**Check**:
```bash
# Verify Grafana provisioning
docker exec -it v2-grafana-1 ls /etc/grafana/provisioning/dashboards
```

**Expected**: Should list JSON dashboard files

**Check Prometheus datasource**:
1. Navigate to http://localhost:3000/datasources
2. Click "Prometheus" datasource
3. Click "Test" button
4. Should show "Data source is working"

**Fix**:
- Ensure `docker-compose.yml` mounts dashboard volume correctly
- Restart Grafana: `docker-compose restart grafana`
- Import dashboards manually: Dashboards � Import � Upload JSON file
- Verify Prometheus is accessible from Grafana container:
  ```bash
  docker exec -it v2-grafana-1 wget -O- http://prometheus:9090/api/v1/status/config
  ```

### Kibana Saved Searches Missing

**Symptom**: No saved searches appear in Kibana

**Check**:
```bash
# Verify index pattern exists
curl http://localhost:5601/api/saved_objects/_find?type=index-pattern
```

**Expected**: Should return `chargeroute-logs-*` pattern

**Fix**:
```bash
# Re-run setup script
./scripts/setup-observability.sh

# Or manually create index pattern
curl -X POST http://localhost:5601/api/saved_objects/index-pattern/chargeroute-logs \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{
    "attributes": {
      "title": "chargeroute-logs-*",
      "timeFieldName": "@timestamp"
    }
  }'
```

### Alerts Not Firing in Prometheus

**Symptom**: Alerts configured but never trigger

**Check alert rules loaded**:
```bash
# View Prometheus config
curl http://localhost:9090/api/v1/status/config | jq .data.yaml
```

**Expected**: Should include `rule_files: ["/etc/prometheus/alerts.yml"]`

**Check alert status**:
1. Navigate to http://localhost:9090/alerts
2. Verify alert rules are listed
3. Check "State" column (Inactive, Pending, Firing)

**Fix**:
- Ensure `alerts.yml` is mounted in docker-compose.yml
- Restart Prometheus: `docker-compose restart prometheus`
- Verify alert expression with query in Prometheus web UI first
- Check alert evaluation interval in prometheus.yml

### High Memory Usage

**Symptom**: System consuming >12GB RAM

**Check resource usage**:
```bash
docker stats --no-stream
```

**Reduce resource consumption**:

Edit `docker-compose.yml`:
```yaml
elasticsearch:
  environment:
    - ES_JAVA_OPTS=-Xms256m -Xmx256m  # Reduce from 512m

logstash:
  environment:
    - LS_JAVA_OPTS=-Xms128m -Xmx128m  # Reduce from 256m
```

Reduce trace sampling in production:
```yaml
# config-data/session-management-service.yml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% instead of 100%
```

Limit log retention:
```yaml
# logback-spring.xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
  <maxHistory>3</maxHistory>  <!-- Keep only 3 days -->
  <totalSizeCap>1GB</totalSizeCap>  <!-- Limit total size -->
</rollingPolicy>
```

## Production Considerations

### Metric Cardinality Management

**Problem**: High-cardinality tags (user IDs, session IDs) can overwhelm Prometheus.

**Solution**:
```java
// AVOID: Adding user ID as tag creates unbounded metrics
Counter.builder("chargeroute.sessions.started")
    .tag("user_id", userId.toString())  // L Bad - creates metric per user
    .register(registry);

// PREFER: Use tags with bounded cardinality
Counter.builder("chargeroute.sessions.started")
    .tag("service", "session-management")  //  Good - fixed set of values
    .tag("region", region)  //  Good - limited regions
    .register(registry);
```

### Trace Sampling Strategy

Adjust sampling based on environment:

```yaml
# Development: 100% sampling
management:
  tracing:
    sampling:
      probability: 1.0

# Staging: 25% sampling
management:
  tracing:
    sampling:
      probability: 0.25

# Production: 5-10% sampling for high-traffic services
management:
  tracing:
    sampling:
      probability: 0.05
```

Implement intelligent sampling:
```java
@Bean
public Sampler customSampler() {
    return new Sampler() {
        @Override
        public SamplingResult trySample(TraceContext parent) {
            // Always sample errors
            if (isError(parent)) return SAMPLE;

            // Always sample slow requests
            if (isSlowRequest(parent)) return SAMPLE;

            // Probabilistic sampling for normal requests
            return Math.random() < 0.1 ? SAMPLE : DROP;
        }
    };
}
```

### Log Volume Management

Implement log levels per environment:

```yaml
# Development
logging:
  level:
    com.chargeroute: DEBUG
    org.springframework: INFO

# Production
logging:
  level:
    com.chargeroute: INFO
    org.springframework: WARN
```

Use structured logging with selective fields:
```java
// Avoid logging sensitive data
log.info("User registered",
    kv("userId", userId),  //  Safe
    kv("email", maskEmail(email)),  //  Masked
    // Don't log: password, creditCard, SSN
);
```

### Dashboard Performance Optimization

Use recording rules for expensive queries:

```yaml
# prometheus.yml
groups:
  - name: chargeroute_aggregations
    interval: 30s
    rules:
      - record: chargeroute:session_success_rate:5m
        expr: |
          (
            rate(chargeroute_sessions_completed_total[5m])
            /
            rate(chargeroute_sessions_started_total[5m])
          ) * 100

      - record: chargeroute:p95_latency:5m
        expr: |
          histogram_quantile(0.95,
            sum by (service, le) (rate(http_server_requests_seconds_bucket[5m]))
          )
```

Use in dashboards:
```promql
# Instead of complex query on every dashboard refresh
chargeroute:session_success_rate:5m

# Instead of expensive quantile calculation
chargeroute:p95_latency:5m
```

### Alert Noise Reduction

Implement alert grouping and inhibition:

```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'chargeroute-team'

inhibit_rules:
  # Don't alert on high error rate if service is down
  - source_match:
      alertname: 'ServiceDown'
    target_match:
      alertname: 'HighHTTPErrorRate'
    equal: ['service']
```

### Health Check Tuning

Implement circuit breakers for health checks:

```java
@Component
public class SessionHealthIndicator implements ReactiveHealthIndicator {

    @Override
    public Mono<Health> health() {
        return checkHealth()
            .timeout(Duration.ofSeconds(5))  // Prevent slow health checks
            .onErrorResume(ex -> Mono.just(Health.down(ex).build()));
    }

    private Mono<Health> checkHealth() {
        // Use circuit breaker to prevent cascading health check failures
        return circuitBreaker.run(
            () -> sessionRepository.count()
                .map(count -> Health.up()
                    .withDetail("totalSessions", count)
                    .build()),
            ex -> Mono.just(Health.down(ex).build())
        );
    }
}
```

### Data Retention Policies

Configure retention based on data type:

```yaml
# Prometheus retention
prometheus:
  command:
    - '--storage.tsdb.retention.time=15d'  # Keep metrics for 15 days
    - '--storage.tsdb.retention.size=10GB'  # Or max 10GB

# Elasticsearch retention (ILM policy)
PUT _ilm/policy/chargeroute-logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "5GB",
            "max_age": "1d"
          }
        }
      },
      "delete": {
        "min_age": "7d",  # Delete logs older than 7 days
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

# Zipkin retention
zipkin:
  storage:
    type: elasticsearch
    elasticsearch:
      index-shards: 5
      index-replicas: 1
      max-span-age: 7  # Keep traces for 7 days
```
