ALTER TABLE outbox_booking_events ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_name VARCHAR(100);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_email VARCHAR(100);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(20);
CREATE TABLE IF NOT EXISTS booking_seat_claims (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    showtime_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    CONSTRAINT uk_booking_seat_claim_showtime_seat UNIQUE(showtime_id, seat_id)
);
-- Fail on conflicting existing allocations; never silently choose a winner or delete history.
INSERT INTO booking_seat_claims(id, booking_id, showtime_id, seat_id)
SELECT bs.id, bs.booking_id, bs.showtime_id, bs.seat_id
FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
WHERE b.status IN ('SEAT_RESERVED', 'PAYMENT_PENDING', 'CONFIRMED', 'USED');
