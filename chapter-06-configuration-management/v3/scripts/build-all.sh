#!/bin/bash

echo "Building Chapter 6 v2 - Configuration Management with All Services..."
echo "===================================================================="

# Change to the parent directory
cd "$(dirname "$0")/.."

# Build Config Server (must be built first)
echo "Building Config Server..."
cd config-server
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build Config Server"
    exit 1
fi
cd ..

# Build Eureka Server
echo "Building Eureka Server..."
cd eureka-server
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build Eureka Server"
    exit 1
fi
cd ..

# Build API Gateway
echo "Building API Gateway..."
cd api-gateway
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build API Gateway"
    exit 1
fi
cd ..

# Build User Management Service
echo "Building User Management Service..."
cd user-management-service
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build User Management Service"
    exit 1
fi
cd ..

# Build Charge Unit Management Service
echo "Building Charge Unit Management Service..."
cd charge-unit-management-service
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build Charge Unit Management Service"
    exit 1
fi
cd ..

# Build Session Management Service
echo "Building Session Management Service..."
cd session-management-service
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build Session Management Service"
    exit 1
fi
cd ..

# Build Notification Management Service
echo "Building Notification Management Service..."
cd notification-management-service
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build Notification Management Service"
    exit 1
fi
cd ..

echo "===================================================================="
echo "Build complete! All services built successfully."
echo ""
echo "Next steps:"
echo "  1. Run: ./scripts/server-manager.sh start"
echo "  2. Visit http://localhost:8761 for Eureka Dashboard"
echo "  3. Visit http://localhost:8888/user-management-service/dev for Config Server"
echo "  4. Visit http://localhost:8080/actuator/gateway/routes for API Gateway routes"
echo "  5. Test services:"
echo "     - Config Server: http://localhost:8888"
echo "     - Eureka Server: http://localhost:8761"
echo "     - API Gateway: http://localhost:8080"
echo "     - All Business Services: Dynamic ports (check via Eureka)"