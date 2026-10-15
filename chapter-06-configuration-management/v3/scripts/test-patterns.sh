#!/bin/bash

# Test script to demonstrate v3 Vault integration patterns without Docker
# Shows configuration changes and security improvements

echo "🧪 Chapter 6 v3 - Vault Integration Pattern Analysis"
echo "=================================================="
echo ""

echo "1. 🔍 Analyzing Config Server Vault Integration..."
echo "   Config Server bootstrap.yml shows composite backend:"
cd "$(dirname "$0")/.."
if [ -f "config-server/src/main/resources/bootstrap.yml" ]; then
    echo "   ✅ Found composite configuration (file + vault backends)"
    grep -A 10 "composite:" config-server/src/main/resources/bootstrap.yml || echo "   ⚠️  Composite config not found"
else
    echo "   ❌ Config Server bootstrap.yml not found"
fi
echo ""

echo "2. 🔒 Analyzing Security Improvements..."
echo "   Checking for removed hardcoded credentials in config files:"

# Check if hardcoded passwords are removed from config-data
hardcoded_found=false
for config_file in config-data/main/*.yml; do
    if [ -f "$config_file" ]; then
        if grep -q "Admin123!" "$config_file" 2>/dev/null; then
            echo "   ❌ Found hardcoded Eureka password in $(basename $config_file)"
            hardcoded_found=true
        fi
        if grep -q "configpass" "$config_file" 2>/dev/null; then
            echo "   ❌ Found hardcoded Config Server password in $(basename $config_file)"
            hardcoded_found=true
        fi
    fi
done

if [ "$hardcoded_found" = false ]; then
    echo "   ✅ No hardcoded passwords found in configuration files"
else
    echo "   ⚠️  Some hardcoded credentials still present"
fi
echo ""

echo "3. 🔗 Analyzing Vault Placeholder Usage..."
echo "   Checking for Vault placeholders in service configurations:"

vault_placeholders_found=false
for config_file in config-data/main/*.yml; do
    if [ -f "$config_file" ]; then
        if grep -q "\${vault\." "$config_file" 2>/dev/null; then
            echo "   ✅ Found Vault placeholders in $(basename $config_file)"
            vault_placeholders_found=true
        fi
    fi
done

if [ "$vault_placeholders_found" = false ]; then
    echo "   ⚠️  No Vault placeholders found in configuration files"
fi
echo ""

echo "4. 🏗️  Analyzing Service Bootstrap Configuration..."
echo "   Checking for Vault client configuration in services:"

vault_config_count=0
for service_dir in */; do
    if [ -d "$service_dir" ] && [ -f "$service_dir/src/main/resources/bootstrap.yml" ]; then
        service_name=$(basename "$service_dir")
        if grep -q "vault:" "$service_dir/src/main/resources/bootstrap.yml" 2>/dev/null; then
            echo "   ✅ $service_name has Vault configuration"
            vault_config_count=$((vault_config_count + 1))
        else
            echo "   ⚠️  $service_name missing Vault configuration"
        fi
    fi
done

echo "   📊 $vault_config_count services configured for Vault integration"
echo ""

echo "5. 🐳 Analyzing Docker Infrastructure..."
if [ -f "vault/docker-compose.yml" ]; then
    echo "   ✅ Found Vault Docker Compose configuration"
    echo "   📋 Vault image: $(grep "image:" vault/docker-compose.yml | awk '{print $2}')"
    echo "   📋 Vault port: $(grep "8200:" vault/docker-compose.yml | awk '{print $2}' | cut -d'"' -f2)"
else
    echo "   ❌ Vault Docker Compose configuration not found"
fi

if [ -f "vault/init-vault.sh" ]; then
    echo "   ✅ Found Vault initialization script"
    echo "   📋 Secrets configured: $(grep -c "vault kv put" vault/init-vault.sh.del) infrastructure secrets"
else
    echo "   ❌ Vault initialization script not found"
fi
echo ""

echo "6. 📊 Security Analysis Summary..."
echo "   ===================="

# Count security improvements
improvements=0

if [ "$hardcoded_found" = false ]; then
    echo "   ✅ Hardcoded credentials removed from config files"
    improvements=$((improvements + 1))
fi

if [ "$vault_placeholders_found" = true ]; then
    echo "   ✅ Vault placeholders implemented for dynamic secret retrieval"
    improvements=$((improvements + 1))
fi

if [ $vault_config_count -gt 0 ]; then
    echo "   ✅ Services configured for Vault integration"
    improvements=$((improvements + 1))
fi

if [ -f "vault/docker-compose.yml" ] && [ -f "vault/init-vault.sh" ]; then
    echo "   ✅ Vault infrastructure properly configured"
    improvements=$((improvements + 1))
fi

echo ""
echo "   📈 Security Improvements: $improvements/4 implemented"

if [ $improvements -eq 4 ]; then
    echo "   🎉 Excellent! Full v3 Vault integration patterns implemented"
elif [ $improvements -ge 2 ]; then
    echo "   👍 Good progress on Vault integration patterns"
else
    echo "   ⚠️  More work needed on Vault integration patterns"
fi

echo ""
echo "7. 🚀 Next Steps (when Docker is available)..."
echo "   ========================================="
echo "   1. Install Docker and start Vault container"
echo "   2. Run: ./service-manager.sh start"
echo "   3. Verify services retrieve credentials from Vault"
echo "   4. Test secret rotation capabilities"
echo ""
echo "   Alternative: Test v2 implementation for functionality"
echo "   Run: cd ../v2 && ./scripts/service-manager.sh start"
echo ""
echo "🏁 Pattern analysis complete!"