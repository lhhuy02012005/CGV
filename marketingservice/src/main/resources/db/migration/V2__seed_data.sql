-- ==============================================================================
-- MARKETING SERVICE - SEED DATA V2
-- ==============================================================================
INSERT INTO promotions (
    id, code, name, description, discount_type, discount_value,
    max_discount_amount, min_order_value, applicable_tier, valid_from, valid_to,
    usage_limit, max_uses_per_user, is_active, version, created_at, updated_at
)
VALUES
    ('31000000-0000-0000-0000-000000000001', 'CGVWELCOME', 'Chào mừng thành viên mới - Giảm 20%', 'Giảm 20% tối đa 50,000 VND cho đơn hàng từ 100,000 VND', 'PERCENT', 20.00, 50000.00, 100000.00, 'ALL', NOW() - INTERVAL '10 days', NOW() + INTERVAL '365 days', 10000, 1, true, 1, NOW(), NOW()),
    ('31000000-0000-0000-0000-000000000002', 'CGVIP50K', 'Ưu đãi thành viên VIP - Giảm 50K', 'Giảm trực tiếp 50,000 VND cho thành viên từ hạng SILVER trở lên', 'FIXED', 50000.00, NULL, 150000.00, 'SILVER', NOW() - INTERVAL '10 days', NOW() + INTERVAL '365 days', 5000, 2, true, 1, NOW(), NOW()),
    ('31000000-0000-0000-0000-000000000003', 'MEGASALE', 'Siêu Sale Cuối Tuần - Giảm 30%', 'Giảm 30% tối đa 100,000 VND cho đơn hàng từ 200,000 VND', 'PERCENT', 30.00, 100000.00, 200000.00, 'ALL', NOW() - INTERVAL '10 days', NOW() + INTERVAL '365 days', 2000, 1, true, 1, NOW(), NOW()),
    ('31000000-0000-0000-0000-000000000004', 'CGVPLATINUM', 'Đặc quyền Bạch Kim - Giảm 100K', 'Giảm 100,000 VND khi đặt vé IMAX hoặc Gold Class cho khách hàng Platinum', 'FIXED', 100000.00, NULL, 250000.00, 'PLATINUM', NOW() - INTERVAL '10 days', NOW() + INTERVAL '365 days', 1000, 5, true, 1, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET is_active = EXCLUDED.is_active, discount_value = EXCLUDED.discount_value;
