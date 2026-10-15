-- Session Management Service - Initial Schema
-- Creates tables for charging sessions and session metrics

-- Charging sessions table
CREATE TABLE charging_sessions (
    session_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    charge_unit_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    initial_battery_level INTEGER,
    final_battery_level INTEGER,
    energy_delivered_kwh DECIMAL(10, 2),
    total_cost DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Session metrics table
CREATE TABLE session_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    current_power_kw DECIMAL(10, 2),
    voltage DECIMAL(10, 2),
    current_amps DECIMAL(10, 2),
    battery_level INTEGER,
    temperature_celsius DECIMAL(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metric_session FOREIGN KEY (session_id)
        REFERENCES charging_sessions(session_id)
        ON DELETE CASCADE
);

-- Create indexes for commonly queried columns
CREATE INDEX idx_session_user_id ON charging_sessions(user_id);
CREATE INDEX idx_session_charge_unit_id ON charging_sessions(charge_unit_id);
CREATE INDEX idx_session_status ON charging_sessions(status);
CREATE INDEX idx_metric_session_id ON session_metrics(session_id);
CREATE INDEX idx_metric_recorded_at ON session_metrics(recorded_at);

-- Add comments for documentation
COMMENT ON TABLE charging_sessions IS 'Stores charging session information for EV charging platform';
COMMENT ON TABLE session_metrics IS 'Stores real-time metrics captured during charging sessions';
COMMENT ON COLUMN charging_sessions.status IS 'Session status: INITIATED, ACTIVE, COMPLETED, FAILED';
COMMENT ON COLUMN session_metrics.current_power_kw IS 'Current power delivery in kilowatts';
COMMENT ON COLUMN session_metrics.battery_level IS 'Current battery level percentage (0-100)';
