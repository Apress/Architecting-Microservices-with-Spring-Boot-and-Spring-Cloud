#!/bin/bash

# Service Manager Script for ChargeRoute Microservices
# Provides start, stop, status, and list functionality for individual services

set -e

# Service names array
SERVICES=(
    "charge-unit-management-service"
    "user-management-service"
    "session-management-service"
    "notification-management-service"
)

# Function to get port for service
get_service_port() {
    case "$1" in
        "charge-unit-management-service") echo "8081" ;;
        "user-management-service") echo "8082" ;;
        "session-management-service") echo "8083" ;;
        "notification-management-service") echo "8084" ;;
        *) echo "" ;;
    esac
}

# Function to get jar path for service
get_service_jar() {
    case "$1" in
        "charge-unit-management-service") echo "charge-unit-management-service/target/charge-unit-management-service-0.0.1-SNAPSHOT.jar" ;;
        "user-management-service") echo "user-management-service/target/user-management-service-0.0.1-SNAPSHOT.jar" ;;
        "session-management-service") echo "session-management-service/target/session-management-service-0.0.1-SNAPSHOT.jar" ;;
        "notification-management-service") echo "notification-management-service/target/notification-management-service-0.0.1-SNAPSHOT.jar" ;;
        *) echo "" ;;
    esac
}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Function to get PID of a service by port
get_pid_by_port() {
    local port=$1
    local pid=$(lsof -ti:$port 2>/dev/null || true)
    echo "$pid"
}

# Function to get PID of a service by name pattern
get_pid_by_name() {
    local service_name=$1
    local pid=$(ps aux | grep "java.*$service_name" | grep -v grep | awk '{print $2}' | head -1)
    echo "$pid"
}

# Function to check if service is running
is_service_running() {
    local service_name=$1
    local port=$(get_service_port "$service_name")
    
    if [ -z "$port" ]; then
        return 1
    fi
    
    local pid=$(get_pid_by_port $port)
    if [ -n "$pid" ]; then
        return 0
    fi
    return 1
}

# Function to list all services and their status
list_services() {
    echo -e "${BLUE}ChargeRoute Microservices Status:${NC}"
    echo "=================================="
    printf "%-35s %-10s %-10s %s\n" "SERVICE" "PORT" "PID" "STATUS"
    echo "----------------------------------"
    
    for service_name in "${SERVICES[@]}"; do
        local port=$(get_service_port "$service_name")
        local pid=$(get_pid_by_port $port)
        local status="STOPPED"
        local status_color=$RED
        
        if [ -n "$pid" ]; then
            status="RUNNING"
            status_color=$GREEN
        fi
        
        printf "%-35s %-10s %-10s " "$service_name" "$port" "${pid:-N/A}"
        echo -e "${status_color}$status${NC}"
    done
    echo ""
}

# Function to stop a specific service
stop_service() {
    local service_name=$1
    local port=$(get_service_port "$service_name")
    
    if [ -z "$port" ]; then
        echo -e "${RED}Error: Unknown service '$service_name'${NC}"
        echo "Available services:"
        for svc in "${SERVICES[@]}"; do
            echo "  - $svc"
        done
        return 1
    fi
    
    echo -e "${YELLOW}Stopping $service_name on port $port...${NC}"
    
    local pid=$(get_pid_by_port $port)
    if [ -n "$pid" ]; then
        kill -15 $pid 2>/dev/null || true
        
        # Wait for graceful shutdown (max 10 seconds)
        local count=0
        while [ $count -lt 10 ]; do
            sleep 1
            if ! kill -0 $pid 2>/dev/null; then
                echo -e "${GREEN}✓ $service_name stopped successfully${NC}"
                return 0
            fi
            count=$((count + 1))
        done
        
        # Force kill if still running
        echo -e "${YELLOW}Force killing $service_name...${NC}"
        kill -9 $pid 2>/dev/null || true
        sleep 1
        echo -e "${GREEN}✓ $service_name stopped (forced)${NC}"
    else
        echo -e "${YELLOW}$service_name is not running${NC}"
    fi
}

