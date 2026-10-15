#!/bin/bash
set -e

# This script initializes all databases and users for the ChargeRoute microservices
# It runs automatically when the PostgreSQL container starts for the first time

echo "=========================================="
echo "Initializing ChargeRoute databases..."
echo "=========================================="

# Function to create database and user
create_database() {
    local db_name=$1
    local db_user=$2
    local db_password=$3

    echo "Creating database: $db_name with user: $db_user"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        -- Create database
        CREATE DATABASE $db_name;

        -- Create user with password (to be overridden by Vault in production)
        CREATE USER $db_user WITH ENCRYPTED PASSWORD '$db_password';

        -- Grant all privileges on database to user
        GRANT ALL PRIVILEGES ON DATABASE $db_name TO $db_user;

        -- Connect to the new database and grant schema privileges
        \c $db_name
        GRANT ALL ON SCHEMA public TO $db_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $db_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $db_user;
EOSQL

    echo "✓ Database $db_name created successfully"
}

# Keycloak database
# Password will be retrieved from Vault: secret/keycloak/db
create_database "keycloak" "keycloak" "${KEYCLOAK_DB_PASSWORD:-keycloak_password}"

# User Management Service database
# Password will be retrieved from Vault: secret/user-management-service/db
create_database "userdb" "userservice" "${USER_DB_PASSWORD:-userpass123}"

# Charge Unit Management Service database
# Password will be retrieved from Vault: secret/charge-unit-management-service/db
create_database "chargeunitdb" "chargeunitservice" "${CHARGEUNIT_DB_PASSWORD:-chargeunitpass123}"

# Session Management Service database
# Password will be retrieved from Vault: secret/session-management-service/db
create_database "sessiondb" "sessionservice" "${SESSION_DB_PASSWORD:-sessionpass123}"

# Notification Management Service database
# Password will be retrieved from Vault: secret/notification-management-service/db
create_database "notificationdb" "notificationservice" "${NOTIFICATION_DB_PASSWORD:-notificationpass123}"

echo "=========================================="
echo "Database initialization completed!"
echo "=========================================="
echo ""
echo "Databases created:"
echo "  - keycloak         (user: keycloak)"
echo "  - userdb           (user: userservice)"
echo "  - chargeunitdb     (user: chargeunitservice)"
echo "  - sessiondb        (user: sessionservice)"
echo "  - notificationdb   (user: notificationservice)"
echo ""
echo "Connection details:"
echo "  Host: localhost"
echo "  Port: 5433 (external) / 5432 (internal)"
echo ""
echo "NOTE: Default passwords are used for local development."
echo "      In production, retrieve passwords from Vault at:"
echo "        - secret/keycloak/db"
echo "        - secret/user-management-service/db"
echo "        - secret/charge-unit-management-service/db"
echo "        - secret/session-management-service/db"
echo "        - secret/notification-management-service/db"
echo "=========================================="
