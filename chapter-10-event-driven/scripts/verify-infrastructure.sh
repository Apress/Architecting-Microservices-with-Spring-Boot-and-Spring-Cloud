#!/bin/bash

# Script to verify the infrastructure setup is working correctly

set -e

echo "=========================================="
echo "ChargeRoute Infrastructure Verification"
echo "=========================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a service is responding
check_service() {
    local service_name=$1
    local url=$2
    local expected_code=${3:-200}

    printf "Checking %-25s " "$service_name..."

    if response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>&1); then
        if [ "$response" = "$expected_code" ]; then
            echo -e "${GREEN}✓ OK${NC} (HTTP $response)"
            return 0
        else
            echo -e "${YELLOW}⚠ Unexpected${NC} (HTTP $response, expected $expected_code)"
            return 1
        fi
    else
        echo -e "${RED}✗ Failed${NC} (Connection refused)"
        return 1
    fi
}

# Function to check database connectivity
check_database() {
    local db_name=$1
    local db_user=$2
    local db_password=$3

    printf "Checking database %-15s " "$db_name..."

    if docker exec chargeroute-postgres psql -U $db_user -d $db_name -c "SELECT 1" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ Failed${NC}"
        return 1
    fi
}

# Function to check Vault secret
check_vault_secret() {
    local secret_path=$1

    printf "Checking Vault secret %-35s " "$secret_path..."

    export VAULT_ADDR='http://localhost:8200'
    export VAULT_TOKEN='root-token'

    if vault kv get "$secret_path" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ Failed${NC}"
        return 1
    fi
}

# Check Docker containers are running
echo "1. Checking Docker Containers"
echo "------------------------------"
if ! docker ps | grep -q "chargeroute-postgres"; then
    echo -e "${RED}✗ PostgreSQL container not running${NC}"
    echo "  Run: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ PostgreSQL container running${NC}"

if ! docker ps | grep -q "chargeroute-vault"; then
    echo -e "${YELLOW}⚠ Vault container not running${NC}"
else
    echo -e "${GREEN}✓ Vault container running${NC}"
fi

if ! docker ps | grep -q "chargeroute-keycloak"; then
    echo -e "${YELLOW}⚠ Keycloak container not running${NC}"
else
    echo -e "${GREEN}✓ Keycloak container running${NC}"
fi

echo ""
echo "2. Checking Service Endpoints"
echo "------------------------------"
check_service "Vault" "http://localhost:8200/v1/sys/health" "200" || true
check_service "Keycloak" "http://localhost:9000/realms/master" "200" || true
check_service "pgAdmin" "http://localhost:5050" "302" || true

echo ""
echo "3. Checking Database Connectivity"
echo "----------------------------------"
check_database "userdb" "userservice" "userpass123"
check_database "chargeunitdb" "chargeunitservice" "chargeunitpass123"
check_database "sessiondb" "sessionservice" "sessionpass123"
check_database "notificationdb" "notificationservice" "notificationpass123"

echo ""
echo "4. Checking Database Schemas"
echo "-----------------------------"

# Check if Flyway migrations have run
printf "Checking userdb schema...          "
if docker exec chargeroute-postgres psql -U userservice -d userdb -c "\dt" 2>/dev/null | grep -q "users"; then
    echo -e "${GREEN}✓ Migrations applied${NC}"
else
    echo -e "${YELLOW}⚠ No migrations yet${NC} (Run services to apply)"
fi

printf "Checking chargeunitdb schema...    "
if docker exec chargeroute-postgres psql -U chargeunitservice -d chargeunitdb -c "\dt" 2>/dev/null | grep -q "charge_units"; then
    echo -e "${GREEN}✓ Migrations applied${NC}"
else
    echo -e "${YELLOW}⚠ No migrations yet${NC} (Run services to apply)"
fi

printf "Checking sessiondb schema...       "
if docker exec chargeroute-postgres psql -U sessionservice -d sessiondb -c "\dt" 2>/dev/null | grep -q "charging_sessions"; then
    echo -e "${GREEN}✓ Migrations applied${NC}"
else
    echo -e "${YELLOW}⚠ No migrations yet${NC} (Run services to apply)"
fi

printf "Checking notificationdb schema...  "
if docker exec chargeroute-postgres psql -U notificationservice -d notificationdb -c "\dt" 2>/dev/null | grep -q "notifications"; then
    echo -e "${GREEN}✓ Migrations applied${NC}"
else
    echo -e "${YELLOW}⚠ No migrations yet${NC} (Run services to apply)"
fi

echo ""
echo "5. Checking Vault Secrets"
echo "-------------------------"

if command -v vault > /dev/null 2>&1; then
    check_vault_secret "secret/user-management-service" || true
    check_vault_secret "secret/charge-unit-management-service" || true
    check_vault_secret "secret/session-management-service" || true
    check_vault_secret "secret/notification-management-service" || true
else
    echo -e "${YELLOW}⚠ Vault CLI not installed${NC}"
    echo "  Install: brew install vault"
fi

echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="
echo ""
echo "Quick Commands:"
echo "  View logs:      docker-compose logs -f [service-name]"
echo "  Connect to DB:  psql -h localhost -p 5433 -U userservice -d userdb"
echo "  Access Vault:   vault kv get secret/user-management-service/db"
echo "  Keycloak UI:    http://localhost:9000 (admin/admin)"
echo "  pgAdmin UI:     http://localhost:5050 (admin@chargeroute.com/admin123)"
echo ""
