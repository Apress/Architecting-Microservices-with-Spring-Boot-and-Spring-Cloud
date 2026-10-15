#!/bin/bash

# Test health endpoints of all services
set -e

echo "=== Testing ChargeRoute Service Health ==="
echo ""

NAMESPACE="chargeroute"
SERVICES=("user-service" "charge-unit-service" "session-service" "notification-service")

for SERVICE in "${SERVICES[@]}"; do
    echo "Testing ${SERVICE}..."

    # Get pod name
    POD=$(kubectl get pods -n ${NAMESPACE} -l app=${SERVICE} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

    if [ -z "$POD" ]; then
        echo "  ❌ No pod found for ${SERVICE}"
        continue
    fi

    # Test liveness
    LIVENESS=$(kubectl exec -n ${NAMESPACE} ${POD} -- wget -q -O - http://localhost:8080/actuator/health/liveness 2>/dev/null || echo "failed")
    if [[ $LIVENESS == *"UP"* ]]; then
        echo "  ✅ Liveness: UP"
    else
        echo "  ❌ Liveness: DOWN"
    fi

    # Test readiness
    READINESS=$(kubectl exec -n ${NAMESPACE} ${POD} -- wget -q -O - http://localhost:8080/actuator/health/readiness 2>/dev/null || echo "failed")
    if [[ $READINESS == *"UP"* ]]; then
        echo "  ✅ Readiness: UP"
    else
        echo "  ❌ Readiness: DOWN"
    fi

    echo ""
done

echo "=== Health Check Complete ==="
echo ""
echo "For detailed health info:"
echo "  kubectl exec -n chargeroute <pod-name> -- wget -q -O - http://localhost:8080/actuator/health"
