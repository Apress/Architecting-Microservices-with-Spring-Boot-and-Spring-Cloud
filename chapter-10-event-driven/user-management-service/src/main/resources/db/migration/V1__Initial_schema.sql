-- User Management Service - Initial Schema
-- Creates tables for users and their preferences

-- Users table
CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    membership_type VARCHAR(50),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for commonly queried columns
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_status ON users(status);

-- User preferences table (one-to-one with users)
CREATE TABLE user_preferences (
    user_id VARCHAR(50) PRIMARY KEY,
    preferred_connector_type VARCHAR(50),
    notifications_enabled BOOLEAN DEFAULT true,
    notification_channel VARCHAR(20) DEFAULT 'EMAIL',
    auto_charge_enabled BOOLEAN DEFAULT false,
    max_charge_amount DECIMAL(10, 2),
    preferred_payment_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information for the EV charging platform';
COMMENT ON TABLE user_preferences IS 'Stores user-specific preferences for charging, notifications, and payments';

COMMENT ON COLUMN users.status IS 'User account status: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION';
COMMENT ON COLUMN users.membership_type IS 'Membership tier: BASIC, PREMIUM, ENTERPRISE';
COMMENT ON COLUMN user_preferences.notification_channel IS 'Preferred notification channel: EMAIL, SMS, PUSH, ALL';
