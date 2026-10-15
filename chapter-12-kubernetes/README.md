# Chapter 12: Local Kubernetes Deployment

Complete Kubernetes deployment for the ChargeRoute EV charging platform microservices, demonstrating the evolution from Spring Cloud-based architecture to Kubernetes-native deployment patterns.

## Architecture Overview

Chapter 12 migrates the ChargeRoute platform from Spring Cloud infrastructure to Kubernetes-native equivalents. Several Spring Cloud components are replaced by Kubernetes primitives:

| Spring Cloud Component | Kubernetes Replacement |
|------------------------|------------------------|
| Eureka Server | Kubernetes Services + DNS |
| Config Server | ConfigMaps + Secrets |
| Spring Cloud Gateway | NGINX Ingress Controller |
| Ribbon Load Balancer | Kubernetes Service Load Balancing |

### Inside Kubernetes (chargeroute namespace)

- User Management Service (2 replicas)
- Charge Unit Management Service (2 replicas)
- Session Management Service (2 replicas)
- Notification Management Service (2 replicas)
- RabbitMQ (StatefulSet with persistent storage)

### Outside Kubernetes (Docker Compose on host)

- PostgreSQL on port 5433 (4 databases: userdb, chargeunitdb, sessiondb, notificationdb)
- Zipkin (distributed tracing)
- Prometheus + Grafana (metrics and dashboards)
- ELK Stack - Elasticsearch, Logstash, Kibana (centralized logging)
- pgAdmin (optional database management)

## Prerequisites

### Install Kind (Kubernetes in Docker)

Kind runs a Kubernetes cluster inside Docker containers - no VM or cloud account needed.

**macOS (Homebrew):**

```bash
brew install kind
```

**Linux:**

```bash
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

**Verify installation:**

```bash
kind version
```

### Install kubectl

kubectl is the command-line tool for interacting with Kubernetes clusters.

**macOS (Homebrew):**

```bash
brew install kubectl
```

**Linux:**

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/kubectl
```

**Verify installation:**

```bash
kubectl version --client
```

### Other Requirements

- Docker Desktop installed and running
- Java 21 and Maven 3.9+ (for building services)

### Test Your Setup

Before proceeding, verify everything is installed:

```bash
docker version
kind version
kubectl version --client
java -version
mvn -version
```

## Project Structure

```
chapter-12-kubernetes/
├── docker-compose.yml                # PostgreSQL, observability stack (external to K8s)
├── kind-config.yaml                  # 3-node cluster (1 control plane + 2 workers)
├── pom.xml                           # Multi-module reactor build
├── user-management-service/          # Service source + Dockerfile
├── charge-unit-management-service/   # Service source + Dockerfile
├── session-management-service/       # Service source + Dockerfile
├── notification-management-service/  # Service source + Dockerfile
├── manifests/
│   ├── namespace.yaml                # chargeroute namespace
│   ├── configmaps/                   # Application configuration (4 files)
│   ├── secrets/                      # Database and RabbitMQ credentials (2 files)
│   ├── deployments/                  # Service deployments with 2 replicas (4 files)
│   ├── services/                     # ClusterIP service definitions (5 files)
│   ├── statefulsets/                 # RabbitMQ with persistent storage (1 file)
│   └── ingress/                      # NGINX path-based routing (1 file)
└── scripts/
    ├── build-all.sh                  # Build Docker images and load into Kind
    ├── deploy-all.sh                 # Apply all manifests in order
    ├── undeploy-all.sh               # Remove all resources
    ├── dev-reload.sh                 # Rebuild and reload a single service
    ├── test-health.sh                # Health check all services
    └── clean-all.sh                  # Remove namespace, images, and cluster
```

## Quick Start

### 1. Start External Infrastructure

Start PostgreSQL, the observability stack, and supporting services:

```bash
docker compose up -d
```

This starts PostgreSQL (port 5433), Zipkin (9411), Prometheus (9090), Grafana (3000), Elasticsearch (9200), Logstash (5000), Kibana (5601), pgAdmin (5050), and the PostgreSQL exporter.

Wait for all containers to be healthy:

```bash
docker compose ps
```

### 2. Create the Kind Cluster

```bash
kind create cluster --config kind-config.yaml --name chargeroute-cluster
```

This creates a 3-node cluster (1 control plane + 2 workers) with port mappings for the Ingress controller on ports 80 and 443.

**Verify the cluster is running:**

```bash
kubectl cluster-info --context kind-chargeroute-cluster
kubectl get nodes
```

You should see three nodes (one control-plane, two workers) in Ready status.

### 3. Install the NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for the ingress controller to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

### 4. Build and Load Service Images

```bash
# Build all four service Docker images and load them into the Kind cluster
./scripts/build-all.sh
```

This builds multi-stage Docker images for each service and loads them directly into the Kind cluster (no registry needed).

Can see all of the images loaded into the cluster after Running the build with

```bash
docker exec -it chargeroute-cluster-control-plane crictl images
```
### 5. Deploy to Kubernetes

```bash
./scripts/deploy-all.sh
```

This applies manifests in dependency order: namespace, secrets, configmaps, RabbitMQ StatefulSet, services, deployments, and ingress.

### 6. Verify Deployment

```bash
# Check all resources in the chargeroute namespace
kubectl get all -n chargeroute

# Test health endpoints
./scripts/test-health.sh
```

All pods should show Running status with 1/1 ready containers.

## Accessing the Services

### Via Ingress (localhost)

With the Ingress controller and port mappings, services are available at `http://localhost/api/...`. Chapter 12 removes OAuth2 security, so no Bearer token is needed.