# Function to start a specific service
start_service() {
    local service_name=$1
    local jar_path=$(get_service_jar "$service_name")
    local port=$(get_service_port "$service_name")
    
    if [ -z "$jar_path" ]; then
        echo -e "${RED}Error: Unknown service '$service_name'${NC}"
        return 1
    fi
    
    if is_service_running "$service_name"; then
        echo -e "${YELLOW}$service_name is already running${NC}"
        return 0
    fi
    
    local full_jar_path="$PROJECT_ROOT/$jar_path"
    if [ ! -f "$full_jar_path" ]; then
        echo -e "${RED}Error: JAR file not found at $full_jar_path${NC}"
        echo "Please build the service first using: ./build-all.sh"
        return 1
    fi
    
    echo -e "${YELLOW}Starting $service_name on port $port...${NC}"
    
    cd "$PROJECT_ROOT"
    nohup java -jar "$full_jar_path" \
        --server.port=$port \
        > "$PROJECT_ROOT/logs/${service_name}.log" 2>&1 &
    
    local new_pid=$!
    sleep 2
    
    if kill -0 $new_pid 2>/dev/null; then
        echo -e "${GREEN}✓ $service_name started successfully (PID: $new_pid)${NC}"
    else
        echo -e "${RED}✗ Failed to start $service_name${NC}"
        return 1
    fi
}

# Function to stop all services
stop_all() {
    echo -e "${BLUE}Stopping all services...${NC}"
    for service_name in "${SERVICES[@]}"; do
        stop_service "$service_name"
    done
}

# Function to start all services
start_all() {
    echo -e "${BLUE}Starting all services...${NC}"
    
    # Create logs directory if it doesn't exist
    mkdir -p "$PROJECT_ROOT/logs"
    
    # Start services in order (dependencies first)
    for service_name in "${SERVICES[@]}"; do
        start_service "$service_name"
        sleep 2  # Give each service time to start before starting the next
    done
    
    echo ""
    echo -e "${GREEN}All services start sequence completed${NC}"
    echo ""
    
    # Show final status
    list_services
}

# Function to show service status
show_status() {
    local service_name=$1
    
    if [ -z "$service_name" ]; then
        list_services
        return
    fi
    
    local port=$(get_service_port "$service_name")
    if [ -z "$port" ]; then
        echo -e "${RED}Error: Unknown service '$service_name'${NC}"
        return 1
    fi
    
    local pid=$(get_pid_by_port $port)
    if [ -n "$pid" ]; then
        echo -e "${GREEN}$service_name is RUNNING${NC}"
        echo "  Port: $port"
        echo "  PID: $pid"
        
        # Show process details
        echo "  Process info:"
        ps -p $pid -o pid,vsz,rss,comm,args | tail -1
    else
        echo -e "${RED}$service_name is STOPPED${NC}"
        echo "  Port: $port (not listening)"
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <command> [service-name]"
    echo ""
    echo "Commands:"
    echo "  list                    - List all services with their status and PIDs"
    echo "  status [service-name]   - Show status of all services or specific service"
    echo "  start <service-name>    - Start a specific service"
    echo "  start-all               - Start all services in order"
    echo "  stop <service-name>     - Stop a specific service"
    echo "  stop-all                - Stop all running services"
    echo "  restart <service-name>  - Restart a specific service"
    echo "  restart-all             - Restart all services"
    echo ""
    echo "Available services:"
    for service_name in "${SERVICES[@]}"; do
        local port=$(get_service_port "$service_name")
        echo "  - $service_name (port $port)"
    done
    echo ""
    echo "Examples:"
    echo "  $0 list                                    # List all services"
    echo "  $0 start-all                               # Start all services"
    echo "  $0 stop user-management-service            # Stop user service"
    echo "  $0 start session-management-service        # Start session service"
    echo "  $0 restart charge-unit-management-service  # Restart charge unit service"
    echo "  $0 status notification-management-service  # Check notification service status"
    echo "  $0 stop-all                                # Stop all services"
    echo "  $0 restart-all                             # Restart all services"
}

# Create logs directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/logs"

# Main script logic
case "${1:-}" in
    list)
        list_services
        ;;
    stop)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        stop_service "$2"
        ;;
    stop-all)
        stop_all
        ;;
    start)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        start_service "$2"
        ;;
    start-all)
        start_all
        ;;
    restart)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        stop_service "$2"
        sleep 2
        start_service "$2"
        ;;
    restart-all)
        stop_all
        echo ""
        echo -e "${YELLOW}Waiting for services to fully stop...${NC}"
        sleep 3
        start_all
        ;;
    status)
        show_status "${2:-}"
        ;;
    *)
        show_usage
        exit 1
        ;;
esac