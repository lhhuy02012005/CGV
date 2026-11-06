DO $$
DECLARE
    first_day date := GREATEST(DATE '2026-11-04', (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date);
BEGIN
    IF (SELECT count(*) FROM movies WHERE id BETWEEN
        '11000000-0000-0000-0000-000000000001'::uuid AND
        '11000000-0000-0000-0000-000000000008'::uuid) <> 8 THEN
        RAISE EXCEPTION 'Expected eight demo movies';
    END IF;

    IF first_day <= DATE '2026-12-31' AND EXISTS (
        SELECT 1 FROM generate_series(first_day, DATE '2026-12-31', INTERVAL '1 day') day
        WHERE NOT EXISTS (SELECT 1 FROM showtimes s WHERE s.show_date = day::date AND s.status = 'SCHEDULED')
    ) THEN
        RAISE EXCEPTION 'Missing a demo screening day';
    END IF;

    IF EXISTS (
        SELECT 1 FROM showtimes a JOIN showtimes b
          ON a.room_id = b.room_id AND a.id < b.id
         AND a.status = 'SCHEDULED' AND b.status = 'SCHEDULED'
         AND a.start_time < b.end_time AND b.start_time < a.end_time
        WHERE a.show_date BETWEEN DATE '2026-11-04' AND DATE '2026-12-31'
    ) THEN
        RAISE EXCEPTION 'Overlapping screenings in one room';
    END IF;

    IF EXISTS (
        SELECT 1 FROM showtimes s JOIN movies m ON m.id = s.movie_id
        WHERE s.show_date BETWEEN DATE '2026-11-04' AND DATE '2026-12-31'
          AND s.show_date < m.release_date
    ) THEN
        RAISE EXCEPTION 'Screening before movie release';
    END IF;
END $$;

SELECT count(*) AS movies FROM movies WHERE id BETWEEN
    '11000000-0000-0000-0000-000000000001'::uuid AND
    '11000000-0000-0000-0000-000000000008'::uuid;
SELECT count(*) AS screenings, min(show_date) AS first_day, max(show_date) AS last_day
FROM showtimes WHERE show_date BETWEEN DATE '2026-11-04' AND DATE '2026-12-31';
