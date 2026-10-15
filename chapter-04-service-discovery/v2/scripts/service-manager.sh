#!/bin/bash

# Service Manager Script for ChargeRoute Microservices - V2 (Eureka-enabled)
# Supports dynamic ports, multiple instances, and Eureka service discovery

set -e

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Eureka configuration
EUREKA_URL="http://localhost:8761"
EUREKA_AUTH="chargeroute:Admin123!"
EUREKA_APPS_URL="${EUREKA_URL}/eureka/apps"

# Service names
SERVICES=(
    "eureka-discovery-service"
    "charge-unit-management-service"
    "user-management-service"
    "session-management-service"
    "notification-management-service"
)

# Fixed ports (only for Eureka server)
EUREKA_PORT=8761

# Management port base (for Actuator endpoints)
MGMT_PORT_BASE=9080

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to get service jar path
get_service_jar() {
    local service_name=$1
    echo "${service_name}/target/${service_name}-0.0.1-SNAPSHOT.jar"
}

# Function to check if Eureka is running
is_eureka_running() {
    curl -sf -u "$EUREKA_AUTH" "${EUREKA_URL}/actuator/health" >/dev/null 2>&1
}

# Function to query Eureka for registered services
query_eureka() {
    if ! is_eureka_running; then
        return 1
    fi
    
    curl -sf -u "$EUREKA_AUTH" -H "Accept: application/json" "${EUREKA_APPS_URL}" 2>/dev/null
}

# Function to get instances from Eureka for a specific service
get_eureka_instances() {
    local service_name=$1
    local app_name=$(echo "$service_name" | tr '[:lower:]' '[:upper:]')
    
    if ! is_eureka_running; then
        return 1
    fi
    
    curl -sf -u "$EUREKA_AUTH" -H "Accept: application/json" \
        "${EUREKA_APPS_URL}/${app_name}" 2>/dev/null | \
        grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4
}

# Function to get port for an instance from Eureka
get_instance_port_from_eureka() {
    local service_name=$1
    local instance_id=$2
    local app_name=$(echo "$service_name" | tr '[:lower:]' '[:upper:]')
    
    curl -sf -u "$EUREKA_AUTH" -H "Accept: application/json" \
        "${EUREKA_APPS_URL}/${app_name}" 2>/dev/null | \
        grep -B5 -A5 "\"instanceId\":\"$instance_id\"" | \
        grep '"port"' | head -1 | grep -o '[0-9]*'
}

# Function to check if a port is in use
is_port_in_use() {
    local port=$1
    lsof -ti:$port >/dev/null 2>&1
}

# Function to get PID by port
get_pid_by_port() {
    local port=$1
    lsof -ti:$port 2>/dev/null || true
}

# Function to get next available instance number
get_next_instance_number() {
    local service_name=$1
    local max_num=0
    
    for pid_file in "$PROJECT_ROOT/pids/${service_name}"*.pid; do
        if [ -f "$pid_file" ]; then
            local filename=$(basename "$pid_file")
            local num=$(echo "$filename" | sed -n 's/.*-instance-\([0-9]*\)\.pid/\1/p')
            if [ -n "$num" ] && [ "$num" -gt "$max_num" ]; then
                max_num=$num
            fi
        fi
    done
    
    echo $((max_num + 1))
}

# Function to get next available management port
get_next_management_port() {
    local port=$MGMT_PORT_BASE
    while [ $port -lt $((MGMT_PORT_BASE + 100)) ]; do
        if ! is_port_in_use $port; then
            echo $port
            return 0
        fi
        port=$((port + 1))
    done
    echo -e "${RED}Error: No available management ports${NC}" >&2
    return 1
}

# Function to start Eureka server
start_eureka() {
    echo -e "${YELLOW}Starting Eureka Discovery Service...${NC}"
    
    local jar_path="$PROJECT_ROOT/$(get_service_jar eureka-discovery-service)"
    if [ ! -f "$jar_path" ]; then
        echo -e "${RED}Error: Eureka JAR not found at $jar_path${NC}"
        echo "Please build the service first using: ../scripts/build-all.sh"
        return 1
    fi
    
    if is_port_in_use $EUREKA_PORT; then
        echo -e "${GREEN}Eureka is already running on port $EUREKA_PORT${NC}"
        return 0
    fi
    
    cd "$PROJECT_ROOT"
    nohup java -jar "$jar_path" \
        --server.port=$EUREKA_PORT \
        > "$PROJECT_ROOT/logs/eureka-discovery-service.log" 2>&1 &
    
    local pid=$!
    echo "$pid:eureka-discovery-service:eureka-primary:$EUREKA_PORT" > "$PROJECT_ROOT/pids/eureka-discovery-service.pid"
    
    # Wait for Eureka to be ready
    echo -n "Waiting for Eureka to start"
    local count=0
    while [ $count -lt 30 ]; do
        if is_eureka_running; then
            echo -e "\n${GREEN}✓ Eureka started successfully (PID: $pid)${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        count=$((count + 1))
    done
    
    echo -e "\n${RED}✗ Failed to start Eureka${NC}"
    return 1
}

