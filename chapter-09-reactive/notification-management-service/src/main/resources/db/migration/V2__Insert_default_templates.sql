-- Insert default notification templates

-- Session Started Template (EMAIL)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'session-started-email',
    'SESSION_STARTED',
    'EMAIL',
    'Charging Session Started',
    'Hello {{userName}},

Your charging session has started successfully.

Session Details:
- Session ID: {{sessionId}}
- Charge Unit: {{chargeUnitId}}
- Start Time: {{startTime}}
- Initial Battery Level: {{initialBatteryLevel}}%

You will receive updates as your session progresses.

ChargeRoute Team',
    true
);

-- Session Completed Template (EMAIL)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'session-completed-email',
    'SESSION_COMPLETED',
    'EMAIL',
    'Charging Session Completed',
    'Hello {{userName}},

Your charging session has been completed.

Session Summary:
- Session ID: {{sessionId}}
- Duration: {{duration}} minutes
- Energy Delivered: {{energyDelivered}} kWh
- Final Battery Level: {{finalBatteryLevel}}%
- Total Cost: ${{totalCost}}

Thank you for using ChargeRoute!',
    true
);

-- Payment Received Template (EMAIL)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'payment-received-email',
    'PAYMENT_RECEIVED',
    'EMAIL',
    'Payment Received - Thank You',
    'Hello {{userName}},

We have received your payment for charging session {{sessionId}}.

Payment Details:
- Amount: ${{amount}}
- Payment Method: {{paymentMethod}}
- Transaction Date: {{transactionDate}}

Receipt has been sent to your email.

ChargeRoute Team',
    true
);

-- Maintenance Alert Template (EMAIL)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'maintenance-alert-email',
    'MAINTENANCE_ALERT',
    'EMAIL',
    'Maintenance Alert - Charge Unit Unavailable',
    'Hello {{userName}},

The charge unit at {{location}} is currently undergoing maintenance.

Estimated completion: {{estimatedCompletion}}

Please use our app to find nearby available charging stations.

We apologize for any inconvenience.

ChargeRoute Team',
    true
);

-- Session Started Template (SMS)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'session-started-sms',
    'SESSION_STARTED',
    'SMS',
    NULL,
    'ChargeRoute: Your charging session {{sessionId}} has started. Initial battery: {{initialBatteryLevel}}%',
    true
);

-- Session Completed Template (SMS)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'session-completed-sms',
    'SESSION_COMPLETED',
    'SMS',
    NULL,
    'ChargeRoute: Session {{sessionId}} complete! Energy: {{energyDelivered}}kWh, Cost: ${{totalCost}}. Battery: {{finalBatteryLevel}}%',
    true
);

-- Charging Complete Template (PUSH)
INSERT INTO notification_templates (template_name, notification_type, channel, subject, body_template, active)
VALUES (
    'charging-complete-push',
    'CHARGING_COMPLETE',
    'PUSH',
    'Charging Complete!',
    'Your vehicle is fully charged and ready to go. Session {{sessionId}} delivered {{energyDelivered}}kWh.',
    true
);
