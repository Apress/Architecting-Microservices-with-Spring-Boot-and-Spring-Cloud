#!/bin/bash
TOKEN=$(curl -s -X POST "http://localhost:9000/oauth2/token" \
  -u "session-service-client:session-secret" \
  -d "grant_type=client_credentials&scope=service.internal" | \
  grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token: ${TOKEN:0:50}..."
echo ""
echo "Direct call to user service:"
curl -s -w "\nStatus: %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:63316/api/users/user-123"
echo ""
echo ""
echo "Call through API Gateway:"
curl -s -w "\nStatus: %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/user-management-service/api/users/user-123"
