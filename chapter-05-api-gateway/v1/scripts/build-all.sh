#!/bin/bash

# ChargeRoute Chapter 5 Build All Services
# Builds all Maven projects in the correct order

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

# Service definitions (in build order)
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

# Build options
SKIP_TESTS=false
VERBOSE=false
PARALLEL=false
BUILD_MODE="compile"

# Build a service
build_service() {
    local service_name="$1"
    local service_dir="$(get_service_dir "$service_name")"
    
    if [[ ! -d "$service_dir" ]]; then
        error "Service directory not found: $service_dir"
        return 1
    fi
    
    log "Building $service_name..."
    
    cd "$service_dir"
    
    # Construct Maven command
    local mvn_cmd="./mvnw"
    local mvn_args=()
    
    # Add build mode
    case "$BUILD_MODE" in
        compile)
            mvn_args+=("clean" "compile")
            ;;
        package)
            mvn_args+=("clean" "package")
            ;;
        install)
            mvn_args+=("clean" "install")
            ;;
    esac
    
    # Add options
    if [[ "$SKIP_TESTS" == "true" ]]; then
        mvn_args+=("-DskipTests")
    fi
    
    if [[ "$VERBOSE" == "false" ]]; then
        mvn_args+=("-q")
    fi
    
    # Execute build
    local start_time=$(date +%s)
    
    if [[ "$VERBOSE" == "true" ]]; then
        "${mvn_cmd}" "${mvn_args[@]}"
    else
        "${mvn_cmd}" "${mvn_args[@]}" > /dev/null 2>&1
    fi
    
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [[ $exit_code -eq 0 ]]; then
        success "✓ $service_name built successfully (${duration}s)"
        return 0
    else
        error "✗ Failed to build $service_name"
        return 1
    fi
}

# Build services in parallel
build_parallel() {
    local services=("$@")
    local pids=()
    local results=()
    
    log "Building ${#services[@]} services in parallel..."
    
    # Start builds in background
    for service in "${services[@]}"; do
        (
            build_service "$service"
            echo $? > "/tmp/build_result_$service"
        ) &
        pids+=($!)
    done
    
    # Wait for all builds to complete
    for i in "${!pids[@]}"; do
        wait "${pids[$i]}"
        local service="${services[$i]}"
        local result=$(cat "/tmp/build_result_$service" 2>/dev/null || echo "1")
        results+=("$service:$result")
        rm -f "/tmp/build_result_$service"
    done
    
    # Check results
    local failed_services=()
    for result in "${results[@]}"; do
        local service="${result%:*}"
        local exit_code="${result#*:}"
        if [[ "$exit_code" != "0" ]]; then
            failed_services+=("$service")
        fi
    done
    
    return ${#failed_services[@]}
}

# Build all services
build_all_services() {
    echo -e "\n${BLUE}=== ChargeRoute Chapter 5 - Build All Services ===${NC}\n"
    echo -e "${BLUE}Build Mode:${NC} $BUILD_MODE"
    echo -e "${BLUE}Skip Tests:${NC} $SKIP_TESTS"
    echo -e "${BLUE}Parallel:${NC} $PARALLEL"
    echo ""
    
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    local failed_services=()
    local start_time=$(date +%s)
    
    if [[ "$PARALLEL" == "true" ]]; then
        # Parallel build (less safe but faster)
        if ! build_parallel "${services[@]}"; then
            # Collect failed services from error messages
            failed_services=("check logs")
        fi
    else
        # Sequential build (safer, shows progress)
        for service in "${services[@]}"; do
            if ! build_service "$service"; then
                failed_services+=("$service")
                
                # Stop on first failure unless continuing
                if [[ "$CONTINUE_ON_FAILURE" != "true" ]]; then
                    break
                fi
            fi
        done
    fi
    
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    echo ""
    echo -e "${BLUE}Build Summary (${total_duration}s total):${NC}"
    
    if [[ ${#failed_services[@]} -eq 0 ]]; then
        success "All ${#services[@]} services built successfully!"
        echo -e "\n${GREEN}Ready to start services with:${NC}"
        echo -e "  ${CYAN}./service-manager.sh start-all${NC}"
    else
        error "Some services failed to build:"
        for service in "${failed_services[@]}"; do
            echo -e "  ${RED}✗${NC} $service"
        done
        echo -e "\n${YELLOW}Fix the errors above and retry${NC}"
        exit 1
    fi
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        error "Java not found. Please install Java 21+"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log "Java version: $java_version"
    
    # Check Maven wrapper
    local missing_mvnw=()
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    for service in "${services[@]}"; do
        local service_dir="$(get_service_dir "$service")"
        if [[ ! -x "$service_dir/mvnw" ]]; then
            missing_mvnw+=("$service")
        fi
    done
    
    if [[ ${#missing_mvnw[@]} -gt 0 ]]; then
        error "Missing Maven wrapper in services:"
        for service in "${missing_mvnw[@]}"; do
            echo "  - $service"
        done
        exit 1
    fi
    
    success "Prerequisites check passed"
}

# Show build status
show_status() {
    echo -e "\n${BLUE}=== Build Status ===${NC}\n"
    
    local services=("$EUREKA_SERVICE" "$USER_SERVICE" "$CHARGE_UNIT_SERVICE" "$SESSION_SERVICE" "$NOTIFICATION_SERVICE" "$GATEWAY_SERVICE")
    
    printf "%-35s %-15s %-20s\n" "SERVICE" "STATUS" "LAST BUILT"
    printf "%-35s %-15s %-20s\n" "-------" "------" "----------"
    
    for service in "${services[@]}"; do
        local service_dir="$(get_service_dir "$service")"
        local target_dir="$service_dir/target"
        local jar_file="$target_dir/*.jar"
        
        if [[ -d "$target_dir" ]] && compgen -G "$jar_file" > /dev/null 2>&1; then
            local last_built=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M" "$target_dir" 2>/dev/null || echo "Unknown")
            printf "%-35s ${GREEN}%-15s${NC} %-20s\n" "$service" "BUILT" "$last_built"
        else
            printf "%-35s ${RED}%-15s${NC} %-20s\n" "$service" "NOT BUILT" "N/A"
        fi
    done
    
    echo ""
}

# Show usage
show_help() {
    echo "ChargeRoute Chapter 5 Build All Services"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Build Options:"
    echo "  --compile        Compile only (default)"
    echo "  --package        Compile and package JARs"
    echo "  --install        Compile, package, and install to local Maven repo"
    echo ""
    echo "Execution Options:"
    echo "  --skip-tests     Skip running tests during build"
    echo "  --verbose        Show detailed Maven output"
    echo "  --parallel       Build services in parallel (faster but less safe)"
    echo "  --continue       Continue building other services if one fails"
    echo ""
    echo "Utility Options:"
    echo "  --status         Show current build status"
    echo "  --check          Check prerequisites only"
    echo "  --help, -h       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                      # Basic compile"
    echo "  $0 --package --skip-tests  # Package without tests"
    echo "  $0 --parallel --verbose    # Parallel build with output"
    echo "  $0 --status               # Check build status"
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --compile)
                BUILD_MODE="compile"
                shift
                ;;
            --package)
                BUILD_MODE="package"
                shift
                ;;
            --install)
                BUILD_MODE="install"
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --parallel)
                PARALLEL=true
                shift
                ;;
            --continue)
                CONTINUE_ON_FAILURE=true
                shift
                ;;
            --status)
                show_status
                exit 0
                ;;
            --check)
                check_prerequisites
                exit 0
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# Main execution
main() {
    parse_args "$@"
    check_prerequisites
    build_all_services
}

main "$@"