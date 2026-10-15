#!/bin/bash

# Rebuild and reload a specific service for development
set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <service-name>"
    echo ""
    echo "Available services:"
    echo "  user-management-service"
    echo "  charge-unit-management-service"
    echo "  session-management-service"
    echo "  notification-management-service"
    exit 1
fi

SERVICE=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Validate service name
if [ ! -d "${PROJECT_ROOT}/${SERVICE}" ]; then
    echo "Error: Service '${SERVICE}' not found"
    exit 1
fi

# Extract deployment name (remove -management-service suffix)
DEPLOYMENT_NAME=$(echo "${SERVICE}" | sed 's/-management-service$//' | sed 's/-$//')-service

echo "=== Reloading ${SERVICE} ==="

# Build Docker image
echo "Building ${SERVICE}..."
docker build -t "${SERVICE}:latest" "${PROJECT_ROOT}/${SERVICE}"

# Load into kind cluster
echo "Loading ${SERVICE} into kind cluster..."
kind load docker-image "${SERVICE}:latest" --name chargeroute-cluster

# Restart deployment
echo "Restarting deployment..."
kubectl rollout restart deployment/${DEPLOYMENT_NAME} -n chargeroute

# Wait for rollout
echo "Waiting for rollout to complete..."
kubectl rollout status deployment/${DEPLOYMENT_NAME} -n chargeroute

echo ""
echo "=== ${SERVICE} reloaded successfully ==="
echo ""
echo "View logs:"
echo "  kubectl logs -f deployment/${DEPLOYMENT_NAME} -n chargeroute"
