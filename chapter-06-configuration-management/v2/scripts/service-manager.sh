#!/bin/bash

# Service Manager for Chapter 6 v2 - Configuration Management with All Services
# Usage: ./service-manager.sh [build|clean|start|stop|restart|status|logs|test]

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
        for service in config-server eureka-server api-gateway user-management-service charge-unit-management-service session-management-service notification-management-service; do
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
        
        # Check Config Server
        if curl -s http://configuser:configpass@localhost:8888/actuator/health > /dev/null 2>&1; then
            echo "✓ Config Server (8888) - Running"
        else
            echo "✗ Config Server (8888) - Not running"
        fi
        
        # Check Eureka Server
        if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
            echo "✓ Eureka Server (8761) - Running"
        else
            echo "✗ Eureka Server (8761) - Not running"
        fi
        
        # Check API Gateway
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo "✓ API Gateway (8080) - Running"
        else
            echo "✗ API Gateway (8080) - Not running"
        fi
        
        # Check User Management Service (dynamic port - check via Eureka)
        USER_SERVICE_INFO=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$USER_SERVICE_INFO" ] && curl -s http://localhost:$USER_SERVICE_INFO/actuator/health > /dev/null 2>&1; then
            echo "✓ User Management Service ($USER_SERVICE_INFO) - Running"
        else
            echo "✗ User Management Service - Not running or not registered"
        fi
        
        # Check Charge Unit Management Service (dynamic port)
        CHARGE_SERVICE_INFO=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/CHARGE-UNIT-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$CHARGE_SERVICE_INFO" ] && curl -s http://localhost:$CHARGE_SERVICE_INFO/actuator/health > /dev/null 2>&1; then
            echo "✓ Charge Unit Management Service ($CHARGE_SERVICE_INFO) - Running"
        else
            echo "✗ Charge Unit Management Service - Not running or not registered"
        fi
        
        # Check Session Management Service (dynamic port)
        SESSION_SERVICE_INFO=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/SESSION-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$SESSION_SERVICE_INFO" ] && curl -s http://localhost:$SESSION_SERVICE_INFO/actuator/health > /dev/null 2>&1; then
            echo "✓ Session Management Service ($SESSION_SERVICE_INFO) - Running"
        else
            echo "✗ Session Management Service - Not running or not registered"
        fi
        
        # Check Notification Management Service (dynamic port)
        NOTIFICATION_SERVICE_INFO=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/NOTIFICATION-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$NOTIFICATION_SERVICE_INFO" ] && curl -s http://localhost:$NOTIFICATION_SERVICE_INFO/actuator/health > /dev/null 2>&1; then
            echo "✓ Notification Management Service ($NOTIFICATION_SERVICE_INFO) - Running"
        else
            echo "✗ Notification Management Service - Not running or not registered"
        fi
        ;;
    logs)
        service_name=${2:-"all"}
        case "$service_name" in
            config)
                tail -f ../logs/config-server.log
                ;;
            eureka)
                tail -f ../logs/eureka-server.log
                ;;
            gateway)
                tail -f ../logs/api-gateway.log
                ;;
            user)
                tail -f ../logs/user-management-service.log
                ;;
            charge-unit)
                tail -f ../logs/charge-unit-management-service.log
                ;;
            session)
                tail -f ../logs/session-management-service.log
                ;;
            notification)
                tail -f ../logs/notification-management-service.log
                ;;
            all|*)
                echo "Available log files:"
                ls -la ../logs/ 2>/dev/null || echo "No log files found. Start services first."
                echo ""
                echo "Usage: $0 logs [config|eureka|gateway|user|charge-unit|session|notification]"
                ;;
        esac
        ;;
    test)
        echo "Running basic tests..."
        echo "====================="
        
        echo "1. Testing Config Server..."
        curl -s http://configuser:configpass@localhost:8888/actuator/health | jq '.' || echo "Config Server not responding"
        
        echo -e "\n2. Testing Eureka Server..."
        curl -s http://localhost:8761/actuator/health | jq '.' || echo "Eureka Server not responding"
        
        echo -e "\n3. Testing API Gateway..."
        curl -s http://localhost:8080/actuator/health | jq '.' || echo "API Gateway not responding"
        
        echo -e "\n4. Testing service configurations via Config Server, branch development..."
        curl -s http://configuser:configpass@localhost:8888/user-management-service/dev/development | jq '.' || echo "User Management config not responding"

       echo -e "\n5. Testing service configurations via Config Server, branch main..."
       curl -s http://configuser:configpass@localhost:8888/user-management-service/prod/main | jq '.' || echo "User Management config not responding"

        echo -e "\n6. Testing services via API Gateway..."
        echo "Getting service ports from Eureka..."
        
        USER_PORT=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/USER-MANAGEMENT-SERVICE 2>/dev/null | grep -o '<port[^>]*>[0-9]*</port>' | head -1 | sed 's/<port[^>]*>//g' | sed 's/<\/port>//g')
        if [ ! -z "$USER_PORT" ]; then
            echo "User Management Service running on port: $USER_PORT"
            curl -s http://localhost:$USER_PORT/actuator/health || echo "User Service direct access failed"
            curl -s http://localhost:8080/api/users/health || echo "User Service via API Gateway failed"
        else
            echo "User Management Service not found in Eureka registry"
        fi
        ;;
    *)
        echo "Chapter 6 v2 - Configuration Management with All Services"
        echo "========================================================="
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
        echo "  logs     - View service logs (logs [config|eureka|gateway|user|charge-unit|session|notification])"
        echo "  test     - Run basic functionality tests"
        echo ""
        echo "Services:"
        echo "  Config Server               - http://localhost:8888"
        echo "  Eureka Server               - http://localhost:8761"
        echo "  API Gateway                 - http://localhost:8080"
        echo "  User Management             - Dynamic port (check via Eureka)"
        echo "  Charge Unit Management      - Dynamic port (check via Eureka)"
        echo "  Session Management          - Dynamic port (check via Eureka)"
        echo "  Notification Management     - Dynamic port (check via Eureka)"
        echo ""
        echo "Startup Order:"
        echo "  1. Config Server (8888) - Configuration management"
        echo "  2. Eureka Server (8761) - Service discovery"
        echo "  3. API Gateway (8080) - API routing and load balancing"
        echo "  4. Business Services - All use dynamic ports via Config Server"
        ;;
esac