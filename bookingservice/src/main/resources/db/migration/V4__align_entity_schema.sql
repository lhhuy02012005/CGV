-- Align Instant columns created by historical V1 with Hibernate's TIMESTAMPTZ mapping.
-- Existing timestamp-without-zone values are interpreted in UTC (the local DB timezone).
DO $$
DECLARE c record;
BEGIN
    FOR c IN SELECT table_name, column_name FROM information_schema.columns
             WHERE table_schema = 'public' AND data_type = 'timestamp without time zone'
               AND table_name IN ('bookings', 'booking_seats', 'outbox_booking_events')
    LOOP
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE ''UTC''',
                       c.table_name, c.column_name, c.column_name);
    END LOOP;
END $$;
