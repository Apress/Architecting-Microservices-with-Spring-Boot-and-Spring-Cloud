#!/bin/bash

echo "Starting Chapter 6 v2 - Configuration Management with All Services..."
echo "====================================================================="

# Change to the parent directory
cd "$(dirname "$0")/.."

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $service_name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "$service_name is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "ERROR: $service_name failed to start after $max_attempts attempts"
    return 1
}

# Start Config Server FIRST (all other services depend on it)
echo "Starting Config Server..."
cd config-server
java -jar target/*.jar > ../logs/config-server.log 2>&1 &
CONFIG_PID=$!
echo "Config Server PID: $CONFIG_PID"
cd ..

# Wait for Config Server to be ready
wait_for_service "http://configuser:configpass@localhost:8888/actuator/health" "Config Server"
if [ $? -ne 0 ]; then
    echo "Failed to start Config Server"
    exit 1
fi

# Start Eureka Server (after Config Server is ready)
echo "Starting Eureka Server..."
cd eureka-server
java -jar target/*.jar > ../logs/eureka-server.log 2>&1 &
EUREKA_PID=$!
echo "Eureka Server PID: $EUREKA_PID"
cd ..

# Wait for Eureka to be ready
wait_for_service "http://localhost:8761/actuator/health" "Eureka Server"
if [ $? -ne 0 ]; then
    echo "Failed to start Eureka Server"
    exit 1
fi

# Start API Gateway (after Eureka is ready)
echo "Starting API Gateway..."
cd api-gateway
java -jar target/*.jar > ../logs/api-gateway.log 2>&1 &
GATEWAY_PID=$!
echo "API Gateway PID: $GATEWAY_PID"
cd ..

# Wait for API Gateway to be ready
wait_for_service "http://localhost:8080/actuator/health" "API Gateway"
if [ $? -ne 0 ]; then
    echo "Failed to start API Gateway"
    exit 1
fi

# Start User Management Service
echo "Starting User Management Service..."
cd user-management-service
java -jar target/*.jar > ../logs/user-management-service.log 2>&1 &
USER_PID=$!
echo "User Management Service PID: $USER_PID"
cd ..

# Start Charge Unit Management Service
echo "Starting Charge Unit Management Service..."
cd charge-unit-management-service
java -jar target/*.jar > ../logs/charge-unit-management-service.log 2>&1 &
CHARGE_UNIT_PID=$!
echo "Charge Unit Management Service PID: $CHARGE_UNIT_PID"
cd ..

# Start Session Management Service
echo "Starting Session Management Service..."
cd session-management-service
java -jar target/*.jar > ../logs/session-management-service.log 2>&1 &
SESSION_PID=$!
echo "Session Management Service PID: $SESSION_PID"
cd ..

# Start Notification Management Service
echo "Starting Notification Management Service..."
cd notification-management-service
java -jar target/*.jar > ../logs/notification-management-service.log 2>&1 &
NOTIFICATION_PID=$!
echo "Notification Management Service PID: $NOTIFICATION_PID"
cd ..

# Wait for all business services to register with Eureka
echo "Waiting for business services to register with Eureka..."
sleep 15

echo "====================================================================="
echo "All services started successfully!"
echo ""
echo "Service URLs:"
echo "  Config Server:              http://localhost:8888"
echo "  Eureka Server:              http://localhost:8761"
echo "  API Gateway:                http://localhost:8080"
echo "  Business Services:          Dynamic ports (check via Eureka)"
echo ""
echo "Configuration URLs:"
echo "  Default config:    http://configuser:configpass@localhost:8888/user-management-service/default"
echo "  Dev config:        http://configuser:configpass@localhost:8888/user-management-service/dev"
echo "  Prod config:       http://configuser:configpass@localhost:8888/user-management-service/prod"
echo ""
echo "API Gateway Routes:"
echo "  User Management:    http://localhost:8080/api/users/**"
echo "  Charge Units:       http://localhost:8080/api/charge-units/**"
echo "  Sessions:           http://localhost:8080/api/sessions/**"
echo "  Notifications:      http://localhost:8080/api/notifications/**"
echo ""
echo "Test endpoints:"
echo "  API Gateway health: http://localhost:8080/actuator/health"
echo "  Gateway routes:     http://localhost:8080/actuator/gateway/routes"
echo "  User validation:    http://localhost:8080/api/users/user123/validate"
echo "  Charge unit info:   http://localhost:8080/api/charge-units/unit456/availability"
echo ""
echo "Process IDs:"
echo "  Config Server: $CONFIG_PID"
echo "  Eureka Server: $EUREKA_PID"
echo "  API Gateway: $GATEWAY_PID"
echo "  User Management: $USER_PID"
echo "  Charge Unit Management: $CHARGE_UNIT_PID"
echo "  Session Management: $SESSION_PID"
echo "  Notification Management: $NOTIFICATION_PID"
echo ""
echo "To stop all services, run: ./scripts/stop-all.sh"