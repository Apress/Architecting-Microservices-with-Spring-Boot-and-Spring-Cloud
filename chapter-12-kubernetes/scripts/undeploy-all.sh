#!/bin/bash

# Undeploy all ChargeRoute services from Kubernetes
set -e

echo "=== Undeploying ChargeRoute from Kubernetes ==="

# Delete namespace (this will cascade delete everything)
echo "Deleting chargeroute namespace..."
kubectl delete namespace chargeroute --wait=true

echo ""
echo "=== Undeploy Complete ==="
echo ""
echo "Verify deletion:"
echo "  kubectl get all -n chargeroute"
