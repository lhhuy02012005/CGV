-- ==============================================================================
-- IDENTITY SERVICE - SEED DATA V2
-- ==============================================================================
INSERT INTO membership_tiers (code, name, min_spend, description, created_at, updated_at)
VALUES
    ('MEMBER', 'Thành viên Tiêu chuẩn', 0.00, 'Hạng mặc định khi đăng ký tài khoản CGV', NOW(), NOW()),
    ('SILVER', 'Thành viên Bạc', 2000000.00, 'Chi tiêu từ 2 triệu/năm, tích 5% điểm thưởng', NOW(), NOW()),
    ('GOLD', 'Thành viên Vàng', 5000000.00, 'Chi tiêu từ 5 triệu/năm, tích 7% điểm, miễn phí bắp nước sinh nhật', NOW(), NOW()),
    ('PLATINUM', 'Thành viên Bạch Kim', 15000000.00, 'Chi tiêu từ 15 triệu/năm, tích 10% điểm, phòng chờ VIP & vé xem phim miễn phí', NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name, min_spend = EXCLUDED.min_spend, description = EXCLUDED.description;

INSERT INTO users (id, email, full_name, membership_tier, total_spend_ytd, version, created_at, updated_at)
VALUES
    ('c3effc5c-2ee6-4837-bc8a-8d0c9ab0e909', 'lhhuy.2005@gmail.com', 'Le Huu Huy', 'GOLD', 5500000.00, 1, NOW(), NOW()),
    ('2434983f-0796-423b-a134-e4a24b9e8643', 'admin@gmail.com', 'Huy Admin', 'PLATINUM', 16000000.00, 1, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email, full_name = EXCLUDED.full_name, membership_tier = EXCLUDED.membership_tier, total_spend_ytd = EXCLUDED.total_spend_ytd;
