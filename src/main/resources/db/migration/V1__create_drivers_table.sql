CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    qualification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_qualification_status CHECK (qualification_status IN ('PENDING', 'APPROVED', 'OBSERVED', 'REJECTED', 'SUSPENDED'))
);

CREATE INDEX idx_drivers_identity_subject ON drivers(identity_subject);
