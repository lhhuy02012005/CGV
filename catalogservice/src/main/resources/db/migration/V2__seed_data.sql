-- ==============================================================================
-- CATALOG SERVICE - SEED DATA V2
-- ==============================================================================
INSERT INTO regions (id, name, slug)
VALUES
    (1, 'Hồ Chí Minh', 'ho-chi-minh'),
    (2, 'Hà Nội', 'ha-noi'),
    (3, 'Đà Nẵng', 'da-nang'),
    (4, 'Cần Thơ', 'can-tho'),
    (5, 'Bình Dương', 'binh-duong')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, slug = EXCLUDED.slug;

SELECT setval('regions_id_seq', (SELECT MAX(id) FROM regions));

INSERT INTO seat_types (name, surcharge, description)
VALUES
    ('NORMAL', 0.00, 'Ghế tiêu chuẩn với góc nhìn tiêu chuẩn'),
    ('VIP', 15000.00, 'Ghế VIP khu vực trung tâm xem phim tốt nhất'),
    ('SWEETBOX', 30000.00, 'Ghế đôi vách ngăn riêng tư dành cho cặp đôi')
ON CONFLICT (name) DO UPDATE SET surcharge = EXCLUDED.surcharge, description = EXCLUDED.description;

INSERT INTO cinemas (id, region_id, name, address, phone, opening_hours, latitude, longitude, status, created_at, updated_at)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 1, 'CGV Vincom Đồng Khởi', 'Tầng 3, TTTM Vincom Center, 72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM', '1900 6017', '08:00 - 24:00', 10.777823, 106.702515, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000002', 1, 'CGV Crescent Mall', 'Tầng 5, Crescent Mall, Đại Lộ Nguyễn Văn Linh, Tân Phú, Quận 7, TP.HCM', '1900 6017', '08:30 - 23:30', 10.729352, 106.718818, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000003', 1, 'CGV Vạn Hạnh Mall', 'Tầng 6, Vạn Hạnh Mall, 11 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM', '1900 6017', '08:00 - 23:30', 10.768652, 106.669894, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000004', 1, 'CGV Vincom Landmark 81', 'Tầng B1, Vincom Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM', '1900 6017', '08:00 - 24:00', 10.795034, 106.721867, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000005', 2, 'CGV Vincom Bà Triệu', 'Tầng 6, Vincom Center Hà Nội, 191 Bà Triệu, Hai Bà Trưng, Hà Nội', '1900 6017', '08:30 - 23:30', 21.011832, 105.849925, 'ACTIVE', NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000006', 2, 'CGV Vincom Center Nguyễn Chí Thanh', 'Số 54A Nguyễn Chí Thanh, Láng Thượng, Đống Đa, Hà Nội', '1900 6017', '08:30 - 23:30', 21.023812, 105.811823, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, status = EXCLUDED.status;

