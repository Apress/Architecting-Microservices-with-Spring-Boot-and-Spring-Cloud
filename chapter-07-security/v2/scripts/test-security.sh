#!/bin/bash

# OAuth2 Security Testing Script for Chapter 7 v2 - Keycloak
# Tests authentication and authorization flows

KEYCLOAK="http://localhost:9000"
KEYCLOAK_REALM="ev-charging"
TOKEN_ENDPOINT="$KEYCLOAK/realms/$KEYCLOAK_REALM/protocol/openid-connect/token"
GATEWAY="http://localhost:8080"

echo "=========================================="
echo "Chapter 7 v2 - OAuth2 Security Testing (Keycloak)"
echo "=========================================="
echo ""

# Test 1: Get token with client credentials (simulating service-to-service)
echo "1. Testing Client Credentials Flow (Service-to-Service)"
echo "-------------------------------------------"
echo "Getting access token for session-service-client..."

TOKEN_RESPONSE=$(curl -s -X POST "$TOKEN_ENDPOINT" \
  -u "session-service-client:session-secret" \
  -d "grant_type=client_credentials")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Failed to get access token"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✓ Access token obtained (service.internal scope)"
echo "Token (first 50 chars): ${ACCESS_TOKEN:0:50}..."
echo ""

# Test 2: Call protected User Service endpoint
echo "2. Testing Protected Endpoint Access"
echo "-------------------------------------------"
echo "Calling User Service GET /user-management-service/api/users/user123 with token..."

USER_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/user-management-service/api/users/user123")

HTTP_STATUS=$(echo "$USER_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)

if [ "$HTTP_STATUS" = "200" ]; then
    echo "✓ Successfully accessed protected endpoint"
    echo "$USER_RESPONSE" | sed '/HTTP_STATUS/d'
else
    echo "❌ Failed to access endpoint (Status: $HTTP_STATUS)"
fi
echo ""

# Test 3: Try accessing without token
echo "3. Testing Unauthorized Access (No Token)"
echo "-------------------------------------------"
echo "Calling User Service without token..."

UNAUTH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "$GATEWAY/user-management-service/api/users/user123")

HTTP_STATUS=$(echo "$UNAUTH_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)

if [ "$HTTP_STATUS" = "401" ]; then
    echo "✓ Correctly rejected unauthorized request (401)"
else
    echo "❌ Unexpected status: $HTTP_STATUS (expected 401)"
fi
echo ""

# Test 4: Try accessing admin endpoint with non-admin token
echo "4. Testing Insufficient Scope (Forbidden)"
echo "-------------------------------------------"
echo "Trying to access admin endpoint with service.internal scope..."

FORBIDDEN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/user-management-service/api/users/admin/config")

HTTP_STATUS=$(echo "$FORBIDDEN_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)

if [ "$HTTP_STATUS" = "403" ]; then
    echo "✓ Correctly rejected insufficient scope (403)"
else
    echo "⚠ Unexpected status: $HTTP_STATUS (expected 403)"
fi
echo ""

# Test 5: Get admin token and access admin endpoint
echo "5. Testing Admin Access with Correct Scope"
echo "-------------------------------------------"
echo "Getting admin token..."

ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "$TOKEN_ENDPOINT" \
  -u "admin-client:admin-secret" \
  -d "grant_type=client_credentials")

ADMIN_TOKEN=$(echo $ADMIN_TOKEN_RESPONSE | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Failed to get admin token"
else
    echo "✓ Admin token obtained (admin.all scope)"

    echo "Calling admin endpoint with admin token..."
    ADMIN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$GATEWAY/user-management-service/api/users/admin/config")

    HTTP_STATUS=$(echo "$ADMIN_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)

    if [ "$HTTP_STATUS" = "200" ]; then
        echo "✓ Successfully accessed admin endpoint"
    else
        echo "❌ Failed to access admin endpoint (Status: $HTTP_STATUS)"
    fi
fi
echo ""

# Test 6: Health check endpoints (public)
echo "6. Testing Public Endpoints (No Auth Required)"
echo "-------------------------------------------"
echo "Checking health endpoints..."

HEALTH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "$GATEWAY/actuator/health")

HTTP_STATUS=$(echo "$HEALTH_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)

if [ "$HTTP_STATUS" = "200" ]; then
    echo "✓ Public health endpoint accessible without token"
else
    echo "❌ Health endpoint failed (Status: $HTTP_STATUS)"
fi
echo ""

echo "=========================================="
echo "OAuth2 Security Testing Complete"
echo "=========================================="
