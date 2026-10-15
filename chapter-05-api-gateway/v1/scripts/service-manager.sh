#!/bin/bash

# ChargeRoute Chapter 5 Service Manager
# Manages API Gateway along with Chapter 4 microservices for complete system testing

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHAPTER5_DIR="$(dirname "$SCRIPT_DIR")"
CHAPTER4_DIR="$(dirname "$(dirname "$CHAPTER5_DIR")")/chapter-04-service-discovery/v2"
PID_DIR="$SCRIPT_DIR/pids"

# Ensure PID directory exists
mkdir -p "$PID_DIR"

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

# Service ports function (macOS compatible)
get_service_port() {
    local service_name="$1"
    case "$service_name" in
        "$EUREKA_SERVICE")
            echo "8761"
            ;;
        "$USER_SERVICE"|"$CHARGE_UNIT_SERVICE"|"$SESSION_SERVICE"|"$NOTIFICATION_SERVICE")
            echo "0"  # Dynamic port
            ;;
        "$GATEWAY_SERVICE")
            echo "8080"
            ;;
        *)
            echo "0"
            ;;
    esac
}

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

# Check if service is running
is_service_running() {
    local service_name="$1"
    local pid_file="$PID_DIR/${service_name}.pid"
    
    if [[ -f "$pid_file" ]]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        else
            # PID file exists but process is dead
            rm -f "$pid_file"
            return 1
        fi
    fi
    return 1
}

# Get next available management port
get_next_management_port() {
    local start_port=8090
    local port=$start_port
    
    while netstat -an 2>/dev/null | grep -q ":$port "; do
        ((port++))
        if [ $port -gt 8999 ]; then
            error "No available management ports in range 8090-8999"
            exit 1
        fi
    done
    
    echo $port
}

# Start a service
start_service() {
    local service_name="$1"
    local service_dir="$(get_service_dir "$service_name")"
    local pid_file="$PID_DIR/${service_name}.pid"
    
    if [[ ! -d "$service_dir" ]]; then
        error "Service directory not found: $service_dir"
        return 1
    fi
    
    if is_service_running "$service_name"; then
        warning "$service_name is already running"
        return 0
    fi
    
    log "Starting $service_name..."
    
    cd "$service_dir"
    
    # Get management port
    local mgmt_port=$(get_next_management_port)
    
    # Start service in background
    if [[ "$service_name" == "$GATEWAY_SERVICE" ]]; then
        # API Gateway uses fixed port 8080
        nohup mvn spring-boot:run \
            -Dspring-boot.run.jvmArguments="-Dmanagement.server.port=$mgmt_port" \
            > "$PID_DIR/${service_name}.log" 2>&1 &
    else
        # Other services use dynamic ports
        nohup mvn spring-boot:run \
            -Dspring-boot.run.jvmArguments="-Dserver.port=0 -Dmanagement.server.port=$mgmt_port" \
            > "$PID_DIR/${service_name}.log" 2>&1 &
    fi
    
    local pid=$!
    echo "$pid" > "$pid_file"
    
    # Wait a moment for startup
    sleep 3
    
    if ps -p "$pid" > /dev/null 2>&1; then
        success "$service_name started with PID $pid (management port: $mgmt_port)"
        return 0
    else
        error "Failed to start $service_name"
        rm -f "$pid_file"
        return 1
    fi
}

# Stop a service  
stop_service() {
    local service_name="$1"
    local pid_file="$PID_DIR/${service_name}.pid"
    
    if [[ ! -f "$pid_file" ]]; then
        warning "$service_name is not running (no PID file)"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    
    if ! ps -p "$pid" > /dev/null 2>&1; then
        warning "$service_name PID $pid not found (already stopped)"
        rm -f "$pid_file"
        return 0
    fi
    
    log "Stopping $service_name (PID: $pid)..."
    kill "$pid"
    
    # Wait for graceful shutdown
    local count=0
    while ps -p "$pid" > /dev/null 2>&1 && [ $count -lt 30 ]; do
        sleep 1
        ((count++))
    done
    
    if ps -p "$pid" > /dev/null 2>&1; then
        warning "Forcefully killing $service_name..."
        kill -9 "$pid"
        sleep 2
    fi
    
    rm -f "$pid_file"
    success "$service_name stopped"
}

