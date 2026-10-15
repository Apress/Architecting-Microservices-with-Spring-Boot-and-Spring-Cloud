#!/bin/bash

# ChargeRoute Chapter 5 Clean All Services
# Cleans all Maven projects for fresh builds

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHAPTER5_DIR="$(dirname "$SCRIPT_DIR")"
CHAPTER4_DIR="$(dirname "$(dirname "$CHAPTER5_DIR")")/chapter-04-service-discovery/v2"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log() {
    echo -e "${CYAN}[$(date +'%H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Service definitions
EUREKA_SERVICE="eureka-discovery-service"
USER_SERVICE="user-management-service" 
CHARGE_UNIT_SERVICE="charge-unit-management-service"
SESSION_SERVICE="session-management-service"
NOTIFICATION_SERVICE="notification-management-service"
GATEWAY_SERVICE="api-gateway"

# Service directories function (macOS compatible) - All services now in Chapter 5
get_service_dir() {
    local service_name="$1"
    case "$service_name" in
        "$EUREKA_SERVICE"|"$USER_SERVICE"|"$CHARGE_UNIT_SERVICE"|"$SESSION_SERVICE"|"$NOTIFICATION_SERVICE"|"$GATEWAY_SERVICE")
            echo "$CHAPTER5_DIR/$service_name"
            ;;
        *)
            echo ""
            ;;
    esac
}

# Clean a service
clean_service() {
    local service_name="$1"
    local service_dir="$(get_service_dir "$service_name")"
    
    if [[ ! -d "$service_dir" ]]; then
        error "Service directory not found: $service_dir"
        return 1
    fi
    
    log "Cleaning $service_name..."
    
    cd "$service_dir"
    
    # Clean Maven project
    if ./mvnw clean > /dev/null 2>&1; then
        success "✓ $service_name cleaned successfully"
    else
        error "✗ Failed to clean $service_name"
        return 1
    fi
}

# Clean all services
clean_all_services() {
    echo -e "\n${BLUE}=== ChargeRoute Chapter 5 - Clean All Services ===${NC}\n"
    
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    local failed_services=()
    
    for service in "${services[@]}"; do
        if ! clean_service "$service"; then
            failed_services+=("$service")
        fi
    done
    
    echo ""
    
    if [[ ${#failed_services[@]} -eq 0 ]]; then
        success "All services cleaned successfully!"
        echo -e "\n${GREEN}Ready for fresh builds with build-all.sh${NC}"
    else
        error "Some services failed to clean:"
        for service in "${failed_services[@]}"; do
            echo -e "  ${RED}✗${NC} $service"
        done
        echo -e "\n${YELLOW}Fix the errors above and retry${NC}"
        exit 1
    fi
}

# Clean target directories manually
clean_target_directories() {
    log "Cleaning target directories manually..."
    
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    
    for service in "${services[@]}"; do
        local service_dir="$(get_service_dir "$service")"
        local target_dir="$service_dir/target"
        
        if [[ -d "$target_dir" ]]; then
            log "Removing $target_dir"
            rm -rf "$target_dir"
        fi
    done
    
    success "Target directories cleaned"
}

# Clean PID files and logs
clean_runtime_files() {
    log "Cleaning runtime files (PID and LOG)..."
    
    local pid_dir="$SCRIPT_DIR/pids"
    local log_dir="$SCRIPT_DIR/logs"
    
    if [[ -d "$pid_dir" ]]; then
        log "Cleaning PID files..."
        rm -f "$pid_dir"/*.pid
        success "PID files cleaned"
    else
        log "No PID files to clean"
    fi

    if [[ -d "$log_dir" ]]; then
        log "Cleaning log files..."
        rm -f "$log_dir"/*.log
        success "LOG files cleaned"
    else
        log "No LOG files to clean"
    fi
}

# Show usage
show_help() {
    echo "ChargeRoute Chapter 5 Clean All Services"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --maven-only     Clean only Maven projects (default)"
    echo "  --target-only    Clean only target directories"
    echo "  --runtime-only   Clean only PID files and logs"
    echo "  --deep           Clean everything (Maven + target + runtime)"
    echo "  --help, -h       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                   # Clean all Maven projects"
    echo "  $0 --deep           # Full clean"
    echo "  $0 --runtime-only   # Clean only runtime files"
}

# Main execution
main() {
    case "${1:-maven}" in
        --maven-only|maven)
            clean_all_services
            ;;
        --target-only)
            clean_target_directories
            ;;
        --runtime-only)
            clean_runtime_files
            ;;
        --deep)
            clean_all_services
            clean_target_directories
            clean_runtime_files
            echo -e "\n${GREEN}Deep clean completed!${NC}"
            ;;
        --help|-h)
            show_help
            ;;
        *)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"