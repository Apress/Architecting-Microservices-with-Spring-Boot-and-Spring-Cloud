#!/bin/bash

echo "Building Chapter 6 v1 - Configuration Management..."
echo "=================================================="

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

# Build User Management Service
echo "Building User Management Service..."
cd user-management-service
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Failed to build User Management Service"
    exit 1
fi
cd ..

echo "=================================================="
echo "Build complete! All services built successfully."
echo ""
echo "Next steps:"
echo "  1. Run: ./scripts/service-manager.sh start"
echo "  2. Visit http://localhost:8761 for Eureka Dashboard"
echo "  3. Visit http://localhost:8888/user-management-service/dev for Config Server"
echo "  4. Test services:"
echo "     - Config Server: http://localhost:8888"
echo "     - Eureka Server: http://localhost:8761"
echo "     - User Management: Dynamic port (check via Eureka)"