# Get service status from Eureka
get_eureka_status() {
    local service_name="$1"
    local eureka_name=$(echo "$service_name" | tr '[:lower:]' '[:upper:]' | tr '-' '-')
    
    if ! is_service_running "$EUREKA_SERVICE"; then
        echo "EUREKA-DOWN"
        return
    fi
    
    # Try to get status from Eureka API
    local status=$(curl -s --connect-timeout 2 \
        -u "chargeroute:Admin123!" \
        "http://localhost:8761/eureka/apps/$eureka_name" 2>/dev/null | \
        grep -o '<status>[^<]*</status>' | sed 's/<[^>]*>//g' | head -1)
    
    if [[ -n "$status" ]]; then
        echo "REG-$status"
    else
        echo "UNREG"
    fi
}

# List all services with status
list_services() {
    echo -e "\n${BLUE}=== ChargeRoute Chapter 5 Services Status ===${NC}\n"
    printf "%-35s %-10s %-15s %-10s\n" "SERVICE" "STATUS" "EUREKA" "PID"
    printf "%-35s %-10s %-15s %-10s\n" "-------" "------" "------" "---"
    
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    
    for service in "${services[@]}"; do
        local pid_file="$PID_DIR/${service}.pid"
        local status="STOPPED"
        local pid="N/A"
        local eureka_status="N/A"
        
        if [[ -f "$pid_file" ]]; then
            pid=$(cat "$pid_file" 2>/dev/null || echo "N/A")
            if ps -p "$pid" > /dev/null 2>&1; then
                status="${GREEN}RUNNING${NC}"
                if [[ "$service" != "$EUREKA_SERVICE" ]]; then
                    eureka_status=$(get_eureka_status "$service")
                    if [[ "$eureka_status" == "REG-UP" ]]; then
                        eureka_status="${GREEN}REG-UP${NC}"
                    elif [[ "$eureka_status" == "UNREG" ]]; then
                        eureka_status="${YELLOW}UNREG${NC}"
                    elif [[ "$eureka_status" == "EUREKA-DOWN" ]]; then
                        eureka_status="${RED}EUREKA-DOWN${NC}"
                    else
                        eureka_status="${YELLOW}$eureka_status${NC}"
                    fi
                else
                    eureka_status="${BLUE}SERVER${NC}"
                fi
            else
                status="${RED}DEAD${NC}"
                eureka_status="${RED}DEAD${NC}"
                rm -f "$pid_file"
                pid="N/A"
            fi
        else
            status="${RED}STOPPED${NC}"
            eureka_status="${RED}STOPPED${NC}"
        fi
        
        printf "%-35s %-20s %-25s %-10s\n" "$service" "$status" "$eureka_status" "$pid"
    done
    
    echo -e "\n${BLUE}Gateway URL:${NC} http://localhost:8080"
    echo -e "${BLUE}Eureka Dashboard:${NC} http://localhost:8761 (chargeroute/Admin123!)"
    echo ""
}

# Start all services in correct order
start_all() {
    log "Starting all ChargeRoute services..."
    
    # Start Eureka first
    start_service "$EUREKA_SERVICE"
    
    # Wait for Eureka to be ready
    log "Waiting for Eureka to be ready..."
    sleep 10
    
    # Start microservices
    start_service "$USER_SERVICE"
    start_service "$CHARGE_UNIT_SERVICE" 
    start_service "$SESSION_SERVICE"
    start_service "$NOTIFICATION_SERVICE"
    
    # Wait for services to register
    log "Waiting for services to register with Eureka..."
    sleep 15
    
    # Start API Gateway last
    start_service "$GATEWAY_SERVICE"
    
    # Final wait for gateway startup
    sleep 10
    
    success "All services started!"
    list_services
}

# Stop all services
stop_all() {
    log "Stopping all services..."
    
    local services=("$GATEWAY_SERVICE" "$NOTIFICATION_SERVICE" "$SESSION_SERVICE" "$CHARGE_UNIT_SERVICE" "$USER_SERVICE" "$EUREKA_SERVICE")
    
    for service in "${services[@]}"; do
        stop_service "$service"
    done
    
    success "All services stopped!"
}

