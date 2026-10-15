#!/bin/bash

# Service Manager for Chapter 6 v1 - Configuration Management
# Usage: ./service-manager.sh [build|start|stop|restart|status|logs|test]

# Change to the script directory
cd "$(dirname "$0")"

case "$1" in
    build)
        echo "Building all services..."
        ./build-all.sh
        ;;
    clean)
        echo "Cleaning all services..."
        echo "Stopping services first..."
        ./stop-all.sh
        echo "Cleaning Maven artifacts..."
        cd ..
        for service in config-server eureka-server user-management-service; do
            if [ -d "$service" ]; then
                echo "Cleaning $service..."
                cd $service
                mvn clean
                cd ..
            fi
        done
        echo "Removing logs..."
        rm -rf logs/
        echo "Clean complete!"
        ;;
    start)
        echo "Creating logs directory..."
        mkdir -p ../logs
        ./start-all.sh
        ;;
    stop)
        ./stop-all.sh
        ;;
    restart)
        echo "Restarting all services..."
        ./stop-all.sh
        sleep 2
        ./start-all.sh
        ;;
    status)
        echo "Checking service status..."
        echo "=========================="
        
        # Check Eureka Server
        if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
            echo "✓ Eureka Server (8761) - Running"
        else
            echo "✗ Eureka Server (8761) - Not running"
        fi
        
        # Check Config Server
        if curl -s http://configuser:configpass@localhost:8888/actuator/health > /dev/null 2>&1; then
            echo "✓ Config Server (8888) - Running"
        else
            echo "✗ Config Server (8888) - Not running"
        fi
        
        # Check User Management Service (dynamic port - check via Eureka)
        USER_SERVICE_INFO=$(curl -s http://localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$USER_SERVICE_INFO" ] && curl -s http://localhost:$USER_SERVICE_INFO/users/health > /dev/null 2>&1; then
            echo "✓ User Management Service ($USER_SERVICE_INFO) - Running"
        else
            echo "✗ User Management Service - Not running or not registered"
        fi
        ;;
    logs)
        service_name=${2:-"all"}
        case "$service_name" in
            eureka)
                tail -f ../logs/eureka-server.log
                ;;
            config)
                tail -f ../logs/config-server.log
                ;;
            user)
                tail -f ../logs/user-management-service.log
                ;;
            all|*)
                echo "Available log files:"
                ls -la ../logs/ 2>/dev/null || echo "No log files found. Start services first."
                echo ""
                echo "Usage: $0 logs [eureka|config|user]"
                ;;
        esac
        ;;
    test)
        echo "Running basic tests..."
        echo "====================="
        
        echo "1. Testing Eureka Server..."
        curl -s http://localhost:8761/actuator/health | jq '.' || echo "Eureka not responding"
        
        echo -e "\n2. Testing Config Server..."
        curl -s http://configuser:configpass@localhost:8888/user-management-service/dev | jq '.' || echo "Config Server not responding"
        
        echo -e "\n3. Getting User Service port from Eureka..."
        USER_PORT=$(curl -s http://localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$USER_PORT" ]; then
            echo "User Management Service running on port: $USER_PORT"
            
            echo -e "\n4. Testing User Service health..."
            curl -s http://localhost:$USER_PORT/users/health || echo "User Service not responding"
            
            echo -e "\n5. Testing User Service validation..."
            curl -s http://localhost:$USER_PORT/users/user123/validate | jq '.' || echo "User Service validation not responding"
            
            echo -e "\n6. Testing User Service configuration..."
            curl -s http://localhost:$USER_PORT/users/config | jq '.' || echo "User Service config not responding"
        else
            echo "User Management Service not found in Eureka registry"
        fi
        ;;
    *)
        echo "Chapter 6 v1 - Configuration Management Service Manager"
        echo "======================================================="
        echo ""
        echo "Usage: $0 [build|clean|start|stop|restart|status|logs|test]"
        echo ""
        echo "Commands:"
        echo "  build    - Build all services"
        echo "  clean    - Clean all services (stop, clean Maven artifacts, remove logs)"
        echo "  start    - Start all services"
        echo "  stop     - Stop all services"
        echo "  restart  - Restart all services"
        echo "  status   - Check service status"
        echo "  logs     - View service logs (logs [eureka|config|user])"
        echo "  test     - Run basic functionality tests"
        echo ""
        echo "Services:"
        echo "  Eureka Server           - http://localhost:8761"
        echo "  Config Server           - http://localhost:8888"
        echo "  User Management         - Dynamic port (check via Eureka)"
        ;;
esac