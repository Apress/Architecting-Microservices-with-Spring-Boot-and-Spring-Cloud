#!/bin/bash

echo "Stopping Chapter 6 v2 - Configuration Management Services..."
echo "============================================================"

# Function to stop service by port
stop_service_by_port() {
    local port=$1
    local service_name=$2
    
    echo "Stopping $service_name (port $port)..."
    local pid=$(lsof -ti:$port)
    if [ -n "$pid" ]; then
        kill $pid
        echo "$service_name stopped (PID: $pid)"
    else
        echo "$service_name not running on port $port"
    fi
}

# Function to stop service by process name
stop_service_by_name() {
    local service_name=$1
    local jar_pattern=$2
    
    echo "Stopping $service_name..."
    local pid=$(ps aux | grep "$jar_pattern" | grep -v grep | awk '{print $2}')
    if [ -n "$pid" ]; then
        kill $pid
        echo "$service_name stopped (PID: $pid)"
    else
        echo "$service_name not running"
    fi
}

# Stop services in reverse startup order

# Stop business services (dynamic ports)
stop_service_by_name "Notification Management Service" "notification-management-service.*\.jar"
stop_service_by_name "Session Management Service" "session-management-service.*\.jar"
stop_service_by_name "Charge Unit Management Service" "charge-unit-management-service.*\.jar"
stop_service_by_name "User Management Service" "user-management-service.*\.jar"

# Stop infrastructure services (fixed ports)
stop_service_by_port 8080 "API Gateway"
stop_service_by_port 8761 "Eureka Server"
stop_service_by_port 8888 "Config Server"

echo "============================================================"
echo "All services stopped!"