# Function to start a microservice with instance support
start_service() {
    local service_name=$1
    local instance_num=${2:-}
    
    if [ "$service_name" == "eureka-discovery-service" ]; then
        start_eureka
        return $?
    fi
    
    # Auto-assign instance number if not provided
    if [ -z "$instance_num" ]; then
        instance_num=$(get_next_instance_number "$service_name")
    fi
    
    local instance_id="${service_name}-instance-${instance_num}"
    local mgmt_port=$(get_next_management_port)
    
    if [ -z "$mgmt_port" ]; then
        echo -e "${RED}Error: Could not find available management port${NC}"
        return 1
    fi
    
    echo -e "${YELLOW}Starting $service_name (Instance #$instance_num)...${NC}"
    
    local jar_path="$PROJECT_ROOT/$(get_service_jar "$service_name")"
    if [ ! -f "$jar_path" ]; then
        echo -e "${RED}Error: JAR not found at $jar_path${NC}"
        return 1
    fi
    
    # Check if this instance is already running
    if [ -f "$PROJECT_ROOT/pids/${instance_id}.pid" ]; then
        local old_pid=$(cut -d: -f1 "$PROJECT_ROOT/pids/${instance_id}.pid")
        if kill -0 "$old_pid" 2>/dev/null; then
            echo -e "${YELLOW}Instance $instance_num of $service_name is already running (PID: $old_pid)${NC}"
            return 0
        fi
    fi
    
    cd "$PROJECT_ROOT"
    nohup java -jar "$jar_path" \
        --server.port=0 \
        --management.server.port=$mgmt_port \
        --spring.application.instance_id="$instance_id" \
        --eureka.instance.instance-id="$instance_id" \
        > "$PROJECT_ROOT/logs/${instance_id}.log" 2>&1 &
    
    local pid=$!
    sleep 3
    
    if kill -0 $pid 2>/dev/null; then
        # Extract actual port from log file
        local actual_port=$(grep -o 'Tomcat started on port(s): [0-9]*' "$PROJECT_ROOT/logs/${instance_id}.log" 2>/dev/null | grep -o '[0-9]*' | head -1)
        
        if [ -z "$actual_port" ]; then
            actual_port="dynamic"
        fi
        
        echo "$pid:$service_name:$instance_id:$actual_port:$mgmt_port" > "$PROJECT_ROOT/pids/${instance_id}.pid"
        echo -e "${GREEN}✓ $service_name instance #$instance_num started${NC}"
        echo -e "  PID: $pid"
        echo -e "  Instance ID: $instance_id"
        echo -e "  Service Port: $actual_port"
        echo -e "  Management Port: $mgmt_port"
    else
        echo -e "${RED}✗ Failed to start $service_name instance #$instance_num${NC}"
        return 1
    fi
}

# Function to stop a service instance
stop_service() {
    local service_name=$1
    local instance_num=${2:-}
    
    if [ -z "$instance_num" ]; then
        # Stop all instances of the service
        echo -e "${YELLOW}Stopping all instances of $service_name...${NC}"
        local found=false
        
        for pid_file in "$PROJECT_ROOT/pids/${service_name}"*.pid; do
            if [ -f "$pid_file" ]; then
                found=true
                local pid=$(cut -d: -f1 "$pid_file")
                local instance_id=$(cut -d: -f3 "$pid_file")
                
                if kill -15 "$pid" 2>/dev/null; then
                    echo -e "${GREEN}✓ Stopped $instance_id (PID: $pid)${NC}"
                    rm -f "$pid_file"
                else
                    echo -e "${YELLOW}Process $pid not found, cleaning up PID file${NC}"
                    rm -f "$pid_file"
                fi
            fi
        done
        
        if ! $found; then
            echo -e "${YELLOW}No running instances of $service_name found${NC}"
        fi
    else
        # Stop specific instance
        local instance_id="${service_name}-instance-${instance_num}"
        local pid_file="$PROJECT_ROOT/pids/${instance_id}.pid"
        
        if [ ! -f "$pid_file" ]; then
            echo -e "${YELLOW}Instance #$instance_num of $service_name is not running${NC}"
            return 0
        fi
        
        local pid=$(cut -d: -f1 "$pid_file")
        echo -e "${YELLOW}Stopping $instance_id (PID: $pid)...${NC}"
        
        if kill -15 "$pid" 2>/dev/null; then
            rm -f "$pid_file"
            echo -e "${GREEN}✓ Stopped $instance_id${NC}"
        else
            echo -e "${YELLOW}Process already stopped, cleaning up${NC}"
            rm -f "$pid_file"
        fi
    fi
}

