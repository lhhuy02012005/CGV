-- ==============================================================================
-- MARKETING SERVICE - SCHEMA MIGRATION V1
-- ==============================================================================
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_value NUMERIC(19, 2) NOT NULL,
    max_discount_amount NUMERIC(19, 2),
    min_order_value NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    applicable_tier VARCHAR(50) NOT NULL DEFAULT 'ALL' CHECK (applicable_tier IN ('ALL', 'MEMBER', 'SILVER', 'GOLD', 'PLATINUM')),
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    usage_limit INTEGER,
    max_uses_per_user INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT promotions_check CHECK (
        discount_value > 0 AND
        (discount_type <> 'PERCENT' OR discount_value <= 100) AND
        (max_discount_amount IS NULL OR max_discount_amount > 0) AND
        min_order_value >= 0 AND
        valid_from < valid_to AND
        (usage_limit IS NULL OR usage_limit > 0) AND
        max_uses_per_user > 0
    )
);

CREATE TABLE IF NOT EXISTS promotion_usages (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    booking_id UUID NOT NULL UNIQUE,
    discount_applied NUMERIC(12, 2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);
CREATE INDEX IF NOT EXISTS idx_promo_usage_user ON promotion_usages(promotion_id, user_id);