INSERT INTO cinema_amenities (cinema_id, amenity)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 'IMAX'),
    ('c1000000-0000-0000-0000-000000000001', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000001', 'DOLBY_ATMOS'),
    ('c1000000-0000-0000-0000-000000000001', 'PARKING'),
    ('c1000000-0000-0000-0000-000000000001', 'FOOD_COURT'),
    ('c1000000-0000-0000-0000-000000000002', '4D'),
    ('c1000000-0000-0000-0000-000000000002', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000002', 'PARKING'),
    ('c1000000-0000-0000-0000-000000000003', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000003', 'PARKING'),
    ('c1000000-0000-0000-0000-000000000003', 'FOOD_COURT'),
    ('c1000000-0000-0000-0000-000000000004', 'IMAX'),
    ('c1000000-0000-0000-0000-000000000004', 'SWEETBOX'),
    ('c1000000-0000-0000-0000-000000000004', 'PARKING')
ON CONFLICT (cinema_id, amenity) DO NOTHING;

INSERT INTO rooms (id, cinema_id, name, format, total_seats, row_count, column_count, status)
VALUES
    ('01000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'Cinema 1 (IMAX Laser)', 'IMAX', 80, 8, 10, 'ACTIVE'),
    ('01000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'Cinema 2 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    ('02000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000002', 'Cinema 1 (4DX)', '4DX', 80, 8, 10, 'ACTIVE'),
    ('02000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002', 'Cinema 2 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    ('03000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000003', 'Cinema 1 (Standard)', '2D', 80, 8, 10, 'ACTIVE'),
    ('04000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000004', 'Cinema 1 (IMAX)', 'IMAX', 80, 8, 10, 'ACTIVE')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, format = EXCLUDED.format, total_seats = EXCLUDED.total_seats, status = EXCLUDED.status;

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

INSERT INTO genres (id, name, slug)
VALUES
    (1, 'Hành Động', 'hanh-dong'),
    (2, 'Khoa Học Viễn Tưởng', 'khoa-hoc-vien-tuong'),
    (3, 'Kinh Dị', 'kinh-di'),
    (4, 'Hoạt Hình', 'hoat-hinh'),
    (5, 'Tâm Lý - Tình Cảm', 'tam-ly-tinh-cam'),
    (6, 'Hài Hước', 'hai-huoc'),
    (7, 'Phiêu Lưu', 'phieu-luu')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, slug = EXCLUDED.slug;

SELECT setval('genres_id_seq', (SELECT MAX(id) FROM genres));

INSERT INTO movies (
    id, title, original_title, synopsis, director, language, subtitle,
    supported_modes, is_featured,
    age_rating, duration_minutes, release_date, end_date, showing_status,
    poster_url, backdrop_url, trailer_youtube_url, status, created_at, updated_at
)
VALUES
    ('11000000-0000-0000-0000-000000000001', 'Avatar: Dòng Chảy Của Nước', 'Avatar: The Way of Water', 'Lấy bối cảnh hơn một thập kỷ sau các sự kiện của phần phim đầu tiên, bộ phim kể câu chuyện về gia đình Sully.', 'James Cameron', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED,DUBBED', true, 'T13', 192, '2026-09-01', '2026-11-30', 'NOW_SHOWING', 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg', 'https://image.tmdb.org/t/p/original/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg', 'https://www.youtube.com/watch?v=d9MyW72ELq0', 'ACTIVE', NOW(), NOW()),
    ('11000000-0000-0000-0000-000000000002', 'Oppenheimer', 'Oppenheimer', 'Bộ phim tiểu sử lịch sử khắc họa cuộc đời của J. Robert Oppenheimer, nhà vật lý lãnh đạo Dự án Manhattan.', 'Christopher Nolan', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', true, 'T18', 180, '2026-09-10', '2026-11-15', 'NOW_SHOWING', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 'https://image.tmdb.org/t/p/original/rLb2cwF3Pazuxaj0sRXQ037tGI1.jpg', 'https://www.youtube.com/watch?v=uYPbbksJxIg', 'ACTIVE', NOW(), NOW()),
    ('11000000-0000-0000-0000-000000000003', 'Thám Tử Lừng Danh Conan: Tàu Ngầm Sắt Màu Đen', 'Detective Conan: Black Iron Submarine', 'Một vụ án ly kỳ diễn ra tại cơ sở an ninh ngoài khơi Hachijo-jima.', 'Yuzuru Tachikawa', 'Tiếng Nhật', 'Tiếng Việt', 'SUBTITLED,DUBBED', false, 'P', 110, '2026-09-15', '2026-10-30', 'NOW_SHOWING', 'https://image.tmdb.org/t/p/w500/98nsm3V1F2U2Yf7z7z0x5.jpg', 'https://image.tmdb.org/t/p/original/bWIIWhnaoWx3FT9Nslao9.jpg', 'https://www.youtube.com/watch?v=kYJvM9B19r0', 'ACTIVE', NOW(), NOW()),
    ('11000000-0000-0000-0000-000000000004', 'Dune: Hành Tinh Cát - Phần 2', 'Dune: Part Two', 'Paul Atreides hợp lực cùng Chani và người Fremen trên con đường trả thù những kẻ đã hủy hoại gia đình mình.', 'Denis Villeneuve', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', true, 'T16', 166, '2026-10-15', '2026-12-15', 'COMING_SOON', 'https://image.tmdb.org/t/p/w500/czembW0Rk1Ke7lCJGhkAi0ym92H.jpg', 'https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5hj0x2.jpg', 'https://www.youtube.com/watch?v=Way9Dexny3w', 'ACTIVE', NOW(), NOW()),
    ('11000000-0000-0000-0000-000000000005', 'Godzilla x Kong: Đế Chế Mới', 'Godzilla x Kong: The New Empire', 'Hai người khổng lồ cổ đại đối mặt với một mối đe dọa to lớn tiềm ẩn trong Trái Đất.', 'Adam Wingard', 'Tiếng Anh', 'Tiếng Việt', 'SUBTITLED', false, 'T13', 115, '2026-11-01', '2026-12-31', 'COMING_SOON', 'https://image.tmdb.org/t/p/w500/bQ2ywkchCe02AcI2dU.jpg', 'https://image.tmdb.org/t/p/original/qrGtVF3YZvJaoJq.jpg', 'https://www.youtube.com/watch?v=lV1OOlGwExg', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, original_title = EXCLUDED.original_title, showing_status = EXCLUDED.showing_status;

INSERT INTO movie_genres (movie_id, genre_id)
VALUES
    ('11000000-0000-0000-0000-000000000001', 1), ('11000000-0000-0000-0000-000000000001', 2), ('11000000-0000-0000-0000-000000000001', 7),
    ('11000000-0000-0000-0000-000000000002', 5),
    ('11000000-0000-0000-0000-000000000003', 4), ('11000000-0000-0000-0000-000000000003', 7),
    ('11000000-0000-0000-0000-000000000004', 1), ('11000000-0000-0000-0000-000000000004', 2),
    ('11000000-0000-0000-0000-000000000005', 1)
ON CONFLICT (movie_id, genre_id) DO NOTHING;

INSERT INTO showtimes (id, movie_id, room_id, show_date, start_time, end_time, language, subtitle_language, format, viewing_mode, base_price, available_seats, status, created_at, updated_at)
VALUES
    ('21000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', '01000000-0000-0000-0000-000000000001', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '09:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '12:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 160000.00, 78, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000001', '01000000-0000-0000-0000-000000000001', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '14:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '17:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 180000.00, 65, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000003', '11000000-0000-0000-0000-000000000001', '01000000-0000-0000-0000-000000000001', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '19:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '22:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 210000.00, 52, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000004', '11000000-0000-0000-0000-000000000001', '02000000-0000-0000-0000-000000000002', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '10:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '13:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 110000.00, 80, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000005', '11000000-0000-0000-0000-000000000001', '02000000-0000-0000-0000-000000000002', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '15:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '18:45:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 120000.00, 75, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000006', '11000000-0000-0000-0000-000000000002', '01000000-0000-0000-0000-000000000002', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '13:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '16:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 120000.00, 80, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000007', '11000000-0000-0000-0000-000000000002', '01000000-0000-0000-0000-000000000002', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '19:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '22:30:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', '2D', 'SUBTITLED', 140000.00, 60, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000008', '11000000-0000-0000-0000-000000000003', '03000000-0000-0000-0000-000000000001', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '11:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '12:50:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Nhật', 'Tiếng Việt', '2D', 'SUBTITLED', 95000.00, 80, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000009', '11000000-0000-0000-0000-000000000003', '03000000-0000-0000-0000-000000000001', (CURRENT_DATE AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, (CURRENT_DATE + TIME '16:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', (CURRENT_DATE + TIME '17:50:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Nhật', 'Tiếng Việt', '2D', 'DUBBED', 95000.00, 70, 'SCHEDULED', NOW(), NOW()),
    ('21000000-0000-0000-0000-000000000010', '11000000-0000-0000-0000-000000000001', '04000000-0000-0000-0000-000000000001', ((CURRENT_DATE + 1) AT TIME ZONE 'Asia/Ho_Chi_Minh')::timestamptz, ((CURRENT_DATE + 1) + TIME '18:00:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', ((CURRENT_DATE + 1) + TIME '21:15:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'Tiếng Anh', 'Tiếng Việt', 'IMAX', 'SUBTITLED', 210000.00, 80, 'SCHEDULED', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET show_date = EXCLUDED.show_date, start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time;

INSERT INTO articles (id, title, slug, thumbnail_url, content, excerpt, category, tags, author_name, views, is_featured, is_trending, movie_id, published_at, created_at, updated_at)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Đánh giá siêu phẩm Avatar 2: Kỷ nguyên mới của kỹ xảo điện ảnh', 'danh-gia-sieu-pham-avatar-2', 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg', '<p>Avatar: The Way of Water thực sự là một kỳ quan thị giác của James Cameron...</p>', 'Avatar 2 mang đến trải nghiệm thị giác ngoạn mục.', 'REVIEW', 'Avatar,James Cameron,IMAX,Review', 'CGV Reviewer', 1520, true, true, '11000000-0000-0000-0000-000000000001', NOW() - INTERVAL '5 days', NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000002', 'Trải nghiệm công nghệ chiếu phim IMAX Laser đỉnh cao tại CGV Đồng Khởi', 'trai-nghiem-imax-laser-cgv-dong-khoi', 'https://image.tmdb.org/t/p/w500/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg', '<p>IMAX with Laser mang lại hình ảnh sắc nét gấp đôi, độ sáng vượt trội...</p>', 'Khám phá phòng chiếu IMAX with Laser hiện đại bậc nhất Việt Nam.', 'TIN_DIEN_ANH', 'IMAX,CGV,CongNghe,DongKhoi', 'CGV News', 890, true, false, NULL, NOW() - INTERVAL '2 days', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, content = EXCLUDED.content, views = EXCLUDED.views;
