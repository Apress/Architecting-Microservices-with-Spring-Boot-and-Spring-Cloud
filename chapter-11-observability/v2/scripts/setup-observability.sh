#!/bin/bash

echo "=========================================="
echo "Setting up ChargeRoute Observability Stack"
echo "=========================================="

# Wait for services to be ready
echo "Waiting for Elasticsearch..."
until curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green\|yellow"'; do
  echo -n "."
  sleep 5
done
echo " Ready!"

echo "Waiting for Kibana..."
until curl -s http://localhost:5601/api/status | grep -q '"state":"green"'; do
  echo -n "."
  sleep 5
done
echo " Ready!"

echo "Creating Kibana index pattern..."
curl -X POST "http://localhost:5601/api/saved_objects/index-pattern/chargeroute-logs" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{
    "attributes": {
      "title": "chargeroute-logs-*",
      "timeFieldName": "@timestamp"
    }
  }'

echo ""
echo "=========================================="
echo "Observability Stack Setup Complete!"
echo "=========================================="
echo ""
echo "Access Points:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Kibana: http://localhost:5601"
echo "  - Zipkin: http://localhost:9411"
echo "  - RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo "  - pgAdmin: http://localhost:5050"
echo "  - Keycloak: http://localhost:9000"
echo ""
echo "Service Actuator Endpoints:"
echo "  - Config Server: http://localhost:8888/actuator"
echo "  - Eureka Server: http://localhost:8761/actuator"
echo "  - API Gateway: http://localhost:8080/actuator"
echo "  - User Service: http://localhost:8082/actuator"
echo "  - Charge Unit Service: http://localhost:8081/actuator"
echo "  - Session Service: http://localhost:8083/actuator"
echo "  - Notification Service: http://localhost:8084/actuator"
echo ""
