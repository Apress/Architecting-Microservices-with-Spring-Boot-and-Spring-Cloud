#!/bin/bash

# Deploy all ChargeRoute services to Kubernetes
set -e

echo "=== Deploying ChargeRoute to Kubernetes ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFESTS_DIR="$(dirname "$SCRIPT_DIR")/manifests"

# Create namespace
echo "Creating namespace..."
kubectl apply -f "${MANIFESTS_DIR}/namespace.yaml"

# Apply secrets
echo "Applying secrets..."
kubectl apply -f "${MANIFESTS_DIR}/secrets/"

# Apply configmaps
echo "Applying configmaps..."
kubectl apply -f "${MANIFESTS_DIR}/configmaps/"

# Deploy RabbitMQ StatefulSet
echo "Deploying RabbitMQ..."
kubectl apply -f "${MANIFESTS_DIR}/statefulsets/"

# Wait for RabbitMQ to be ready
echo "Waiting for RabbitMQ to be ready..."
kubectl wait --for=condition=ready pod -l app=rabbitmq -n chargeroute --timeout=120s

# Apply services
echo "Creating services..."
kubectl apply -f "${MANIFESTS_DIR}/services/"

# Deploy application services
echo "Deploying application services..."
kubectl apply -f "${MANIFESTS_DIR}/deployments/"

# Wait for deployments to be ready
echo "Waiting for deployments to be ready..."
kubectl wait --for=condition=available deployment --all -n chargeroute --timeout=180s

# Apply ingress
echo "Creating ingress..."
kubectl apply -f "${MANIFESTS_DIR}/ingress/"

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Check deployment status:"
echo "  kubectl get all -n chargeroute"
echo ""
echo "View logs:"
echo "  kubectl logs -f deployment/user-service -n chargeroute"
echo ""
echo "Access services via Ingress:"
echo "  curl http://localhost/api/users"
echo "  curl http://localhost/api/charge-units"
echo "  curl http://localhost/api/sessions"
echo "  curl http://localhost/api/notifications"