# Restart a service
restart_service() {
    local service_name="$1"
    stop_service "$service_name"
    sleep 2
    start_service "$service_name"
}

# Show usage
show_help() {
    echo "ChargeRoute Chapter 5 Service Manager"
    echo ""
    echo "Usage: $0 {command} [service-name]"
    echo ""
    echo "Commands:"
    echo "  list                          List all services and their status"
    echo "  start {service-name}          Start a specific service"
    echo "  stop {service-name}           Stop a specific service" 
    echo "  restart {service-name}        Restart a specific service"
    echo "  start-all                     Start all services in correct order"
    echo "  stop-all                      Stop all services"
    echo "  logs {service-name}           Show logs for a service"
    echo "  test-gateway                  Test gateway routing"
    echo "  clean                         Clean all projects (./clean-all.sh)"
    echo "  build                         Build all projects (./build-all.sh)"
    echo ""
    echo "Services:"
    echo "  eureka-discovery-service      Service registry (port 8761)"
    echo "  user-management-service       User management (dynamic port)"
    echo "  charge-unit-management-service Charge unit management (dynamic port)"
    echo "  session-management-service    Session management (dynamic port)"
    echo "  notification-management-service Notification management (dynamic port)"
    echo "  api-gateway                   API Gateway (port 8080)"
    echo ""
    echo "Examples:"
    echo "  $0 start-all"
    echo "  $0 list"
    echo "  $0 restart api-gateway"
    echo "  $0 test-gateway"
}

# Show logs for a service
show_logs() {
    local service_name="$1"
    local log_file="$PID_DIR/${service_name}.log"
    
    if [[ -f "$log_file" ]]; then
        tail -f "$log_file"
    else
        error "Log file not found for $service_name"
        return 1
    fi
}

# Test gateway routing
test_gateway() {
    log "Testing API Gateway routing..."
    
    if ! is_service_running "$GATEWAY_SERVICE"; then
        error "API Gateway is not running"
        return 1
    fi
    
    echo -e "\n${BLUE}Testing gateway endpoints:${NC}"
    
    # Test gateway health
    echo -e "\n1. Gateway Health:"
    curl -s "http://localhost:8080/actuator/health" | jq . 2>/dev/null || curl -s "http://localhost:8080/actuator/health"
    
    # Test user service through gateway
    echo -e "\n2. User Service (through gateway):"
    curl -s "http://localhost:8080/api/users/USER-123" | jq . 2>/dev/null || curl -s "http://localhost:8080/api/users/USER-123"
    
    # Test charge unit service through gateway
    echo -e "\n3. Charge Unit Service (through gateway):" 
    curl -s "http://localhost:8080/api/charge-units/UNIT-123" | jq . 2>/dev/null || curl -s "http://localhost:8080/api/charge-units/UNIT-123"
    
    echo -e "\n${GREEN}Gateway testing complete!${NC}"
}

# Main command processing
case "${1:-help}" in
    list)
        list_services
        ;;
    start)
        if [[ -z "$2" ]]; then
            error "Please specify a service name"
            show_help
            exit 1
        fi
        start_service "$2"
        ;;
    stop)
        if [[ -z "$2" ]]; then
            error "Please specify a service name"
            show_help
            exit 1
        fi
        stop_service "$2"
        ;;
    restart)
        if [[ -z "$2" ]]; then
            error "Please specify a service name"
            show_help
            exit 1
        fi
        restart_service "$2"
        ;;
    start-all)
        start_all
        ;;
    stop-all)
        stop_all
        ;;
    logs)
        if [[ -z "$2" ]]; then
            error "Please specify a service name"
            show_help
            exit 1
        fi
        show_logs "$2"
        ;;
    test-gateway)
        test_gateway
        ;;
    clean)
        log "Running clean-all.sh..."
        "$SCRIPT_DIR/clean-all.sh" "${@:2}"
        ;;
    build)
        log "Running build-all.sh..."
        "$SCRIPT_DIR/build-all.sh" "${@:2}"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac