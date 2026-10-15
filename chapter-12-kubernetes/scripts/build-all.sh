#!/bin/bash

# Build and load all services into kind cluster
set -e

echo "=== Building ChargeRoute Microservices ==="

SERVICES=("user-management-service" "charge-unit-management-service" "session-management-service" "notification-management-service")
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

for SERVICE in "${SERVICES[@]}"; do
    echo ""
    echo "Building ${SERVICE}..."

    # Build Docker image
    docker build -t "${SERVICE}:latest" "${PROJECT_ROOT}/${SERVICE}"

    # Load into kind cluster
    echo "Loading ${SERVICE} into kind cluster..."
    kind load docker-image "${SERVICE}:latest" --name chargeroute-cluster

    echo "${SERVICE} built and loaded successfully!"
done

echo ""
echo "=== All services built and loaded ==="
echo ""
echo "Available images in kind cluster:"
docker exec -it chargeroute-cluster-control-plane crictl images | grep -E "(user-service|charge-unit-service|session-service|notification-service)"
