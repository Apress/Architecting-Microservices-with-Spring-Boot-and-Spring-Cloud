#!/bin/bash

echo "Stopping Chapter 6 v1 - Configuration Management Services..."
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

# Stop services in reverse startup order
echo "Stopping User Management Service (dynamic port)..."
USER_PID=$(ps aux | grep 'user-management-service.*\.jar' | grep -v grep | awk '{print $2}')
if [ -n "$USER_PID" ]; then
    kill $USER_PID
    echo "User Management Service stopped (PID: $USER_PID)"
else
    echo "User Management Service not running"
fi

stop_service_by_port 8761 "Eureka Server"
stop_service_by_port 8888 "Config Server"

echo "============================================================"
echo "All services stopped!"