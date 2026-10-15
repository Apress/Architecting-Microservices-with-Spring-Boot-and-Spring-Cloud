#!/bin/bash

# Vault initialization script for Chapter 7 v1
# Stores infrastructure authentication and OAuth2 client secrets

set -e

echo "🏗️  Initializing HashiCorp Vault for Chapter 7 v1..."

# Set Vault environment
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'

# Wait for Vault to be ready (with timeout)
echo "⏳ Waiting for Vault to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault status > /dev/null 2>&1; then
        echo "✅ Vault is ready!"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ ERROR: Vault failed to start after $max_attempts attempts (60 seconds)"
        echo "💡 Possible causes:"
        echo "   • Docker is not installed or running"
        echo "   • Vault container failed to start"
        echo "   • Port 8200 is already in use"
        echo ""
        echo "🔧 Troubleshooting:"
        echo "   • Check Docker: docker ps | grep vault"
        echo "   • Check logs: docker logs vault-dev"
        echo "   • Check port: lsof -i :8200"
        exit 1
    fi
    
    echo "   Vault not ready yet, waiting... (attempt $attempt/$max_attempts)"
    sleep 2
    attempt=$((attempt + 1))
done

# Enable KV v2 secrets engine at /secret/
echo "🔧 Enabling KV v2 secrets engine..."
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault secrets enable -path=secret -version=2 kv || echo "   KV engine already enabled"

# Store infrastructure authentication secrets
echo "🔐 Storing infrastructure authentication secrets..."

# Eureka admin credentials (used by all services for registration)
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault kv put secret/infrastructure/eureka/admin \
    username=chargeroute \
    password=Admin123!

echo "   ✅ Stored Eureka admin credentials at secret/infrastructure/eureka/admin"

# Config Server credentials (used by all services for config retrieval)
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault kv put secret/infrastructure/config-server/auth \
    username=configuser \
    password=configpass

echo "   ✅ Stored Config Server credentials at secret/infrastructure/config-server/auth"

# Create policy for Config Server (can read all infrastructure secrets)
echo "🛡️  Creating Vault policies..."

docker cp policies/config-server-policy.hcl vault-dev:/tmp/config-server-policy.hcl
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault policy write config-server /tmp/config-server-policy.hcl

echo "   ✅ Created 'config-server' policy"

# Create policy for services (can only read their needed secrets)
docker cp policies/services-policy.hcl vault-dev:/tmp/services-policy.hcl
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault policy write services /tmp/services-policy.hcl

echo "   ✅ Created 'services' policy"

# Verify secrets are stored correctly
echo "🧪 Verifying secrets storage..."

echo "   📋 Eureka admin credentials:"
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault kv get secret/infrastructure/eureka/admin

echo "   📋 Config Server credentials:"
docker exec -e VAULT_ADDR='http://localhost:8200' -e VAULT_TOKEN='dev-root-token' vault-dev vault kv get secret/infrastructure/config-server/auth

echo ""
echo "🎉 Vault initialization complete!"
echo ""
echo "💡 Usage:"
echo "   Vault UI: http://localhost:8200"
echo "   Token:    dev-root-token"
echo ""
echo "🔧 Infrastructure secrets stored:"
echo "   • secret/infrastructure/eureka/admin       (username: chargeroute, password: Admin123!)"
echo "   • secret/infrastructure/config-server/auth (username: configuser, password: configpass)"
echo ""
echo "📋 Policies created:"
echo "   • config-server: Read access to all infrastructure secrets"
echo "   • services:      Read access to Eureka and Config Server auth"
echo ""
echo "💡 OAuth2 client secrets are stored in config-data/ (filesystem)"
echo ""
echo "🚀 Next steps:"
echo "   1. Start Config Server (will connect to Vault for infrastructure secrets)"
echo "   2. Start Authorization Server (OAuth2 provider)"
echo "   3. Start all other services"
echo "   4. Services will retrieve Eureka/Config credentials from Vault, OAuth2 config from filesystem"