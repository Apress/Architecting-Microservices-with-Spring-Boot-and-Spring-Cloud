#!/bin/bash

# Startup script for Config Server with Vault integration

echo "🚀 Starting Config Server with Vault integration..."
echo ""

# Set Vault token environment variable
export VAULT_TOKEN=dev-root-token

echo "✅ Environment variables set:"
echo "   VAULT_TOKEN=$VAULT_TOKEN"
echo ""

# Check if Vault is accessible
if curl -f -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo "✅ Vault is accessible at http://localhost:8200"
else
    echo "❌ WARNING: Vault is not accessible at http://localhost:8200"
    echo "   Make sure Vault is running: cd ../vault && docker-compose up -d"
    echo ""
fi

echo "📦 Starting Config Server on port 8888..."
echo ""

# Run the Config Server with the Vault token
./mvnw spring-boot:run
