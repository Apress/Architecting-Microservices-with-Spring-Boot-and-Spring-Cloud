#!/bin/bash

# This script cleans all microservices in the directory

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Services to clean
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

# Function to clean a service
clean_service() {
    local service_name=$1
    local service_dir="${SCRIPT_DIR}/../${service_name}"
    
    if [ ! -d "$service_dir" ]; then
        print_error "Service directory not found: $service_dir"
        return 1
    fi
    
    print_status "Cleaning $service_name..."
    
    cd "$service_dir"
    
    # Check if target directory exists
    if [ -d "target" ]; then
        local target_size=$(du -sh target 2>/dev/null | cut -f1 || echo "unknown")
        print_status "$service_name - Target directory size: $target_size"
    fi
    
    # Maven clean
    if mvn clean > /dev/null 2>&1; then
        print_success "$service_name - Maven clean successful"
    else
        print_error "$service_name - Maven clean failed"
        return 1
    fi
    
    # Clean additional files if they exist
    local cleaned_files=()
    
    
    # Remove Mac OS files
    if [ -f ".DS_Store" ]; then
        rm -f .DS_Store
        cleaned_files+=(".DS_Store file")
    fi
    
    # Remove log files
    if [ -f "*.log" ]; then
        rm -f *.log
        cleaned_files+=("log files")
    fi
    
    # Report additional cleaned files
    if [ ${#cleaned_files[@]} -gt 0 ]; then
        print_status "$service_name - Also removed: ${cleaned_files[*]}"
    fi
    
    cd "$SCRIPT_DIR/.."
}

# Function to clean the root directory
clean_root() {
    print_status "Cleaning root directory..."
    
    local cleaned_files=()
    
    # Work from parent directory
    cd "$SCRIPT_DIR/.."
    
    # Remove Mac OS files
    if [ -f ".DS_Store" ]; then
        rm -f .DS_Store
        cleaned_files+=(".DS_Store")
    fi
    
    # Remove any stray log files
    if ls *.log 1> /dev/null 2>&1; then
        rm -f *.log
        cleaned_files+=("log files")
    fi
    
    # Remove any temporary files
    if ls *~ 1> /dev/null 2>&1; then
        rm -f *~
        cleaned_files+=("temporary files")
    fi
    
    if [ ${#cleaned_files[@]} -gt 0 ]; then
        print_success "Root directory - Removed: ${cleaned_files[*]}"
    else
        print_success "Root directory - No additional files to clean"
    fi
}

# Main execution
main() {
    echo "========================================"
    echo " Clean Services "
    echo "========================================"
    echo ""
    
    print_status "Starting clean process for all services..."
    print_status "Clean directory: $(dirname $SCRIPT_DIR)"
    echo ""
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        echo "Please install Maven and ensure it's in your PATH"
        exit 1
    fi
    
    echo ""
    
    # Clean each service
    local failed_services=()
    local successful_services=()
    
    for service in "${SERVICES[@]}"; do
        echo "----------------------------------------"
        if clean_service "$service"; then
            successful_services+=("$service")
        else
            failed_services+=("$service")
        fi
        echo ""
    done
    
    # Clean root directory
    echo "----------------------------------------"
    clean_root
    echo ""
    
    # Summary
    echo "========================================"
    echo "             CLEAN SUMMARY              "
    echo "========================================"
    echo ""
    
    if [ ${#successful_services[@]} -gt 0 ]; then
        print_success "Successfully cleaned services (${#successful_services[@]}):"
        for service in "${successful_services[@]}"; do
            echo "  ✓ $service"
        done
        echo ""
    fi
    
    if [ ${#failed_services[@]} -gt 0 ]; then
        print_error "Failed to clean services (${#failed_services[@]}):"
        for service in "${failed_services[@]}"; do
            echo "  ✗ $service"
        done
        echo ""
        print_error "Clean completed with errors. Check the output above for details."
        exit 1
    else
        print_success "All services cleaned successfully!"
        echo ""
        print_status "Next steps:"
        echo "  1. Build all services: ./build-all.sh"
        echo "  2. Or clean and build: ./clean-all.sh && ./build-all.sh"
        echo ""
        print_status "Cleaned artifacts:"
        echo "  - Maven target directories"
        echo "  - System files (.DS_Store)"
        echo "  - Log files and temporary files"
    fi
}

# Run main function
main "$@"