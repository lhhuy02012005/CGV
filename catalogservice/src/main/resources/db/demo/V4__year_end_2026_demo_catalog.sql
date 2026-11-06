-- Dev demo only: film details and posters are real; screening dates are invented, not CGV listings.
UPDATE movies
SET release_date = CASE id
        WHEN '11000000-0000-0000-0000-000000000001' THEN DATE '2022-12-16'
        WHEN '11000000-0000-0000-0000-000000000002' THEN DATE '2023-07-21'
        WHEN '11000000-0000-0000-0000-000000000003' THEN DATE '2023-04-14'
        WHEN '11000000-0000-0000-0000-000000000004' THEN DATE '2024-03-01'
        WHEN '11000000-0000-0000-0000-000000000005' THEN DATE '2024-03-29'
    END,
    end_date = DATE '2026-12-31',
    showing_status = 'NOW_SHOWING',
    poster_url = CASE id
        WHEN '11000000-0000-0000-0000-000000000003' THEN 'https://image.tmdb.org/t/p/w500/ksQ8uNgoWsVH6a0oPB6zx08pOwU.jpg'
        WHEN '11000000-0000-0000-0000-000000000004' THEN 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg'
        WHEN '11000000-0000-0000-0000-000000000005' THEN 'https://image.tmdb.org/t/p/w500/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg'
        ELSE poster_url
    END,
    backdrop_url = CASE id
        WHEN '11000000-0000-0000-0000-000000000003' THEN 'https://image.tmdb.org/t/p/w1280/zZnsfR6BRqtxyLyAhsi2TMukEBx.jpg'
        WHEN '11000000-0000-0000-0000-000000000004' THEN 'https://image.tmdb.org/t/p/w1280/uhUO7vQQKvCTfQWubOt5MAKokbL.jpg'
        WHEN '11000000-0000-0000-0000-000000000005' THEN 'https://image.tmdb.org/t/p/w1280/veIyxxi5Gs8gvztLEW1Ysb8rrzs.jpg'
        ELSE backdrop_url
    END,
    updated_at = now()
WHERE id IN (
    '11000000-0000-0000-0000-000000000001',
    '11000000-0000-0000-0000-000000000002',
    '11000000-0000-0000-0000-000000000003',
    '11000000-0000-0000-0000-000000000004',
    '11000000-0000-0000-0000-000000000005'
);

