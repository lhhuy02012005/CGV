-- ==============================================================================
-- BOOKING SERVICE - SEED DATA V2
-- ==============================================================================
INSERT INTO bookings (
    id, user_id, showtime_id, promotion_id, total_base_amount,
    discount_amount, final_amount, payment_deadline, qr_code_url,
    cancelled_at, status, created_at, updated_at
)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'c3effc5c-2ee6-4837-bc8a-8d0c9ab0e909', '21000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 320000.00, 50000.00, 270000.00, NOW() + INTERVAL '10 minutes', 'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=CGV-B1000000-CONFIRMED', NULL, 'CONFIRMED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 50 minutes'),
    ('b1000000-0000-0000-0000-000000000002', 'c3effc5c-2ee6-4837-bc8a-8d0c9ab0e909', '21000000-0000-0000-0000-000000000002', NULL, 180000.00, 0.00, 180000.00, NOW() + INTERVAL '12 minutes', 'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=CGV-B1000000-PENDING', NULL, 'PAYMENT_PENDING', NOW() - INTERVAL '3 minutes', NOW() - INTERVAL '3 minutes'),
    ('b1000000-0000-0000-0000-000000000003', '2434983f-0796-423b-a134-e4a24b9e8643', '21000000-0000-0000-0000-000000000007', NULL, 190000.00, 0.00, 190000.00, NOW() - INTERVAL '1 hour', NULL, NOW() - INTERVAL '50 minutes', 'CANCELLED', NOW() - INTERVAL '1 hour 15 minutes', NOW() - INTERVAL '50 minutes')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, final_amount = EXCLUDED.final_amount;
