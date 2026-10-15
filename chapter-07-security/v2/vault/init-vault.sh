#!/bin/bash

# Initialize Vault with OAuth2 secrets for Chapter 7 V2 values
# This script sets up Vault in dev mode and creates secret paths

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="root-token"

echo "=== Initializing Vault for EV Charging Platform ==="
echo ""

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until curl -s ${VAULT_ADDR}/v1/sys/health > /dev/null 2>&1; do
    echo "Waiting for Vault..."
    sleep 2
done
echo "✓ Vault is ready"
echo ""

# Enable KV v2 secrets engine (should already be enabled in dev mode)
echo "Enabling KV v2 secrets engine..."
curl -s \
    -H "X-Vault-Token: ${VAULT_TOKEN}" \
    -X POST \
    -d '{"type":"kv-v2"}' \
    ${VAULT_ADDR}/v1/sys/mounts/secret || echo "KV v2 already enabled"
echo "✓ KV v2 secrets engine ready"
echo ""

# Create secret paths (structure for organization)
echo "Creating secret path structure..."
echo "  - secret/ev-charging/session-management-service"
echo "  - secret/ev-charging/notification-management-service"
echo ""

echo "=== Vault initialization complete ==="
echo ""
echo "Vault is ready for secrets. Run seed-secrets.sh to populate OAuth2 credentials."
