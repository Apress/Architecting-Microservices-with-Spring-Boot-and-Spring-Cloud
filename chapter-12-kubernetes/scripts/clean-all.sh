#!/bin/bash

# Clean all resources including images and cluster
set -e

echo "=== Cleaning ChargeRoute Resources ==="

# Delete namespace if exists
if kubectl get namespace chargeroute &> /dev/null; then
    echo "Deleting chargeroute namespace..."
    kubectl delete namespace chargeroute --wait=true
fi

# Remove Docker images
echo "Removing Docker images..."
IMAGES=("user-management-service" "charge-unit-management-service" "session-management-service" "notification-management-service")

for IMAGE in "${IMAGES[@]}"; do
    if docker images | grep -q "${IMAGE}"; then
        echo "  Removing ${IMAGE}:latest"
        docker rmi "${IMAGE}:latest" 2>/dev/null || true
    fi
done

# Ask if user wants to delete the kind cluster
read -p "Delete kind cluster 'chargeroute-cluster'? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Deleting kind cluster..."
    kind delete cluster --name chargeroute-cluster
fi

echo ""
echo "=== Cleanup Complete ==="
