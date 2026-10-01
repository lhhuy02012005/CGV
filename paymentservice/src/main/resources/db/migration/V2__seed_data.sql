-- ==============================================================================
-- PAYMENT SERVICE - SEED DATA V2
-- ==============================================================================
INSERT INTO payments (
    id, provider, booking_id, user_id, amount, status,
    transaction_id, payment_method, currency, paid_at,
    refund_amount, refunded_at, payload, created_at, updated_at
)
VALUES
    ('50000000-0000-0000-0000-000000000001', 'VNPAY', 'b1000000-0000-0000-0000-000000000001', 'c3effc5c-2ee6-4837-bc8a-8d0c9ab0e909', 270000.00, 'SUCCESS', 'VNPAY-TX-17182938102', 'VNPAY_QR', 'VND', NOW() - INTERVAL '1 hour 50 minutes', NULL, NULL, '{"vnp_ResponseCode":"00","vnp_TransactionNo":"14567890","vnp_BankCode":"NCB"}', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 50 minutes')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, transaction_id = EXCLUDED.transaction_id;
