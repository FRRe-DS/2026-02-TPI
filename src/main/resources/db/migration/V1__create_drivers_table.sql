-- ==============================================================================
-- V1: Creación de la tabla principal de conductores (drivers) para M3
-- Arquitectura de Microservicio - TPI Desarrollo de Software 2026
-- ==============================================================================

CREATE TABLE IF NOT EXISTS drivers (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(128) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    dni VARCHAR(32) NOT NULL,
    birth_date DATE,
    address VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(150) NOT NULL,
    status VARCHAR(32) NOT NULL,
    qualification_reason VARCHAR(500),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drivers_identity_subject UNIQUE (identity_subject)
);

CREATE INDEX IF NOT EXISTS idx_drivers_identity_subject ON drivers (identity_subject);
CREATE INDEX IF NOT EXISTS idx_drivers_status ON drivers (status);

COMMENT ON TABLE drivers IS 'Tabla de perfiles de conductores propia del microservicio M3 (RNF-05)';
COMMENT ON COLUMN drivers.identity_subject IS 'Identificador del sujeto en M1 / Keycloak (sub claim)';
COMMENT ON COLUMN drivers.status IS 'Estado de habilitación: PENDING, APPROVED, OBSERVED, REJECTED, SUSPENDED';
COMMENT ON COLUMN drivers.version IS 'Control de concurrencia optimista y control de versiones';
