ALTER TABLE outbox_payment_events ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP;
