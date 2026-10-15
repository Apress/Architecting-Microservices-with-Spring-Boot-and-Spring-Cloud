#!/bin/bash

# Service Manager for Chapter 8 - Data Persistence
# Usage: ./service-manager.sh [build|clean|start|stop|restart|status|logs]

cd "$(dirname "$0")"

case "$1" in
    build)
        echo "Building all services..."
        cd ..
        mvn clean install -DskipTests
        echo "Build complete!"
        ;;
    clean)
        echo "Cleaning all services..."
        echo "Stopping services first..."
        $0 stop
        echo "Cleaning Maven artifacts..."
        cd ..
        mvn clean
        echo "Removing logs..."
        rm -rf logs/
        echo "Clean complete!"
        ;;
    start)
        echo "Creating logs directory..."
        mkdir -p ../logs

        # Check Docker availability
        if ! command -v docker &> /dev/null; then
            echo "WARNING: Docker is not available - infrastructure will not start"
            exit 1
        fi

        # Start infrastructure (PostgreSQL, Keycloak, Vault, pgAdmin)
        echo "Starting infrastructure containers..."
        cd ..
        docker compose up -d
        echo "Waiting for Keycloak to be ready (this takes ~60 seconds)..."
        sleep 60

        # Seed Vault secrets
        export VAULT_ADDR='http://localhost:8200'
        export VAULT_TOKEN='root-token'
        if [ -f "scripts/init-vault-secrets.sh" ]; then
            echo "Seeding Vault secrets..."
            ./scripts/init-vault-secrets.sh
        fi
        cd scripts

        echo "Starting All services with OAuth2 security (Keycloak)..."
        echo "=========================================="

        # 1. Config Server (must start first - needs Vault)
        echo "Starting Config Server..."
        cd ../config-server
        nohup mvn spring-boot:run > ../logs/config-server.log 2>&1 &
        echo $! > ../logs/config-server.pid
        sleep 15

        # 2. Eureka Server (service discovery)
        echo "Starting Eureka Server..."
        cd ../eureka-server
        nohup mvn spring-boot:run > ../logs/eureka-server.log 2>&1 &
        echo $! > ../logs/eureka-server.pid
        sleep 15

        # 3. API Gateway
        echo "Starting API Gateway..."
        cd ../api-gateway
        nohup mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
        echo $! > ../logs/api-gateway.pid
        sleep 10

        # 4. Business Services (can start in parallel)
        echo "Starting User Management Service..."
        cd ../user-management-service
        nohup mvn spring-boot:run > ../logs/user-management-service.log 2>&1 &
        echo $! > ../logs/user-management-service.pid

        echo "Starting Charge Unit Management Service..."
        cd ../charge-unit-management-service
        nohup mvn spring-boot:run > ../logs/charge-unit-management-service.log 2>&1 &
        echo $! > ../logs/charge-unit-management-service.pid

        echo "Starting Session Management Service..."
        cd ../session-management-service
        nohup mvn spring-boot:run > ../logs/session-management-service.log 2>&1 &
        echo $! > ../logs/session-management-service.pid

        echo "Starting Notification Management Service..."
        cd ../notification-management-service
        nohup mvn spring-boot:run > ../logs/notification-management-service.log 2>&1 &
        echo $! > ../logs/notification-management-service.pid

        cd ../scripts

        echo ""
        echo "All services started! Waiting for initialization..."
        sleep 15

        echo ""
        echo "Infrastructure Service URLs (fixed ports):"
        echo "  Vault: http://localhost:8200"
        echo "  Config Server: http://localhost:8888"
        echo "  Keycloak: http://localhost:9000 (admin/admin)"
        echo "  Eureka Server: http://localhost:8761"
        echo "  API Gateway: http://localhost:8080"
        echo ""
        echo "Business Services (dynamic ports - use status command to see assigned ports):"
        echo "  • user-management-service"
        echo "  • charge-unit-management-service"
        echo "  • session-management-service"
        echo "  • notification-management-service"
        echo ""
        echo "Check service status and ports with: ./service-manager.sh status"
        ;;
    stop)
        echo "Stopping all services..."
        cd ..

        # Stop services in reverse order
        for service in notification-management-service session-management-service charge-unit-management-service user-management-service api-gateway eureka-server config-server; do
            if [ -f "logs/$service.pid" ]; then
                PID=$(cat logs/$service.pid)
                if ps -p $PID > /dev/null 2>&1; then
                    echo "Stopping $service (PID: $PID)..."
                    kill $PID
                    rm logs/$service.pid
                fi
            fi
        done

        # Stop Docker containers (PostgreSQL, Keycloak, Vault, pgAdmin)
        if command -v docker &> /dev/null; then
            echo "Stopping infrastructure containers..."
            docker compose down
        fi

        echo "All services stopped!"
        ;;
    restart)
        echo "Restarting all services..."
        $0 stop
        sleep 3
        $0 start
        ;;
    status)
        echo "Checking service status..."
        echo "=========================="

        # Check Vault
        if curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
            echo "✓ Vault (8200) - Running"
        else
            echo "✗ Vault (8200) - Not running"
        fi

        # Check Config Server
        if curl -s http://configuser:configpass@localhost:8888/actuator/health > /dev/null 2>&1; then
            echo "✓ Config Server (8888) - Running"
        else
            echo "✗ Config Server (8888) - Not running"
        fi

        # Check Keycloak (Authorization Server)
        if curl -s http://localhost:9000/health/ready > /dev/null 2>&1; then
            echo "✓ Keycloak (9000) - Running"
        else
            echo "✗ Keycloak (9000) - Not running"
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

        # Check Business Services (query Eureka for dynamic ports)
        echo ""
        echo "Business Services (dynamic ports from Eureka):"
        for service in user-management-service charge-unit-management-service session-management-service notification-management-service; do
            # Convert service name to uppercase for Eureka
            SERVICE_NAME=$(echo $service | tr '[:lower:]' '[:upper:]' | tr '-' '-')

            # Query Eureka for service instances
            EUREKA_RESPONSE=$(curl -s http://chargeroute:Admin123!@localhost:8761/eureka/apps/$SERVICE_NAME 2>/dev/null)

            if [ $? -eq 0 ] && echo "$EUREKA_RESPONSE" | grep -q "<status>"; then
                # Extract enabled port and status from Eureka response
                PORT=$(echo "$EUREKA_RESPONSE" | grep -o '<port enabled="true">[0-9]*</port>' | grep -o '[0-9]*')
                STATUS=$(echo "$EUREKA_RESPONSE" | grep -o '<status>[^<]*</status>' | head -1 | sed 's/<\/*status>//g')

                if [ "$STATUS" = "UP" ] && [ -n "$PORT" ]; then
                    echo "✓ $service ($PORT) - Running"
                else
                    echo "✗ $service (port unknown) - Registered but not UP"
                fi
            else
                echo "✗ $service - Not registered in Eureka"
            fi
        done
        ;;
    logs)
        SERVICE=$2
        if [ -z "$SERVICE" ]; then
            echo "Usage: ./service-manager.sh logs <service-name>"
            echo "Services: config-server, authorization-server, eureka-server, api-gateway, user-management-service, charge-unit-management-service, session-management-service, notification-management-service"
        else
            tail -f ../logs/$SERVICE.log
        fi
        ;;
    *)
        echo "Usage: $0 {build|clean|start|stop|restart|status|logs <service>}"
        exit 1
        ;;
esac
