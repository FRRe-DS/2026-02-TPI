-- =============================================================
-- Migration V4: Availability intents, audit log, outbox,
--               and idempotency records (HU-10)
-- =============================================================

-- -------------------------------------------------------------
-- availability_intents
-- One row per driver. The UNIQUE constraint on driver_id enforces
-- the "one intent per driver" business rule at the DB level.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS availability_intents (
    id              UUID        PRIMARY KEY,
    driver_id       UUID        NOT NULL,
    vehicle_id      UUID        NOT NULL,

    -- Requested state (what the driver wants)
    connected       BOOLEAN     NOT NULL DEFAULT FALSE,
    available       BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Synchronisation state with M4
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        -- PENDING | RETRYING | APPLIED | FAILED

    -- Optimistic versioning for M4 de-duplication
    desired_version BIGINT      NOT NULL DEFAULT 1,
    applied_version BIGINT,

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    synced_at       TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT availability_intents_driver_id_uq UNIQUE (driver_id),
    CONSTRAINT availability_intents_connected_available_ck
        CHECK (connected = TRUE OR available = FALSE),
        -- connected=false → available must be false
    CONSTRAINT availability_intents_status_ck
        CHECK (status IN ('PENDING', 'RETRYING', 'APPLIED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_avail_intents_driver_id
    ON availability_intents (driver_id);

CREATE INDEX IF NOT EXISTS idx_avail_intents_status
    ON availability_intents (status)
    WHERE status IN ('PENDING', 'RETRYING');

-- -------------------------------------------------------------
-- audit_log
-- Append-only log of relevant domain events.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID        PRIMARY KEY,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       UUID        NOT NULL,
    operation       VARCHAR(80) NOT NULL,
    actor           VARCHAR(200),
    correlation_id  VARCHAR(100),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    changes         TEXT
);

-- Compatibilidad si audit_log ya existía previamente en V1/V2
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS entity_type     VARCHAR(80);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS entity_id       UUID;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS operation       VARCHAR(80);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS actor           VARCHAR(200);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS correlation_id  VARCHAR(100);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS occurred_at     TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS changes         TEXT;

CREATE INDEX IF NOT EXISTS idx_audit_log_entity
    ON audit_log (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at
    ON audit_log (occurred_at);

-- -------------------------------------------------------------
-- outbox
-- Transactional outbox for HTTP messages to M4 (E1).
-- Expanded to events in E2.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox (
    id              UUID         PRIMARY KEY,
    destination     VARCHAR(200) NOT NULL,   -- e.g. M4 availability-intent endpoint
    message_type    VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(80)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    aggregate_version BIGINT     NOT NULL,
    payload         TEXT         NOT NULL,   -- JSON
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
        -- PENDING | PROCESSING | SENT | FAILED
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,

    CONSTRAINT outbox_status_ck
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next
    ON outbox (status, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRYING');

-- -------------------------------------------------------------
-- idempotency_records
-- Stores the result of mutable operations keyed by
-- (principal, method, normalized_path, idempotency_key).
-- Retention: 7 days (enforced by a scheduled cleanup job).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency_records (
    id              UUID         PRIMARY KEY,
    principal       VARCHAR(200) NOT NULL,
    http_method     VARCHAR(10)  NOT NULL,
    path            VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,   -- SHA-256 of canonical body
    response_status INTEGER      NOT NULL,
    response_body   TEXT,                   -- minimal, no tokens
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT idempotency_uq
        UNIQUE (principal, http_method, path, idempotency_key)
);

-- Compatibilidad si idempotency_records ya existía previamente en V1/V2
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS principal       VARCHAR(200);
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS http_method     VARCHAR(10);
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS path            VARCHAR(500);
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(200);
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS request_hash    VARCHAR(64);
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS response_status INTEGER;
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS response_body   TEXT;
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS created_at      TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS expires_at      TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at
    ON idempotency_records (expires_at);