# Function to list all services and their status
list_services() {
    echo -e "${BLUE}ChargeRoute Microservices Status (V2 - Eureka Enabled):${NC}"
    echo "========================================================="
    
    # Check Eureka status first
    if is_eureka_running; then
        echo -e "${GREEN}✓ Eureka Server: RUNNING on port $EUREKA_PORT${NC}"
        
        # Query Eureka for registered services
        echo -e "\n${CYAN}Services Registered in Eureka:${NC}"
        local eureka_data=$(query_eureka)
        if [ -n "$eureka_data" ]; then
            echo "$eureka_data" | grep -o '"app":"[^"]*"' | cut -d'"' -f4 | sort -u | while read app; do
                local count=$(echo "$eureka_data" | grep -o "\"app\":\"$app\"" | wc -l)
                echo "  - $app ($count instance(s))"
            done
        else
            echo "  No services registered"
        fi
    else
        echo -e "${RED}✗ Eureka Server: NOT RUNNING${NC}"
    fi
    
    echo -e "\n${CYAN}Local Process Status:${NC}"
    printf "%-40s %-8s %-10s %-8s %s\n" "INSTANCE ID" "PID" "SVC PORT" "MGMT PORT" "STATUS"
    echo "--------------------------------------------------------------------------------"
    
    for service_name in "${SERVICES[@]}"; do
        local has_instances=false
        
        for pid_file in "$PROJECT_ROOT/pids/${service_name}"*.pid; do
            if [ -f "$pid_file" ]; then
                has_instances=true
                local pid=$(cut -d: -f1 "$pid_file")
                local instance_id=$(cut -d: -f3 "$pid_file")
                local svc_port=$(cut -d: -f4 "$pid_file")
                local mgmt_port=$(cut -d: -f5 "$pid_file" 2>/dev/null || echo "N/A")
                
                local status="STOPPED"
                local status_color=$RED
                
                if kill -0 "$pid" 2>/dev/null; then
                    status="RUNNING"
                    status_color=$GREEN
                    
                    # Check if registered with Eureka
                    if is_eureka_running && [ "$service_name" != "eureka-discovery-service" ]; then
                        if get_eureka_instances "$service_name" | grep -q "$instance_id"; then
                            status="RUNNING+REG"
                        else
                            status="RUNNING-UNREG"
                            status_color=$YELLOW
                        fi
                    fi
                fi
                
                printf "%-40s %-8s %-10s %-8s " "$instance_id" "$pid" "${svc_port:-dynamic}" "${mgmt_port:-N/A}"
                echo -e "${status_color}$status${NC}"
            fi
        done
        
        if ! $has_instances && [ "$service_name" != "eureka-discovery-service" ]; then
            printf "%-40s %-8s %-10s %-8s " "$service_name" "N/A" "N/A" "N/A"
            echo -e "${RED}NO INSTANCES${NC}"
        fi
    done
    
    echo ""
    echo "Legend: REG=Registered with Eureka, UNREG=Not registered"
}

