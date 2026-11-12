-- Enable UUID extension (if not already enabled)
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create the `regions` table
CREATE TABLE IF NOT EXISTS regions
(
    id
    UUID
    NOT
    NULL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    50
) NOT NULL,
    code VARCHAR
(
    50
), -- Included as referenced in uk_regions_code constraint
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    regional_supervisor_id UUID NOT NULL,

    -- Unique Constraints
    CONSTRAINT uk_regions_name UNIQUE
(
    name
),
    CONSTRAINT uk_regions_code UNIQUE
(
    code
),
    CONSTRAINT uk_regions_regional_supervisor_id UNIQUE
(
    regional_supervisor_id
),

    -- Foreign key to users table for regional_supervisor_id
    CONSTRAINT fk_regions_regional_supervisor
    FOREIGN KEY
(
    regional_supervisor_id
)
    REFERENCES users
(
    id
)
    );

-- Index on `active` column of `regions` table
CREATE INDEX IF NOT EXISTS idx_regions_active ON regions(active);

-- -----------------------------------------------------------------------------

-- 2. Alter `terminals` table to add `region_id` column and foreign key constraint
ALTER TABLE terminals
    ADD COLUMN IF NOT EXISTS region_id UUID NOT NULL;

-- Add Foreign Key constraint linking terminals to regions
ALTER TABLE terminals
    ADD CONSTRAINT fk_terminals_region
        FOREIGN KEY (region_id)
            REFERENCES regions (id);

-- 3. Create index for `region_id` on `terminals` table
CREATE INDEX IF NOT EXISTS idx_terminals_region_id ON terminals(region_id);