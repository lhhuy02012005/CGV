-- Sửa dữ liệu mẫu bị lệch giữa các phim (seed cũ):
--   * "Conan" đang mang dữ liệu của Dune: Part Two
--   * "Dune: Part Two" đang mang dữ liệu của Kung Fu Panda 4
--   * "Godzilla x Kong" đang mang dữ liệu của Deadpool & Wolverine
-- Các câu lệnh đều khóa theo id nên không ảnh hưởng phim khác; phim không tồn tại thì bỏ qua.
-- Phân loại độ tuổi là dữ liệu demo cho đồ án.

-- 1. Dune: Phần 2 (nhận lại tóm tắt/trailer/đạo diễn đang nằm nhầm ở bản ghi Conan)
UPDATE movies SET
    director            = 'Denis Villeneuve',
    duration_minutes    = 166,
    age_rating          = 'T13',
    trailer_youtube_url = 'https://www.youtube.com/watch?v=Way9Dexny3w',
    synopsis            = 'Hành trình của Paul Atreides khi anh hợp lực với Chani và người Fremen để trả thù những kẻ đã hủy diệt gia tộc mình, đối mặt với sự lựa chọn giữa tình yêu và số phận vũ trụ.'
WHERE id = '11000000-0000-0000-0000-000000000004';

-- 2. Thám Tử Lừng Danh Conan: Tàu Ngầm Sắt Màu Đen
UPDATE movies SET
    director            = 'Yuzuru Tachikawa',
    duration_minutes    = 109,
    age_rating          = 'T13',
    trailer_youtube_url = NULL,
    synopsis            = 'Conan và nhóm thám tử nhí đến tham quan cơ sở Pacific Buoy của Interpol ngoài khơi Hachijo-jima. Một nhân viên bị Tổ chức Áo Đen bắt cóc, và Haibara Ai cũng trở thành mục tiêu của chúng.'
WHERE id = '11000000-0000-0000-0000-000000000003';

-- 3. Godzilla x Kong: Đế Chế Mới
UPDATE movies SET
    director            = 'Adam Wingard',
    duration_minutes    = 115,
    age_rating          = 'T13',
    trailer_youtube_url = NULL,
    synopsis            = 'Kong và Godzilla buộc phải hợp sức chống lại một mối đe dọa khổng lồ ẩn sâu trong lòng Trái Đất rỗng, đe dọa sự tồn vong của cả hai loài và nhân loại.'
WHERE id = '11000000-0000-0000-0000-000000000005';

-- 4. Bổ sung phân loại độ tuổi còn trống
UPDATE movies SET age_rating = 'T16' WHERE id = '11000000-0000-0000-0000-000000000006' AND age_rating IS NULL;
UPDATE movies SET age_rating = 'T13' WHERE id = '11000000-0000-0000-0000-000000000007' AND age_rating IS NULL;
UPDATE movies SET age_rating = 'T13' WHERE id = '11000000-0000-0000-0000-000000000008' AND age_rating IS NULL;

-- 5. Thể loại đúng cho 3 phim bị lệch (genres: 1 Hành Động, 2 KH Viễn Tưởng, 4 Hoạt Hình, 6 Hài Hước, 7 Phiêu Lưu)
DELETE FROM movie_genres WHERE movie_id IN (
    '11000000-0000-0000-0000-000000000003',
    '11000000-0000-0000-0000-000000000004',
    '11000000-0000-0000-0000-000000000005');
INSERT INTO movie_genres (movie_id, genre_id)
SELECT v.movie_id::uuid, v.genre_id
FROM (VALUES
    ('11000000-0000-0000-0000-000000000003', 4), ('11000000-0000-0000-0000-000000000003', 1), ('11000000-0000-0000-0000-000000000003', 7),
    ('11000000-0000-0000-0000-000000000004', 1), ('11000000-0000-0000-0000-000000000004', 2), ('11000000-0000-0000-0000-000000000004', 7),
    ('11000000-0000-0000-0000-000000000005', 1), ('11000000-0000-0000-0000-000000000005', 2), ('11000000-0000-0000-0000-000000000005', 7)
) AS v(movie_id, genre_id)
WHERE EXISTS (SELECT 1 FROM movies m WHERE m.id = v.movie_id::uuid)
  AND EXISTS (SELECT 1 FROM genres g WHERE g.id = v.genre_id);

