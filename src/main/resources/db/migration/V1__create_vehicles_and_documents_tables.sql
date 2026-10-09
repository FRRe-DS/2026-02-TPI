-- Migration V1: Base tables for vehicles and documents (HU-06 support)

CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    vehicle_type VARCHAR(50),
    plate VARCHAR(20),
    make VARCHAR(60),
    model VARCHAR(60),
    model_year INTEGER,
    qualification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    driver_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicles_driver_id ON vehicles(driver_id);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    issuing_entity VARCHAR(100),
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    storage_reference VARCHAR(200),
    revision INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documents_vehicle_type_current ON documents(vehicle_id, document_type, is_current);
CREATE INDEX IF NOT EXISTS idx_documents_driver_id ON documents(driver_id);

