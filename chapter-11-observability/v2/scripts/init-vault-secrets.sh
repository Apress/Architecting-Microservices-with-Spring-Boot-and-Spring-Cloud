#!/bin/bash
set -e

# Script to initialize Vault with database credentials for ChargeRoute microservices
# Run this after starting docker-compose to populate Vault with secrets

echo "=========================================="
echo "Initializing Vault secrets..."
echo "=========================================="

export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='root-token'

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    echo "  Waiting for Vault..."
    sleep 2
done
echo "✓ Vault is ready"

# Enable KV v2 secrets engine if not already enabled
echo ""
echo "Enabling KV v2 secrets engine..."
vault secrets enable -version=2 -path=secret kv 2>/dev/null || echo "  KV secrets engine already enabled"

# Store Keycloak database password
echo ""
echo "Storing Keycloak database password..."
vault kv put secret/keycloak \
    spring.datasource.password=keycloak_password
echo "✓ Keycloak password stored at secret/keycloak"

# Store User Management Service database password
echo ""
echo "Storing User Management Service database password..."
vault kv put secret/user-management-service \
    spring.datasource.password=userpass123
echo "✓ User Management Service password stored at secret/user-management-service"

# Store Charge Unit Management Service database password
echo ""
echo "Storing Charge Unit Management Service database password..."
vault kv put secret/charge-unit-management-service \
    spring.datasource.password=chargeunitpass123
echo "✓ Charge Unit Management Service password stored at secret/charge-unit-management-service"

# Store Session Management Service database password
echo ""
echo "Storing Session Management Service database password..."
vault kv put secret/session-management-service \
    spring.datasource.password=sessionpass123
echo "✓ Session Management Service password stored at secret/session-management-service"

# Store Notification Management Service database password
echo ""
echo "Storing Notification Management Service database password..."
vault kv put secret/notification-management-service \
    spring.datasource.password=notificationpass123
echo "✓ Notification Management Service password stored at secret/notification-management-service"

echo ""
echo "=========================================="
echo "Vault initialization completed!"
echo "=========================================="
echo ""
echo "Database passwords stored in Vault:"
echo "  - secret/keycloak (spring.datasource.password)"
echo "  - secret/user-management-service (spring.datasource.password)"
echo "  - secret/charge-unit-management-service (spring.datasource.password)"
echo "  - secret/session-management-service (spring.datasource.password)"
echo "  - secret/notification-management-service (spring.datasource.password)"
echo ""
echo "To view a secret:"
echo "  vault kv get secret/user-management-service"
echo ""
echo "To rotate a password:"
echo "  vault kv put secret/user-management-service spring.datasource.password=newpass123"
echo ""
echo "Services will pick up new passwords after restart or Spring Cloud Bus refresh"
echo "=========================================="
