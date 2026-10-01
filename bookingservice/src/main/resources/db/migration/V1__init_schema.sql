-- ==============================================================================
-- BOOKING SERVICE - SCHEMA MIGRATION V1
-- ==============================================================================
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255),
    showtime_id UUID NOT NULL,
    promotion_id UUID,
    total_base_amount NUMERIC(19, 2) NOT NULL,
    discount_amount NUMERIC(19, 2) DEFAULT 0.00,
    final_amount NUMERIC(19, 2) NOT NULL,
    payment_deadline TIMESTAMP,
    qr_code_url VARCHAR(500),
    cancelled_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PAYMENT_PENDING' CHECK (status IN ('SEAT_RESERVED', 'PAYMENT_PENDING', 'CONFIRMED', 'USED', 'CANCELLED', 'CANCELLED_DUE_TO_MAINTENANCE', 'REFUNDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_seats (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id UUID NOT NULL,
    showtime_id UUID NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    seat_label VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS seat_locks (
    id UUID PRIMARY KEY,
    showtime_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    status VARCHAR(50) NOT NULL CHECK (status IN ('LOCKED', 'RELEASED', 'CONVERTED_TO_BOOKING')),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cgv_showtime_seat UNIQUE (showtime_id, seat_id)
);

CREATE TABLE IF NOT EXISTS outbox_booking_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    is_published BOOLEAN DEFAULT FALSE,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cgv_booking_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_cgv_booking_showtime_id ON bookings(showtime_id);
CREATE INDEX IF NOT EXISTS idx_cgv_booking_status ON bookings(status);
