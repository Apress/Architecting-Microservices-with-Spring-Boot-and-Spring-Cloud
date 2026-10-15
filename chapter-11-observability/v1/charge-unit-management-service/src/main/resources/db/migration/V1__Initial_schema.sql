-- Charge Unit Management Service - Initial Schema
-- Creates table for charge units

-- Charge units table
CREATE TABLE charge_units (
    charge_unit_id VARCHAR(50) PRIMARY KEY,
    station_name VARCHAR(200) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    connector_type VARCHAR(20) NOT NULL,
    max_power_kw INTEGER NOT NULL,
    current_power_kw INTEGER,
    status VARCHAR(30) NOT NULL,
    price_per_kwh DECIMAL(10, 2) NOT NULL,
    last_maintenance_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for commonly queried columns
CREATE INDEX idx_charge_unit_status ON charge_units(status);
CREATE INDEX idx_charge_unit_connector_type ON charge_units(connector_type);

-- Add comments for documentation
COMMENT ON TABLE charge_units IS 'Stores charge unit information for the EV charging platform';
COMMENT ON COLUMN charge_units.status IS 'Charge unit status: AVAILABLE, OCCUPIED, OFFLINE, MAINTENANCE';
COMMENT ON COLUMN charge_units.connector_type IS 'Connector type: CCS, CHAdeMO, Type2, Tesla';