INSERT INTO movies (
    id, title, original_title, synopsis, director, language, subtitle,
    supported_modes, is_featured, age_rating, duration_minutes, release_date,
    end_date, showing_status, poster_url, backdrop_url, status, created_at, updated_at
)
VALUES
    ('11000000-0000-0000-0000-000000000006', 'Whalefall', 'Whalefall',
     'Một thợ lặn trẻ phải tìm đường sống sót sau khi bị cá voi nuốt chửng.',
     'Brian Duffield', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', true, NULL, 106,
     DATE '2026-10-16', DATE '2026-12-31', 'NOW_SHOWING',
     'https://resizing.flixster.com/id_lWLGnPpOhw3v7nN2joMOU3PE=/206x305/v2/https://resizing.flixster.com/wkyovIOKHfAiMl1v4nSk6__GsJs=/ems.cHJkLWVtcy1hc3NldHMvbW92aWVzLzdjNzc1YmE5LTY3ZTItNDA5Yi05YzI3LWI0MjM5YzJiZTk0ZS5qcGc=',
     'https://resizing.flixster.com/id_lWLGnPpOhw3v7nN2joMOU3PE=/206x305/v2/https://resizing.flixster.com/wkyovIOKHfAiMl1v4nSk6__GsJs=/ems.cHJkLWVtcy1hc3NldHMvbW92aWVzLzdjNzc1YmE5LTY3ZTItNDA5Yi05YzI3LWI0MjM5YzJiZTk0ZS5qcGc=',
     'ACTIVE', now(), now()),
    ('11000000-0000-0000-0000-000000000007', 'Đấu Trường Sinh Tử: Bình Minh Trên Lễ Gặt',
     'The Hunger Games: Sunrise on the Reaping',
     'Chàng trai trẻ Haymitch Abernathy bước vào Kỳ Huyết Chiến thứ 50 tại Panem.',
     'Francis Lawrence', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', true, NULL, 158,
     DATE '2026-11-20', DATE '2026-12-31', 'COMING_SOON',
     'https://resizing.flixster.com/T_EH2BoDF7-8AG_MktHoFUzG4tA=/206x305/v2/https://resizing.flixster.com/S01KQAzBmNdHkNrk3y78_BTlYGY=/ems.cHJkLWVtcy1hc3NldHMvbW92aWVzL2ViMWQyZGUzLTc3ZmYtNDBlMy05ZWFmLTA1Mzk4MmRiM2Q4My5qcGc=',
     'https://resizing.flixster.com/T_EH2BoDF7-8AG_MktHoFUzG4tA=/206x305/v2/https://resizing.flixster.com/S01KQAzBmNdHkNrk3y78_BTlYGY=/ems.cHJkLWVtcy1hc3NldHMvbW92aWVzL2ViMWQyZGUzLTc3ZmYtNDBlMy05ZWFmLTA1Mzk4MmRiM2Q4My5qcGc=',
     'ACTIVE', now(), now()),
    ('11000000-0000-0000-0000-000000000008', 'Avengers: Doomsday', 'Avengers: Doomsday',
     'Các nhóm siêu anh hùng đối mặt với hiểm họa Doctor Doom.',
     'Anthony Russo, Joe Russo', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', true, NULL, 165,
     DATE '2026-12-18', DATE '2026-12-31', 'COMING_SOON',
     'https://image.tmdb.org/t/p/w500/cWXtJhrlruF8CeYuaBGE8vdj3Q9.jpg',
     'https://image.tmdb.org/t/p/w500/cWXtJhrlruF8CeYuaBGE8vdj3Q9.jpg',
     'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
VALUES
    ('11000000-0000-0000-0000-000000000006', 3),
    ('11000000-0000-0000-0000-000000000006', 7),
    ('11000000-0000-0000-0000-000000000007', 1),
    ('11000000-0000-0000-0000-000000000007', 7),
    ('11000000-0000-0000-0000-000000000008', 1),
    ('11000000-0000-0000-0000-000000000008', 2)
ON CONFLICT (movie_id, genre_id) DO NOTHING;

WITH days AS (
    SELECT day::date AS show_date
    FROM generate_series(
        GREATEST(DATE '2026-11-04', (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date),
        DATE '2026-12-31', INTERVAL '1 day'
    ) AS day
), slots AS (
    SELECT day.show_date, room.id AS room_id, room.format, room.total_seats,
           film.id AS movie_id, film.language, film.subtitle, film.duration_minutes,
           ((day.show_date + CASE slot.number WHEN 0 THEN TIME '10:30' ELSE TIME '17:30' END)
             AT TIME ZONE 'Asia/Ho_Chi_Minh') AS starts_at,
           slot.number AS slot_number
    FROM days day
    CROSS JOIN rooms room
    CROSS JOIN generate_series(0, 1) AS slot(number)
    CROSS JOIN LATERAL (
        SELECT movie.id, movie.language, movie.subtitle, movie.duration_minutes
        FROM movies movie
        WHERE movie.id BETWEEN '11000000-0000-0000-0000-000000000001'::uuid
                           AND '11000000-0000-0000-0000-000000000008'::uuid
          AND movie.release_date <= day.show_date AND movie.end_date >= day.show_date
          AND (room.format = '2D'
               OR (room.format = '4DX' AND movie.id = '11000000-0000-0000-0000-000000000005')
               OR (room.format = 'IMAX' AND movie.id IN (
                   '11000000-0000-0000-0000-000000000001',
                   '11000000-0000-0000-0000-000000000002',
                   '11000000-0000-0000-0000-000000000004',
                   '11000000-0000-0000-0000-000000000007',
                   '11000000-0000-0000-0000-000000000008'
               )))
        ORDER BY md5(room.id::text || day.show_date::text || slot.number::text || movie.id::text)
        LIMIT 1
    ) film
    WHERE room.id IN (
        '01000000-0000-0000-0000-000000000001',
        '01000000-0000-0000-0000-000000000002',
        '02000000-0000-0000-0000-000000000001',
        '02000000-0000-0000-0000-000000000002',
        '03000000-0000-0000-0000-000000000001',
        '04000000-0000-0000-0000-000000000001'
    ) AND room.status = 'ACTIVE'
), proposals AS (
    SELECT md5('year-end-2026-demo:' || room_id::text || show_date::text || slot_number::text)::uuid AS id,
           movie_id, room_id, show_date, starts_at,
           starts_at + make_interval(mins => duration_minutes) AS ends_at,
           language, subtitle, format, total_seats
    FROM slots
)
INSERT INTO showtimes (
    id, movie_id, room_id, show_date, start_time, end_time, language,
    subtitle_language, format, viewing_mode, base_price, available_seats,
    status, created_at, updated_at
)
SELECT proposal.id, proposal.movie_id, proposal.room_id, proposal.show_date,
       proposal.starts_at, proposal.ends_at, proposal.language, proposal.subtitle,
       proposal.format, 'SUBTITLED',
       CASE proposal.format WHEN 'IMAX' THEN 180000 WHEN '4DX' THEN 160000 ELSE 110000 END,
       proposal.total_seats, 'SCHEDULED', now(), now()
FROM proposals proposal
WHERE proposal.starts_at > now() + INTERVAL '30 minutes'
  AND NOT EXISTS (
      SELECT 1 FROM showtimes booked
      WHERE booked.room_id = proposal.room_id AND booked.status = 'SCHEDULED'
        AND booked.start_time < proposal.ends_at AND booked.end_time > proposal.starts_at
  )
ON CONFLICT (id) DO NOTHING;

