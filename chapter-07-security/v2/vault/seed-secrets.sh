#!/bin/bash

# Seed Vault with OAuth2 client secrets and Eureka credentials
# These secrets are moved from config-data YAML files to Vault for production security

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="root-token"

echo "=== Seeding Vault with OAuth2 Secrets ==="
echo ""

# Session Management Service - OAuth2 client credentials
echo "Creating secrets for session-management-service..."
curl -s \
    -H "X-Vault-Token: ${VAULT_TOKEN}" \
    -X POST \
    -d '{
        "data": {
            "client-id": "session-service-client",
            "client-secret": "session-secret"
        }
    }' \
    ${VAULT_ADDR}/v1/secret/data/ev-charging/session-management-service > /dev/null
echo "✓ session-service-client credentials stored"

# Notification Management Service - OAuth2 client credentials
echo "Creating secrets for notification-management-service..."
curl -s \
    -H "X-Vault-Token: ${VAULT_TOKEN}" \
    -X POST \
    -d '{
        "data": {
            "client-id": "notification-service-client",
            "client-secret": "notification-secret"
        }
    }' \
    ${VAULT_ADDR}/v1/secret/data/ev-charging/notification-management-service > /dev/null
echo "✓ notification-service-client credentials stored"

echo ""
echo "=== All secrets seeded successfully ==="
echo ""
echo "Verify secrets with:"
echo "  curl -H \"X-Vault-Token: root-token\" http://localhost:8200/v1/secret/data/ev-charging/session-management-service"