-- 6. Diễn viên & đạo diễn mẫu (trước đây bảng movie_casts trống)
INSERT INTO movie_casts (id, movie_id, actor_name, character_name, role_type, display_order)
SELECT v.id::uuid, v.movie_id::uuid, v.actor_name, v.character_name, v.role_type, v.display_order
FROM (VALUES
    ('51000000-0000-0000-0000-000000000101', '11000000-0000-0000-0000-000000000001', 'James Cameron', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000102', '11000000-0000-0000-0000-000000000001', 'Sam Worthington', 'Jake Sully', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000103', '11000000-0000-0000-0000-000000000001', 'Zoe Saldaña', 'Neytiri', 'LEAD', 2),
    ('51000000-0000-0000-0000-000000000104', '11000000-0000-0000-0000-000000000001', 'Sigourney Weaver', 'Kiri', 'SUPPORTING', 3),
    ('51000000-0000-0000-0000-000000000105', '11000000-0000-0000-0000-000000000001', 'Kate Winslet', 'Ronal', 'SUPPORTING', 4),

    ('51000000-0000-0000-0000-000000000201', '11000000-0000-0000-0000-000000000002', 'Christopher Nolan', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000202', '11000000-0000-0000-0000-000000000002', 'Cillian Murphy', 'J. Robert Oppenheimer', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000203', '11000000-0000-0000-0000-000000000002', 'Emily Blunt', 'Kitty Oppenheimer', 'SUPPORTING', 2),
    ('51000000-0000-0000-0000-000000000204', '11000000-0000-0000-0000-000000000002', 'Matt Damon', 'Leslie Groves', 'SUPPORTING', 3),
    ('51000000-0000-0000-0000-000000000205', '11000000-0000-0000-0000-000000000002', 'Robert Downey Jr.', 'Lewis Strauss', 'SUPPORTING', 4),

    ('51000000-0000-0000-0000-000000000301', '11000000-0000-0000-0000-000000000003', 'Yuzuru Tachikawa', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000302', '11000000-0000-0000-0000-000000000003', 'Minami Takayama', 'Conan Edogawa (lồng tiếng)', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000303', '11000000-0000-0000-0000-000000000003', 'Wakana Yamazaki', 'Ran Mouri (lồng tiếng)', 'SUPPORTING', 2),
    ('51000000-0000-0000-0000-000000000304', '11000000-0000-0000-0000-000000000003', 'Megumi Hayashibara', 'Ai Haibara (lồng tiếng)', 'SUPPORTING', 3),

    ('51000000-0000-0000-0000-000000000401', '11000000-0000-0000-0000-000000000004', 'Denis Villeneuve', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000402', '11000000-0000-0000-0000-000000000004', 'Timothée Chalamet', 'Paul Atreides', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000403', '11000000-0000-0000-0000-000000000004', 'Zendaya', 'Chani', 'LEAD', 2),
    ('51000000-0000-0000-0000-000000000404', '11000000-0000-0000-0000-000000000004', 'Rebecca Ferguson', 'Lady Jessica', 'SUPPORTING', 3),
    ('51000000-0000-0000-0000-000000000405', '11000000-0000-0000-0000-000000000004', 'Austin Butler', 'Feyd-Rautha', 'SUPPORTING', 4),

    ('51000000-0000-0000-0000-000000000501', '11000000-0000-0000-0000-000000000005', 'Adam Wingard', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000502', '11000000-0000-0000-0000-000000000005', 'Rebecca Hall', 'Ilene Andrews', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000503', '11000000-0000-0000-0000-000000000005', 'Brian Tyree Henry', 'Bernie Hayes', 'SUPPORTING', 2),
    ('51000000-0000-0000-0000-000000000504', '11000000-0000-0000-0000-000000000005', 'Dan Stevens', 'Trapper', 'SUPPORTING', 3),

    ('51000000-0000-0000-0000-000000000601', '11000000-0000-0000-0000-000000000006', 'Brian Duffield', NULL, 'DIRECTOR', 0),

    ('51000000-0000-0000-0000-000000000701', '11000000-0000-0000-0000-000000000007', 'Francis Lawrence', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000702', '11000000-0000-0000-0000-000000000007', 'Joseph Zada', 'Haymitch Abernathy', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000703', '11000000-0000-0000-0000-000000000007', 'Whitney Peak', 'Lenore Dove Baird', 'LEAD', 2),
    ('51000000-0000-0000-0000-000000000704', '11000000-0000-0000-0000-000000000007', 'Jesse Plemons', 'Plutarch Heavensbee', 'SUPPORTING', 3),
    ('51000000-0000-0000-0000-000000000705', '11000000-0000-0000-0000-000000000007', 'Elle Fanning', 'Effie Trinket', 'SUPPORTING', 4),

    ('51000000-0000-0000-0000-000000000801', '11000000-0000-0000-0000-000000000008', 'Anthony Russo, Joe Russo', NULL, 'DIRECTOR', 0),
    ('51000000-0000-0000-0000-000000000802', '11000000-0000-0000-0000-000000000008', 'Robert Downey Jr.', 'Victor von Doom', 'LEAD', 1),
    ('51000000-0000-0000-0000-000000000803', '11000000-0000-0000-0000-000000000008', 'Chris Hemsworth', 'Thor', 'SUPPORTING', 2),
    ('51000000-0000-0000-0000-000000000804', '11000000-0000-0000-0000-000000000008', 'Anthony Mackie', 'Sam Wilson / Captain America', 'SUPPORTING', 3)
) AS v(id, movie_id, actor_name, character_name, role_type, display_order)
WHERE EXISTS (SELECT 1 FROM movies m WHERE m.id = v.movie_id::uuid)
ON CONFLICT (id) DO NOTHING;

-- 7. Giờ kết thúc suất chiếu = giờ bắt đầu + thời lượng phim (đã kiểm tra không tạo chồng giờ trong cùng phòng)
UPDATE showtimes s
SET end_time = s.start_time + make_interval(mins => m.duration_minutes)
FROM movies m
WHERE m.id = s.movie_id
  AND m.duration_minutes IS NOT NULL
  AND s.end_time <> s.start_time + make_interval(mins => m.duration_minutes);
