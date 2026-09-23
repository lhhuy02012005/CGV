-- ==============================================================================
-- CGV CINEMA PLATFORM - MASTER SEED DATA SCRIPT
-- ==============================================================================
-- Cách chạy:
-- Cách 1 (qua Docker):
--   docker exec -i cgv-postgres psql -U postgres -d cgv < scripts/seed-data.sql
--
-- Cách 2 (trực tiếp psql):
--   psql -h localhost -p 5432 -U postgres -d cgv -f scripts/seed-data.sql
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. HẠNG THÀNH VIÊN (MEMBERSHIP TIERS)
-- ------------------------------------------------------------------------------
INSERT INTO membership_tiers (code, name, min_spend, description, created_at, updated_at)
VALUES
    ('MEMBER', 'Thành viên Tiêu chuẩn', 0.00, 'Hạng mặc định khi đăng ký tài khoản CGV', NOW(), NOW()),
    ('SILVER', 'Thành viên Bạc', 2000000.00, 'Chi tiêu từ 2 triệu/năm, tích 5% điểm thưởng', NOW(), NOW()),
    ('GOLD', 'Thành viên Vàng', 5000000.00, 'Chi tiêu từ 5 triệu/năm, tích 7% điểm, miễn phí bắp nước sinh nhật', NOW(), NOW()),
    ('PLATINUM', 'Thành viên Bạch Kim', 15000000.00, 'Chi tiêu từ 15 triệu/năm, tích 10% điểm, phòng chờ VIP & vé xem phim miễn phí', NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name, min_spend = EXCLUDED.min_spend, description = EXCLUDED.description;

-- ------------------------------------------------------------------------------
-- 2. TÀI KHOẢN NGƯỜI DÙNG MẪU (USERS)
-- ------------------------------------------------------------------------------
INSERT INTO users (id, email, full_name, membership_tier, total_spend_ytd, version, created_at, updated_at)
VALUES
    ('usr-test-001', 'nguyen.an@cgv.vn', 'Nguyễn Văn An', 'GOLD', 5500000.00, 1, NOW(), NOW()),
    ('usr-test-002', 'tran.bich@cgv.vn', 'Trần Thị Bích', 'PLATINUM', 16000000.00, 1, NOW(), NOW()),
    ('usr-test-003', 'le.thang@cgv.vn', 'Lê Hoàng Thắng', 'SILVER', 2800000.00, 1, NOW(), NOW()),
    ('usr-test-004', 'pham.dung@cgv.vn', 'Phạm Tiến Dũng', 'MEMBER', 450000.00, 1, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email, full_name = EXCLUDED.full_name, membership_tier = EXCLUDED.membership_tier, total_spend_ytd = EXCLUDED.total_spend_ytd;

-- ------------------------------------------------------------------------------
-- 3. KHU VỰC ĐỊA LÝ (REGIONS)
-- ------------------------------------------------------------------------------
INSERT INTO regions (id, name, slug)
VALUES
    (1, 'Hồ Chí Minh', 'ho-chi-minh'),
    (2, 'Hà Nội', 'ha-noi'),
    (3, 'Đà Nẵng', 'da-nang'),
    (4, 'Cần Thơ', 'can-tho'),
    (5, 'Bình Dương', 'binh-duong')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, slug = EXCLUDED.slug;

-- Điều chỉnh sequence của regions nếu cần
SELECT setval('regions_id_seq', (SELECT MAX(id) FROM regions));

-- ------------------------------------------------------------------------------
-- 4. LOẠI GHẾ (SEAT TYPES)
-- ------------------------------------------------------------------------------
INSERT INTO seat_types (name, surcharge, description)
VALUES
    ('NORMAL', 0.00, 'Ghế tiêu chuẩn với góc nhìn tiêu chuẩn'),
    ('VIP', 15000.00, 'Ghế VIP khu vực trung tâm xem phim tốt nhất'),
    ('SWEETBOX', 30000.00, 'Ghế đôi vách ngăn riêng tư dành cho cặp đôi')
ON CONFLICT (name) DO UPDATE
SET surcharge = EXCLUDED.surcharge, description = EXCLUDED.description;

-- ------------------------------------------------------------------------------
-- 5. CỤM RẠP CGV (CINEMAS - CÓ TỌA ĐỘ GPS THỰC TẾ)
-- ------------------------------------------------------------------------------
INSERT INTO cinemas (id, region_id, name, address, phone, opening_hours, latitude, longitude, status, created_at, updated_at)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 1, 'CGV Vincom Đồng Khởi', 'Tầng 3, TTTM Vincom Center, 72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM', '1900 6017', '08:00 - 24:00', 10.777823, 106.702515, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000002', 1, 'CGV Crescent Mall', 'Tầng 5, Crescent Mall, Đại Lộ Nguyễn Văn Linh, Tân Phú, Quận 7, TP.HCM', '1900 6017', '08:30 - 23:30', 10.729352, 106.718818, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000003', 1, 'CGV Vạn Hạnh Mall', 'Tầng 6, Vạn Hạnh Mall, 11 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM', '1900 6017', '08:00 - 23:30', 10.768652, 106.669894, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000004', 1, 'CGV Vincom Landmark 81', 'Tầng B1, Vincom Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM', '1900 6017', '08:00 - 24:00', 10.795034, 106.721867, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000005', 2, 'CGV Vincom Bà Triệu', 'Tầng 6, Vincom Center Hà Nội, 191 Bà Triệu, Hai Bà Trưng, Hà Nội', '1900 6017', '08:30 - 23:30', 21.011832, 105.849925, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000006', 2, 'CGV Vincom Center Nguyễn Chí Thanh', 'Số 54A Nguyễn Chí Thanh, Láng Thượng, Đống Đa, Hà Nội', '1900 6017', '08:30 - 23:30', 21.023812, 105.811823, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, address = EXCLUDED.address, latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, status = EXCLUDED.status;

-- ------------------------------------------------------------------------------
-- 6. TIỆN ÍCH RẠP (CINEMA AMENITIES)
-- ------------------------------------------------------------------------------
INSERT INTO cinema_amenities (cinema_id, amenity)
VALUES
    -- Vincom Đồng Khởi
    ('c1000000-0000-0000-0000-000000000001', 'IMAX'),
    ('c1000000-0000-0000-0000-000000000001', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000001', 'DOLBY_ATMOS'),
    ('c1000000-0000-0000-0000-000000000001', 'PARKING'),
    ('c1000000-0000-0000-0000-000000000001', 'FOOD_COURT'),
    -- Crescent Mall
    ('c1000000-0000-0000-0000-000000000002', '4D'),
    ('c1000000-0000-0000-0000-000000000002', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000002', 'PARKING'),
    -- Vạn Hạnh Mall
    ('c1000000-0000-0000-0000-000000000003', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000003', 'PARKING'),
    ('c1000000-0000-0000-0000-000000000003', 'FOOD_COURT'),
    -- Landmark 81
    ('c1000000-0000-0000-0000-000000000004', 'IMAX'),
    ('c1000000-0000-0000-0000-000000000004', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000004', 'PARKING')
ON CONFLICT (cinema_id, amenity) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 7. PHÒNG CHIẾU (ROOMS)
-- ------------------------------------------------------------------------------
INSERT INTO rooms (id, cinema_id, name, format, total_seats, row_count, column_count, status)
VALUES
    -- Rạp Đồng Khởi
    ('01000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'Cinema 1 (IMAX Laser)', 'IMAX', 80, 8, 10, 'ACTIVE'),
    ('01000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'Cinema 2 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    -- Rạp Crescent Mall
    ('02000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000002', 'Cinema 1 (4DX)', '4D', 80, 8, 10, 'ACTIVE'),
    ('02000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002', 'Cinema 2 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    -- Rạp Vạn Hạnh Mall
    ('03000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000003', 'Cinema 1 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    -- Rạp Landmark 81
    ('04000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000004', 'Cinema 1 (IMAX)', 'IMAX', 80, 8, 10, 'ACTIVE')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, format = EXCLUDED.format, total_seats = EXCLUDED.total_seats, status = EXCLUDED.status;

-- ------------------------------------------------------------------------------
-- 8. TỰ ĐỘNG SINH GHẾ CHO TỪNG PHÒNG CHIẾU (SEATS)
--    Hàng A, B, C: Ghế NORMAL
--    Hàng D, E, F, G: Ghế VIP
--    Hàng H: Ghế SWEETBOX
-- ------------------------------------------------------------------------------
INSERT INTO seats (id, room_id, row_char, seat_number, seat_type_name, is_active)
SELECT 
    gen_random_uuid(),
    r.id,
    row_letter,
    col_num,
    CASE 
        WHEN row_letter IN ('A', 'B', 'C') THEN 'NORMAL'
        WHEN row_letter IN ('D', 'E', 'F', 'G') THEN 'VIP'
        ELSE 'SWEETBOX'
    END,
    true
FROM rooms r
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F'), ('G'), ('H')) AS rows(row_letter)
CROSS JOIN generate_series(1, 10) AS col_num
ON CONFLICT (room_id, row_char, seat_number) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 9. THỂ LOẠI PHIM (GENRES)
-- ------------------------------------------------------------------------------
INSERT INTO genres (id, name, slug)
VALUES
    (1, 'Hành Động', 'hanh-dong'),
    (2, 'Khoa Học Viễn Tưởng', 'khoa-hoc-vien-tuong'),
    (3, 'Kinh Dị', 'kinh-di'),
    (4, 'Hoạt Hình', 'hoat-hinh'),
    (5, 'Tâm Lý - Tình Cảm', 'tam-ly-tinh-cam'),
    (6, 'Hài Hước', 'hai-huoc'),
    (7, 'Phiêu Lưu', 'phieu-luu')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, slug = EXCLUDED.slug;

SELECT setval('genres_id_seq', (SELECT MAX(id) FROM genres));

-- ------------------------------------------------------------------------------
-- 10. DANH SÁCH PHIM (MOVIES)
-- ------------------------------------------------------------------------------
INSERT INTO movies (
    id, title, original_title, synopsis, director, language, subtitle,
    supported_modes, is_featured,
    age_rating, duration_minutes, release_date, end_date, showing_status,
    poster_url, backdrop_url, trailer_youtube_url, status, created_at, updated_at
)
VALUES
    (
        '11000000-0000-0000-0000-000000000001',
        'Avatar: Dòng Chảy Của Nước',
        'Avatar: The Way of Water',
        'Lấy bối cảnh hơn một thập kỷ sau các sự kiện của phần phim đầu tiên, bộ phim kể câu chuyện về gia đình Sully, những rắc rối theo sau họ, những nỗ lực họ phải trải qua để giữ an toàn cho nhau.',
        'James Cameron',
        'Tiếng Anh',
        'Tiếng Việt',
        'SUBTITLED,DUBBED',
        true,
        'T13',
        192,
        '2026-09-01',
        '2026-11-30',
        'NOW_SHOWING',
        'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg',
        'https://image.tmdb.org/t/p/original/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg',
        'https://www.youtube.com/watch?v=d9MyW72ELq0',
        'ACTIVE',
        NOW(), NOW()
    ),
    (
        '11000000-0000-0000-0000-000000000002',
        'Oppenheimer',
        'Oppenheimer',
        'Bộ phim tiểu sử lịch sử khắc họa cuộc đời của J. Robert Oppenheimer, nhà vật lý lý thuyết lãnh đạo Dự án Manhattan chế tạo bom nguyên tử trong Thế chiến II.',
        'Christopher Nolan',
        'Tiếng Anh',
        'Tiếng Việt',
        'SUBTITLED',
        true,
        'T18',
        180,
        '2026-09-10',
        '2026-11-20',
        'NOW_SHOWING',
        'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg',
        'https://image.tmdb.org/t/p/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg',
        'https://www.youtube.com/watch?v=uYPbbksJxIg',
        'ACTIVE',
        NOW(), NOW()
    ),
    (
        '11000000-0000-0000-0000-000000000003',
        'Dune: Hành Tinh Cát - Phần Hai',
        'Dune: Part Two',
        'Hành trình của Paul Atreides khi anh hợp lực với Chani và người Fremen để trả thù những kẻ đã hủy diệt gia tộc mình, đối mặt với sự lựa chọn giữa tình yêu và số phận vũ trụ.',
        'Denis Villeneuve',
        'Tiếng Anh',
        'Tiếng Việt',
        'SUBTITLED',
        true,
        'T16',
        166,
        '2026-09-15',
        '2026-12-01',
        'NOW_SHOWING',
        'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg',
        'https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5200bm.jpg',
        'https://www.youtube.com/watch?v=Way9Dexny3w',
        'ACTIVE',
        NOW(), NOW()
    ),
    (
        '11000000-0000-0000-0000-000000000004',
        'Kung Fu Panda 4',
        'Kung Fu Panda 4',
        'Po chuẩn bị trở thành Thủ lĩnh Tinh thần của Thung lũng Bình Yên và cần huấn luyện một Chiến binh Rồng mới, trong khi đối mặt với phù thủy biến hình Tắc Kè Bông.',
        'Mike Mitchell',
        'Tiếng Anh',
        'Tiếng Việt',
        'SUBTITLED,DUBBED',
        true,
        'P',
        94,
        '2026-09-18',
        '2026-11-15',
        'NOW_SHOWING',
        'https://image.tmdb.org/t/p/w500/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg',
        'https://image.tmdb.org/t/p/original/1XDDXPXGiI8id7MrUxK26VI48ue.jpg',
        'https://www.youtube.com/watch?v=_inKs4eeHiI',
        'ACTIVE',
        NOW(), NOW()
    ),
    (
        '11000000-0000-0000-0000-000000000005',
        'Deadpool & Wolverine',
        'Deadpool & Wolverine',
        'Cặp bài trùng lầy lội nhất vũ trụ Marvel cùng nhau tái xuất trong một nhiệm vụ sống còn bảo vệ Đa vũ trụ với những pha hành động mãn nhãn và hài hước đỉnh cao.',
        'Shawn Levy',
        'Tiếng Anh',
        'Tiếng Việt',
        'SUBTITLED',
        false,
        'T18',
        128,
        '2026-10-15',
        '2026-12-30',
        'COMING_SOON',
        'https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg',
        'https://image.tmdb.org/t/p/original/yDHYTfA3R0jFYba16jBB1jv8ag0.jpg',
        'https://www.youtube.com/watch?v=73_1biulkYk',
        'ACTIVE',
        NOW(), NOW()
    )
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title, synopsis = EXCLUDED.synopsis, showing_status = EXCLUDED.showing_status,
    subtitle = EXCLUDED.subtitle, supported_modes = EXCLUDED.supported_modes, is_featured = EXCLUDED.is_featured;


-- ------------------------------------------------------------------------------
-- 11. LIÊN KẾT PHIM VÀ THỂ LOẠI (MOVIE_GENRES)
-- ------------------------------------------------------------------------------
INSERT INTO movie_genres (movie_id, genre_id)
VALUES
    ('11000000-0000-0000-0000-000000000001', 1), -- Avatar: Hành Động
    ('11000000-0000-0000-0000-000000000001', 2), -- Avatar: Viễn Tưởng
    ('11000000-0000-0000-0000-000000000002', 5), -- Oppenheimer: Tâm Lý
    ('11000000-0000-0000-0000-000000000003', 1), -- Dune 2: Hành Động
    ('11000000-0000-0000-0000-000000000003', 2), -- Dune 2: Viễn Tưởng
    ('11000000-0000-0000-0000-000000000004', 4), -- Kung Fu Panda 4: Hoạt Hình
    ('11000000-0000-0000-0000-000000000004', 6), -- Kung Fu Panda 4: Hài Hước
    ('11000000-0000-0000-0000-000000000005', 1), -- Deadpool: Hành Động
    ('11000000-0000-0000-0000-000000000005', 6)  -- Deadpool: Hài Hước
ON CONFLICT (movie_id, genre_id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 12. SUẤT CHIẾU (SHOWTIMES)
--     Tạo suất chiếu ĐỘNG cho HÔM NAY (CURRENT_DATE) và NGÀY MAI để test luôn có data!
-- ------------------------------------------------------------------------------
INSERT INTO showtimes (
    id, movie_id, room_id, show_date, start_time, end_time,
    language, subtitle_language, format, viewing_mode, base_price, available_seats, status, created_at, updated_at
)
VALUES
    -- == HÔM NAY: AVATAR 2 ==
    -- Tại CGV Đồng Khởi - Phòng IMAX
    (
        '21000000-0000-0000-0000-000000000001',
        '11000000-0000-0000-0000-000000000001',
        '01000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '09:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '12:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 160000.00, 78, 'SCHEDULED', NOW(), NOW()
    ),
    (
        '21000000-0000-0000-0000-000000000002',
        '11000000-0000-0000-0000-000000000001',
        '01000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '14:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '17:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 180000.00, 65, 'SCHEDULED', NOW(), NOW()
    ),
    (
        '21000000-0000-0000-0000-000000000003',
        '11000000-0000-0000-0000-000000000001',
        '01000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '19:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '22:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 210000.00, 52, 'SCHEDULED', NOW(), NOW()
    ),
    -- Tại CGV Crescent Mall - Phòng Standard
    (
        '21000000-0000-0000-0000-000000000004',
        '11000000-0000-0000-0000-000000000001',
        '02000000-0000-0000-0000-000000000002',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '10:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '13:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 110000.00, 80, 'SCHEDULED', NOW(), NOW()
    ),
    (
        '21000000-0000-0000-0000-000000000005',
        '11000000-0000-0000-0000-000000000001',
        '02000000-0000-0000-0000-000000000002',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '15:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '18:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 120000.00, 70, 'SCHEDULED', NOW(), NOW()
    ),
    -- Tại CGV Vạn Hạnh Mall
    (
        '21000000-0000-0000-0000-000000000006',
        '11000000-0000-0000-0000-000000000001',
        '03000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '18:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '21:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 110000.00, 60, 'SCHEDULED', NOW(), NOW()
    ),
    -- == HÔM NAY: DUNE 2 ==
    -- Tại CGV Landmark 81 - Phòng IMAX
    (
        '21000000-0000-0000-0000-000000000007',
        '11000000-0000-0000-0000-000000000003',
        '04000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '13:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '16:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 190000.00, 75, 'SCHEDULED', NOW(), NOW()
    ),
    (
        '21000000-0000-0000-0000-000000000008',
        '11000000-0000-0000-0000-000000000003',
        '04000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '18:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '20:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 220000.00, 45, 'SCHEDULED', NOW(), NOW()
    ),
    -- == HÔM NAY: KUNG FU PANDA 4 (lồng tiếng) ==
    -- Tại CGV Crescent Mall - Phòng 4DX
    (
        '21000000-0000-0000-0000-000000000009',
        '11000000-0000-0000-0000-000000000004',
        '02000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '11:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '12:35:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', '4DX', 'DUBBED', 160000.00, 80, 'SCHEDULED', NOW(), NOW()
    ),
    (
        '21000000-0000-0000-0000-000000000010',
        '11000000-0000-0000-0000-000000000004',
        '02000000-0000-0000-0000-000000000001',
        (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz,
        (CURRENT_DATE + TIME '15:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        (CURRENT_DATE + TIME '16:35:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
        'Tiếng Anh', 'Tiếng Việt', '4DX', 'DUBBED', 170000.00, 72, 'SCHEDULED', NOW(), NOW()
    )
ON CONFLICT (id) DO UPDATE
SET show_date = EXCLUDED.show_date, start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time,
    base_price = EXCLUDED.base_price, language = EXCLUDED.language, subtitle_language = EXCLUDED.subtitle_language,
    viewing_mode = EXCLUDED.viewing_mode;


-- ------------------------------------------------------------------------------
-- 13. KHUYẾN MÃI (PROMOTIONS)
-- ------------------------------------------------------------------------------
INSERT INTO promotions (
    id, code, name, description, discount_type, discount_value,
    max_discount_amount, min_order_value, applicable_tier, valid_from, valid_to,
    usage_limit, max_uses_per_user, is_active, version, created_at, updated_at
)
VALUES
    (
        '31000000-0000-0000-0000-000000000001',
        'CGVWELCOME',
        'Chào mừng thành viên mới - Giảm 20%',
        'Giảm 20% tối đa 50,000 VND cho đơn hàng từ 100,000 VND',
        'PERCENT',
        20.00,
        50000.00,
        100000.00,
        'ALL',
        NOW() - INTERVAL '10 days',
        NOW() + INTERVAL '365 days',
        10000,
        1,
        true,
        1,
        NOW(), NOW()
    ),
    (
        '31000000-0000-0000-0000-000000000002',
        'CGVIP50K',
        'Ưu đãi thành viên VIP - Giảm 50K',
        'Giảm trực tiếp 50,000 VND cho thành viên từ hạng SILVER trở lên',
        'FIXED',
        50000.00,
        NULL,
        150000.00,
        'SILVER',
        NOW() - INTERVAL '10 days',
        NOW() + INTERVAL '365 days',
        5000,
        2,
        true,
        1,
        NOW(), NOW()
    ),
    (
        '31000000-0000-0000-0000-000000000003',
        'MEGASALE',
        'Siêu Sale Cuối Tuần - Giảm 30%',
        'Giảm 30% tối đa 100,000 VND cho đơn hàng từ 200,000 VND',
        'PERCENT',
        30.00,
        100000.00,
        200000.00,
        'ALL',
        NOW() - INTERVAL '10 days',
        NOW() + INTERVAL '365 days',
        2000,
        1,
        true,
        1,
        NOW(), NOW()
    ),
    (
        '31000000-0000-0000-0000-000000000004',
        'CGVPLATINUM',
        'Đặc quyền Bạch Kim - Giảm 100K',
        'Giảm 100,000 VND khi đặt vé IMAX hoặc Gold Class cho khách hàng Platinum',
        'FIXED',
        100000.00,
        NULL,
        250000.00,
        'PLATINUM',
        NOW() - INTERVAL '10 days',
        NOW() + INTERVAL '365 days',
        1000,
        5,
        true,
        1,
        NOW(), NOW()
    )
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, discount_value = EXCLUDED.discount_value, is_active = EXCLUDED.is_active;

-- ------------------------------------------------------------------------------
-- 14. BÀI VIẾT & TIN TỨC (ARTICLES)
-- ------------------------------------------------------------------------------
INSERT INTO articles (
    id, title, slug, thumbnail_url, content, excerpt, category, tags,
    author_name, views, is_featured, is_trending, movie_id, published_at, created_at, updated_at
)
VALUES
    (
        'a1000000-0000-0000-0000-000000000001',
        'Đánh giá siêu phẩm Avatar 2: Kỷ nguyên mới của kỹ xảo điện ảnh',
        'danh-gia-sieu-pham-avatar-2',
        'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg',
        '<p>Avatar: The Way of Water thực sự là một kỳ quan thị giác của James Cameron, vượt qua mọi kỳ vọng về đồ họa và cảm xúc...</p>',
        'Avatar 2 mang đến trải nghiệm thị giác ngoạn mục với chuẩn mực mới cho công nghệ CGI thế giới.',
        'REVIEW',
        'Avatar,James Cameron,IMAX,Review',
        'CGV Reviewer',
        1520,
        true,
        true,
        '11000000-0000-0000-0000-000000000001',
        NOW() - INTERVAL '5 days',
        NOW(), NOW()
    ),
    (
        'a1000000-0000-0000-0000-000000000002',
        'Trải nghiệm công nghệ chiếu phim IMAX Laser đỉnh cao tại CGV Đồng Khởi',
        'trai-nghiem-imax-laser-cgv-dong-khoi',
        'https://image.tmdb.org/t/p/w500/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg',
        '<p>IMAX with Laser mang lại hình ảnh sắc nét gấp đôi, độ sáng vượt trội cùng hệ thống âm thanh 12 kênh sống động...</p>',
        'Khám phá phòng chiếu IMAX with Laser hiện đại bậc nhất Việt Nam tại CGV Vincom Đồng Khởi.',
        'TIN_DIEN_ANH',
        'IMAX,CGV,CongNghe,DongKhoi',
        'CGV News',
        890,
        true,
        false,
        NULL,
        NOW() - INTERVAL '2 days',
        NOW(), NOW()
    )
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title, content = EXCLUDED.content, views = EXCLUDED.views;

-- ------------------------------------------------------------------------------
-- 15. ĐƠN ĐẶT VÉ MẪU (BOOKINGS & BOOKING_SEATS)
-- ------------------------------------------------------------------------------
-- Booking 1: Đã xác nhận thành công (CONFIRMED) của user An (usr-test-001)
INSERT INTO bookings (
    id, user_id, showtime_id, promotion_id, total_base_amount,
    discount_amount, final_amount, payment_deadline, qr_code_url,
    cancelled_at, status, created_at, updated_at
)
VALUES
    (
        'b1000000-0000-0000-0000-000000000001',
        'usr-test-001',
        '21000000-0000-0000-0000-000000000001',
        '31000000-0000-0000-0000-000000000001',
        320000.00,
        50000.00,
        270000.00,
        NOW() + INTERVAL '10 minutes',
        'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=CGV-B1000000-CONFIRMED',
        NULL,
        'CONFIRMED',
        NOW() - INTERVAL '2 hours',
        NOW() - INTERVAL '1 hour 50 minutes'
    ),
    -- Booking 2: Đang chờ thanh toán (PAYMENT_PENDING) của user An
    (
        'b1000000-0000-0000-0000-000000000002',
        'usr-test-001',
        '21000000-0000-0000-0000-000000000002',
        NULL,
        180000.00,
        0.00,
        180000.00,
        NOW() + INTERVAL '12 minutes',
        'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=CGV-B1000000-PENDING',
        NULL,
        'PAYMENT_PENDING',
        NOW() - INTERVAL '3 minutes',
        NOW() - INTERVAL '3 minutes'
    ),
    -- Booking 3: Đã hủy (CANCELLED) của user Bích (usr-test-002)
    (
        'b1000000-0000-0000-0000-000000000003',
        'usr-test-002',
        '21000000-0000-0000-0000-000000000007',
        NULL,
        190000.00,
        0.00,
        190000.00,
        NOW() - INTERVAL '1 hour',
        NULL,
        NOW() - INTERVAL '50 minutes',
        'CANCELLED',
        NOW() - INTERVAL '1 hour 15 minutes',
        NOW() - INTERVAL '50 minutes'
    )
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status, final_amount = EXCLUDED.final_amount;

-- Ghế cho Booking 1
INSERT INTO booking_seats (id, booking_id, seat_id, showtime_id, price, seat_label, created_at, updated_at)
SELECT 
    '40000000-0000-0000-0000-000000000001',
    'b1000000-0000-0000-0000-000000000001',
    s.id,
    '21000000-0000-0000-0000-000000000001',
    160000.00,
    'D5',
    NOW(), NOW()
FROM seats s
WHERE s.room_id = '01000000-0000-0000-0000-000000000001' AND s.row_char = 'D' AND s.seat_number = 5
ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_seats (id, booking_id, seat_id, showtime_id, price, seat_label, created_at, updated_at)
SELECT 
    '40000000-0000-0000-0000-000000000002',
    'b1000000-0000-0000-0000-000000000001',
    s.id,
    '21000000-0000-0000-0000-000000000001',
    160000.00,
    'D6',
    NOW(), NOW()
FROM seats s
WHERE s.room_id = '01000000-0000-0000-0000-000000000001' AND s.row_char = 'D' AND s.seat_number = 6
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 16. GIAO DỊCH THANH TOÁN (PAYMENTS)
-- ------------------------------------------------------------------------------
INSERT INTO payments (
    id, provider, booking_id, user_id, amount, status,
    transaction_id, payment_method, currency, paid_at,
    refund_amount, refunded_at, payload, created_at, updated_at
)
VALUES
    (
        '50000000-0000-0000-0000-000000000001',
        'VNPAY',
        'b1000000-0000-0000-0000-000000000001',
        'usr-test-001',
        270000.00,
        'SUCCESS',
        'VNPAY-TX-17182938102',
        'VNPAY_QR',
        'VND',
        NOW() - INTERVAL '1 hour 50 minutes',
        NULL,
        NULL,
        '{"vnp_ResponseCode":"00","vnp_TransactionNo":"14567890","vnp_BankCode":"NCB"}',
        NOW() - INTERVAL '2 hours',
        NOW() - INTERVAL '1 hour 50 minutes'
    )
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status, transaction_id = EXCLUDED.transaction_id;

COMMIT;

-- ==============================================================================
-- KẾT THÚC SEED DỮ LIỆU THÀNH CÔNG!
-- ==============================================================================
