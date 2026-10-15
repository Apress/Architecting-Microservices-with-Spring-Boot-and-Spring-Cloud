#!/bin/bash

echo "Starting Chapter 6 v1 - Configuration Management Services..."
echo "============================================================"

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

# Start Config Server FIRST (Eureka Server depends on it)
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

# Start User Management Service
echo "Starting User Management Service..."
cd user-management-service
java -jar target/*.jar > ../logs/user-management-service.log 2>&1 &
USER_PID=$!
echo "User Management Service PID: $USER_PID"
cd ..

# Wait for User Service to register with Eureka (since it uses dynamic port)
echo "Waiting for User Management Service to register with Eureka..."
sleep 10
USER_PORT=""
for i in {1..15}; do
    USER_PORT=$(curl -s http://localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
    if [ ! -z "$USER_PORT" ]; then
        echo "User Management Service registered on port: $USER_PORT"
        break
    fi
    echo "Attempt $i/15: User Management Service not registered yet..."
    sleep 2
done

if [ -z "$USER_PORT" ]; then
    echo "Failed to find User Management Service in Eureka registry"
    exit 1
fi

# Test User Service health
wait_for_service "http://localhost:$USER_PORT/users/health" "User Management Service"
if [ $? -ne 0 ]; then
    echo "Failed to start User Management Service"
    exit 1
fi

echo "============================================================"
echo "All services started successfully!"
echo ""
echo "Service URLs:"
echo "  Eureka Server:               http://localhost:8761"
echo "  Config Server:               http://localhost:8888"
echo "  User Management Service:     http://localhost:$USER_PORT"
echo ""
echo "Configuration URLs:"
echo "  Default config:    http://configuser:configpass@localhost:8888/user-management-service/default"
echo "  Dev config:        http://configuser:configpass@localhost:8888/user-management-service/dev"
echo "  Prod config:       http://configuser:configpass@localhost:8888/user-management-service/prod"
echo ""
echo "Test endpoints:"
echo "  Health checks:"
echo "    curl http://localhost:$USER_PORT/users/health"
echo "  Service interactions:"
echo "    curl http://localhost:$USER_PORT/users/user123/validate"
echo "    curl http://localhost:$USER_PORT/users/config"
echo "  Configuration refresh:"
echo "    curl -X POST http://localhost:$USER_PORT/actuator/refresh"
echo ""
echo "Process IDs:"
echo "  Eureka Server: $EUREKA_PID"
echo "  Config Server: $CONFIG_PID"
echo "  User Management: $USER_PID"
echo ""
echo "To stop all services, run: ./scripts/stop-all.sh"