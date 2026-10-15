#!/bin/bash

# Chapter 4: Service Discovery - Build All Services Script
# This script builds all microservices in the chapter-04-service-discovery directory

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Services to build
SERVICES=(
    "eureka-discovery-service"
    "user-management-service"
    "charge-unit-management-service"
    "session-management-service"
    "notification-management-service"
)

# Function to print colored output
print_status() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to build a service
build_service() {
    local service_name=$1
    local service_dir="${SCRIPT_DIR}/../${service_name}"
    
    if [ ! -d "$service_dir" ]; then
        print_error "Service directory not found: $service_dir"
        return 1
    fi
    
    if [ ! -f "$service_dir/pom.xml" ]; then
        print_error "No pom.xml found in: $service_dir"
        return 1
    fi
    
    print_status "Building $service_name..."
    
    cd "$service_dir"
    
    # Clean and compile
    if mvn clean compile > /dev/null 2>&1; then
        print_success "$service_name - Compilation successful"
    else
        print_error "$service_name - Compilation failed"
        return 1
    fi
    
    # Run tests
    if mvn test > /dev/null 2>&1; then
        print_success "$service_name - Tests passed"
    else
        print_warning "$service_name - Tests failed or no tests found"
    fi
    
    # Package
    if mvn package -DskipTests > /dev/null 2>&1; then
        print_success "$service_name - Packaging successful"
        
        # Check if JAR was created
        if [ -f "target/${service_name}-0.0.1-SNAPSHOT.jar" ]; then
            local jar_size=$(du -h "target/${service_name}-0.0.1-SNAPSHOT.jar" | cut -f1)
            print_success "$service_name - JAR created (${jar_size})"
        fi
    else
        print_error "$service_name - Packaging failed"
        return 1
    fi
    
    cd "$SCRIPT_DIR/.."
}

# Main execution
main() {
    echo "========================================"
    echo "  Chapter 4: Service Discovery Build   "
    echo "========================================"
    echo ""
    
    print_status "Starting build process for all services..."
    print_status "Build directory: $(dirname $SCRIPT_DIR)"
    echo ""
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        echo "Please install Maven and ensure it's in your PATH"
        exit 1
    fi
    
    # Check Java version
    if command -v java &> /dev/null; then
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_version" -ge 17 ]; then
            print_status "Java version: $(java -version 2>&1 | head -n 1)"
        else
            print_warning "Java 17+ recommended. Current version: $(java -version 2>&1 | head -n 1)"
        fi
    else
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    echo ""
    
    # Build each service
    local failed_services=()
    local successful_services=()
    
    for service in "${SERVICES[@]}"; do
        echo "----------------------------------------"
        if build_service "$service"; then
            successful_services+=("$service")
        else
            failed_services+=("$service")
        fi
        echo ""
    done
    
    # Summary
    echo "========================================"
    echo "             BUILD SUMMARY              "
    echo "========================================"
    echo ""
    
    if [ ${#successful_services[@]} -gt 0 ]; then
        print_success "Successfully built services (${#successful_services[@]}):"
        for service in "${successful_services[@]}"; do
            echo "  ✓ $service"
        done
        echo ""
    fi
    
    if [ ${#failed_services[@]} -gt 0 ]; then
        print_error "Failed to build services (${#failed_services[@]}):"
        for service in "${failed_services[@]}"; do
            echo "  ✗ $service"
        done
        echo ""
        print_error "Build completed with errors. Check the output above for details."
        exit 1
    else
        print_success "All services built successfully!"
        echo ""
        print_status "Next steps:"
        echo "  1. Start services using: ./scripts/service-manager.sh start-all"
        echo "  2. Or run individual services with: cd <service-name> && mvn spring-boot:run"
        echo "  3. Check service health at http://localhost:<port>/actuator/health"
        echo ""
        print_status "Service configuration:"
        echo "  - Eureka Discovery Service: 8761 (fixed port)"
        echo "  - Microservices: Dynamic ports (registered with Eureka)"
        echo ""
        print_status "Access points:"
        echo "  - Eureka Dashboard: http://localhost:8761/"
        echo "  - Service health checks: http://localhost:<dynamic-port>/actuator/health"
    fi
}

# Run main function
main "$@"