**Create a user:**

```bash
curl -s -X POST http://localhost/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "phoneNumber": "+1234567890",
    "password": "securepass123"
  }' | jq
```

**Create a charge unit:**

```bash
curl -s -X POST http://localhost/api/charge-units \
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

**Create a session** (use actual user and charge unit IDs from the responses above):

```bash
curl -s -X POST http://localhost/api/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_ID",
    "chargeUnitId": "CHARGE_UNIT_ID",
    "status": "ACTIVE",
    "startTime": "2026-03-24T12:00:00"
  }' | jq
```

**Query data:**

```bash
# List users
curl -s http://localhost/api/users | jq

# List charge units
curl -s http://localhost/api/charge-units | jq

# List sessions
curl -s http://localhost/api/sessions | jq

# List notifications (should show welcome notification from user creation event)
curl -s http://localhost/api/notifications | jq
```

**Delete a user:**

```bash
curl -s -X DELETE http://localhost/api/users/USER_ID
```

**Delete a charge unit:**

```bash
curl -s -X DELETE http://localhost/api/charge-units/CHARGE_UNIT_ID
```

**Delete a session:**

```bash
curl -s -X DELETE http://localhost/api/sessions/SESSION_ID
```

### Health Endpoints

```bash
curl -s http://localhost/api/users/actuator/health | jq
curl -s http://localhost/api/charge-units/actuator/health | jq
curl -s http://localhost/api/sessions/actuator/health | jq
curl -s http://localhost/api/notifications/actuator/health | jq
```

### RabbitMQ Management UI

```bash
# Port-forward to access RabbitMQ management
kubectl port-forward svc/rabbitmq 15672:15672 -n chargeroute
```

Then open http://localhost:15672 (guest/guest).

### Internal Service Communication

Inside the cluster, services communicate via Kubernetes DNS:

- `user-service.chargeroute.svc.cluster.local:8080`
- `charge-unit-service.chargeroute.svc.cluster.local:8080`
- `session-service.chargeroute.svc.cluster.local:8080`
- `notification-service.chargeroute.svc.cluster.local:8080`
- `rabbitmq.chargeroute.svc.cluster.local:5672`

## Development Workflow

### Build All Services

```bash
mvn clean install -DskipTests
```

### Rebuild a Single Service

After making code changes to one service:

```bash
./scripts/dev-reload.sh user-management-service
```

This rebuilds the Docker image, loads it into Kind, restarts the deployment, and waits for the rollout to complete. Available services: `user-management-service`, `charge-unit-management-service`, `session-management-service`, `notification-management-service`.

### View Logs

```bash
# Follow logs for a specific deployment
kubectl logs -f deployment/user-service -n chargeroute

# Follow logs for all pods of a service
kubectl logs -f -l app=user-service -n chargeroute
```

### Scaling

```bash
# Scale a deployment manually
kubectl scale deployment user-service --replicas=3 -n chargeroute

# Set up auto-scaling based on CPU
kubectl autoscale deployment user-service \
  --cpu-percent=70 \
  --min=2 --max=5 \
  -n chargeroute
```

## What Changed from Chapter 11

### Dependencies Removed

Each service had these Spring Cloud dependencies removed since Kubernetes provides the equivalent:

- `spring-cloud-starter-netflix-eureka-client` (replaced by K8s Services + DNS)
- `spring-cloud-starter-config` (replaced by ConfigMaps)
- `spring-cloud-starter-loadbalancer` (replaced by K8s Service load balancing)
- `spring-boot-starter-oauth2-resource-server` (security removed for simplicity)
- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-security`

### Classes Removed

Security and discovery classes that are no longer needed:

- SecurityConfig.java, KeycloakSecurityConfig.java (no OAuth2 in K8s)
- OAuth2ClientConfig.java, ServiceDiscoveryConfig.java (K8s handles this)
- @EnableDiscoveryClient annotations removed
- @PreAuthorize annotations removed from controllers

### Observability Retained

All Chapter 11 v2 observability features are maintained: custom business metrics, distributed tracing with Zipkin, structured JSON logging, custom health indicators, and Prometheus exporters.

## Troubleshooting

### Pods Not Starting

```bash
kubectl get pods -n chargeroute
kubectl describe pod <pod-name> -n chargeroute
kubectl logs <pod-name> -n chargeroute
kubectl logs <pod-name> -n chargeroute --previous
```

### Database Connection Issues

Services connect to PostgreSQL on the host via `host.docker.internal`. If pods can't connect:

```bash
kubectl exec <pod-name> -n chargeroute -- env | grep DATABASE
```

Verify PostgreSQL is running on the host and accessible on the configured port.

### Image Not Found

```bash
# Verify images are loaded in Kind
docker exec -it chargeroute-cluster-control-plane crictl images

# Rebuild and reload all images
./scripts/build-all.sh
```

### Ingress Not Routing

```bash
kubectl get pods -n ingress-nginx
kubectl describe ingress chargeroute-ingress -n chargeroute
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

### Service Communication Failures

```bash
# Test DNS resolution from inside a pod
kubectl exec <pod-name> -n chargeroute -- nslookup user-service

# Test service connectivity
kubectl exec <pod-name> -n chargeroute -- wget -q -O - http://user-service:8080/actuator/health
```

## Cleanup

### Remove Deployed Resources

```bash
./scripts/undeploy-all.sh
```

This deletes the `chargeroute` namespace, which cascades to all resources within it.

### Full Cleanup (Including Cluster)

```bash
./scripts/clean-all.sh
```

This removes the namespace, deletes all Docker images, and optionally deletes the Kind cluster.