# Function to show service status
show_status() {
    local service_name=$1
    local instance_num=${2:-}
    
    if [ -z "$service_name" ]; then
        list_services
        return
    fi
    
    if [ -z "$instance_num" ]; then
        # Show all instances of the service
        echo -e "${BLUE}Status for $service_name:${NC}"
        local found=false
        
        for pid_file in "$PROJECT_ROOT/pids/${service_name}"*.pid; do
            if [ -f "$pid_file" ]; then
                found=true
                local pid=$(cut -d: -f1 "$pid_file")
                local instance_id=$(cut -d: -f3 "$pid_file")
                local svc_port=$(cut -d: -f4 "$pid_file")
                local mgmt_port=$(cut -d: -f5 "$pid_file" 2>/dev/null || echo "N/A")
                
                echo -e "\n${CYAN}Instance: $instance_id${NC}"
                
                if kill -0 "$pid" 2>/dev/null; then
                    echo -e "  Status: ${GREEN}RUNNING${NC}"
                    echo "  PID: $pid"
                    echo "  Service Port: ${svc_port:-dynamic}"
                    echo "  Management Port: ${mgmt_port}"
                    
                    # Check Actuator health endpoint if available
                    if [ "$mgmt_port" != "N/A" ] && [ "$mgmt_port" != "" ]; then
                        local health=$(curl -sf "http://localhost:$mgmt_port/actuator/health" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
                        if [ -n "$health" ]; then
                            echo "  Health: $health"
                        fi
                    fi
                else
                    echo -e "  Status: ${RED}STOPPED${NC} (stale PID file)"
                fi
            fi
        done
        
        if ! $found; then
            echo -e "${YELLOW}No instances found for $service_name${NC}"
        fi
    else
        # Show specific instance
        local instance_id="${service_name}-instance-${instance_num}"
        local pid_file="$PROJECT_ROOT/pids/${instance_id}.pid"
        
        if [ ! -f "$pid_file" ]; then
            echo -e "${YELLOW}Instance #$instance_num of $service_name is not running${NC}"
            return 0
        fi
        
        local pid=$(cut -d: -f1 "$pid_file")
        local svc_port=$(cut -d: -f4 "$pid_file")
        local mgmt_port=$(cut -d: -f5 "$pid_file" 2>/dev/null)
        
        echo -e "${BLUE}Status for $instance_id:${NC}"
        
        if kill -0 "$pid" 2>/dev/null; then
            echo -e "  Status: ${GREEN}RUNNING${NC}"
            echo "  PID: $pid"
            echo "  Service Port: ${svc_port:-dynamic}"
            echo "  Management Port: ${mgmt_port:-N/A}"
            
            # Check health
            if [ -n "$mgmt_port" ] && [ "$mgmt_port" != "N/A" ]; then
                local health=$(curl -sf "http://localhost:$mgmt_port/actuator/health" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
                if [ -n "$health" ]; then
                    echo "  Health: $health"
                fi
            fi
        else
            echo -e "  Status: ${RED}STOPPED${NC}"
        fi
    fi
}

# Function to start all services
start_all() {
    echo -e "${BLUE}Starting all services...${NC}"
    
    # Create required directories
    mkdir -p "$PROJECT_ROOT/logs" "$PROJECT_ROOT/pids"
    
    # Start Eureka first
    start_eureka
    
    # Wait for Eureka to be fully ready
    echo "Waiting for Eureka to be fully ready..."
    sleep 5
    
    # Start microservices (one instance each by default)
    for service_name in "${SERVICES[@]}"; do
        if [ "$service_name" != "eureka-discovery-service" ]; then
            start_service "$service_name" 1
            sleep 2
        fi
    done
    
    echo -e "\n${GREEN}All services start sequence completed${NC}\n"
    list_services
}

# Function to stop all services
stop_all() {
    echo -e "${BLUE}Stopping all services...${NC}"
    
    # Stop microservices first
    for service_name in "${SERVICES[@]}"; do
        if [ "$service_name" != "eureka-discovery-service" ]; then
            stop_service "$service_name"
        fi
    done
    
    # Stop Eureka last
    echo -e "\n${YELLOW}Stopping Eureka Discovery Service...${NC}"
    stop_service "eureka-discovery-service"
    
    echo -e "\n${GREEN}All services stopped${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <command> [service-name] [instance-num]"
    echo ""
    echo "Commands:"
    echo "  list                                  - List all services with status"
    echo "  status [service] [instance]           - Show status of service(s)"
    echo "  start <service> [instance]            - Start a service (auto-assigns instance if not specified)"
    echo "  start-all                             - Start all services (one instance each)"
    echo "  stop <service> [instance]             - Stop a service (all instances if instance not specified)"
    echo "  stop-all                              - Stop all services"
    echo "  restart <service> [instance]          - Restart a service instance"
    echo "  scale <service> <count>               - Scale service to specified number of instances"
    echo "  eureka-status                         - Show detailed Eureka registry status"
    echo ""
    echo "Available services:"
    for service_name in "${SERVICES[@]}"; do
        echo "  - $service_name"
    done
    echo ""
    echo "Examples:"
    echo "  $0 list                                         # List all services"
    echo "  $0 start user-management-service                # Start instance 1 of user service"
    echo "  $0 start user-management-service 2              # Start instance 2 of user service"
    echo "  $0 scale session-management-service 3           # Scale session service to 3 instances"
    echo "  $0 stop user-management-service 2               # Stop instance 2 of user service"
    echo "  $0 stop user-management-service                 # Stop all instances of user service"
    echo "  $0 status session-management-service            # Show all instances of session service"
    echo "  $0 eureka-status                                # Show services registered in Eureka"
}

# Function to scale a service to N instances
scale_service() {
    local service_name=$1
    local target_count=$2
    
    if [ -z "$target_count" ] || ! [[ "$target_count" =~ ^[0-9]+$ ]]; then
        echo -e "${RED}Error: Please specify a valid number of instances${NC}"
        return 1
    fi
    
    echo -e "${BLUE}Scaling $service_name to $target_count instance(s)...${NC}"
    
    # First, count current instances
    local current_count=0
    for pid_file in "$PROJECT_ROOT/pids/${service_name}-instance-"*.pid; do
        if [ -f "$pid_file" ]; then
            local pid=$(cut -d: -f1 "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                current_count=$((current_count + 1))
            fi
        fi
    done
    
    echo "Current instances: $current_count"
    echo "Target instances: $target_count"
    
    if [ $current_count -lt $target_count ]; then
        # Scale up
        for i in $(seq $((current_count + 1)) $target_count); do
            start_service "$service_name" "$i"
            sleep 2
        done
    elif [ $current_count -gt $target_count ]; then
        # Scale down
        for i in $(seq $target_count $((current_count - 1)) | tac); do
            stop_service "$service_name" $((i + 1))
        done
    else
        echo -e "${GREEN}Already at target instance count${NC}"
    fi
    
    echo -e "${GREEN}✓ Scaling complete${NC}"
}

# Function to show Eureka registry details
eureka_status() {
    if ! is_eureka_running; then
        echo -e "${RED}Eureka Server is not running${NC}"
        return 1
    fi
    
    echo -e "${BLUE}Eureka Discovery Service Registry:${NC}"
    echo "===================================="
    
    local apps_data=$(curl -sf -u "$EUREKA_AUTH" -H "Accept: application/json" "${EUREKA_APPS_URL}" 2>/dev/null)
    
    if [ -z "$apps_data" ]; then
        echo "No applications registered"
        return 0
    fi
    
    # Parse JSON and display formatted information
    echo "$apps_data" | python3 -c "
import json, sys
data = json.load(sys.stdin)
apps = data.get('applications', {}).get('application', [])
if not isinstance(apps, list):
    apps = [apps]

for app in apps:
    print(f\"\n{app['name']}:\")
    instances = app.get('instance', [])
    if not isinstance(instances, list):
        instances = [instances]
    
    for inst in instances:
        print(f\"  Instance ID: {inst['instanceId']}\")
        print(f\"    Status: {inst['status']}\")
        print(f\"    IP: {inst['ipAddr']}\")
        print(f\"    Port: {inst['port']['$']}\")
        print(f\"    Health URL: {inst['healthCheckUrl']}\")
        print()
" 2>/dev/null || echo "Error parsing Eureka response"
}

# Main script logic
case "${1:-}" in
    list)
        list_services
        ;;
    start)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        start_service "$2" "${3:-}"
        ;;
    start-all)
        start_all
        ;;
    stop)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        stop_service "$2" "${3:-}"
        ;;
    stop-all)
        stop_all
        ;;
    restart)
        if [ -z "${2:-}" ]; then
            echo -e "${RED}Error: Please specify a service name${NC}"
            show_usage
            exit 1
        fi
        stop_service "$2" "${3:-}"
        sleep 2
        start_service "$2" "${3:-}"
        ;;
    status)
        show_status "${2:-}" "${3:-}"
        ;;
    scale)
        if [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
            echo -e "${RED}Error: Please specify service name and target count${NC}"
            show_usage
            exit 1
        fi
        scale_service "$2" "$3"
        ;;
    eureka-status)
        eureka_status
        ;;
    *)
        show_usage
        exit 1
        ;;
esac