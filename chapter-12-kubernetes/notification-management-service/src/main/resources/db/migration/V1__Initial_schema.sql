-- Notification Management Service - Initial Schema
-- Creates tables for notifications and notification templates

-- Notifications table
CREATE TABLE notifications (
    notification_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notification templates table
CREATE TABLE notification_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(100) UNIQUE NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    body_template VARCHAR(5000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for commonly queried columns
CREATE INDEX idx_notification_user_id ON notifications(user_id);
CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_template_type ON notification_templates(notification_type);
CREATE INDEX idx_template_name ON notification_templates(template_name);

-- Add comments for documentation
COMMENT ON TABLE notifications IS 'Stores notification records for the EV charging platform';
COMMENT ON TABLE notification_templates IS 'Stores reusable notification templates';
COMMENT ON COLUMN notifications.status IS 'Notification status: PENDING, SENT, FAILED, RETRYING';
COMMENT ON COLUMN notifications.channel IS 'Notification channel: EMAIL, SMS, PUSH, IN_APP';
COMMENT ON COLUMN notification_templates.body_template IS 'Template with placeholders like {{userName}}, {{sessionId}}';
