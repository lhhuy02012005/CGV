-- ==============================================================================
-- IDENTITY SERVICE - SCHEMA MIGRATION V1
-- ==============================================================================
CREATE TABLE IF NOT EXISTS membership_tiers (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    min_spend NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    membership_tier VARCHAR(50) NOT NULL REFERENCES membership_tiers(code),
    total_spend_ytd NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    version BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_connected_accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cgv_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_cgv_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_cgv_user_membership_tier ON users(membership_tier);
CREATE INDEX IF NOT EXISTS idx_cgv_user_phone ON users(phone);
