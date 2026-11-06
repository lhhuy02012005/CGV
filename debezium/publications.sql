-- Publication là danh sách bảng được công bố qua pgoutput; không phải một tiến trình gửi Kafka.
-- Chạy bằng psql với owner/DBA phù hợp sau khi cả hai bảng Outbox tồn tại.
-- Dừng file nếu có SQL lỗi; không tiếp tục rồi coi cấu hình đã thành công.
\set ON_ERROR_STOP on
-- Gom bước tạo publication có điều kiện trong khối PL/pgSQL.
DO $$ BEGIN
  -- Kiểm tra tên publication Booking trong danh mục PostgreSQL của database đang kết nối.
  IF NOT EXISTS (SELECT FROM pg_publication WHERE pubname = 'cinematrix_booking_outbox_pub') THEN
    -- Chỉ công bố bảng Outbox Booking; tên phải khớp publication.name trong booking-outbox.json.
    CREATE PUBLICATION cinematrix_booking_outbox_pub FOR TABLE public.outbox_booking_events;
  -- Kết thúc nhánh Booking. Nếu publication đã tồn tại, không tự sửa danh sách bảng cũ.
  END IF;
  -- Tương tự nhưng độc lập cho Payment; mỗi connector dùng publication/slot riêng.
  IF NOT EXISTS (SELECT FROM pg_publication WHERE pubname = 'cinematrix_payment_outbox_pub') THEN
    -- Chỉ công bố bảng Outbox Payment; không dùng FOR ALL TABLES.
    CREATE PUBLICATION cinematrix_payment_outbox_pub FOR TABLE public.outbox_payment_events;
  -- Kết thúc nhánh Payment.
  END IF;
-- Kết thúc khối tạo publication; file không tự tạo replication slot hoặc connector Kafka Connect.
END $$;
-- Liệt kê bảng thực tế trong publication để người chạy đối chiếu, kể cả khi publication đã có trước.
SELECT pubname, schemaname, tablename FROM pg_publication_tables
-- Chỉ lấy thông tin hai publication vừa cấu hình; không hiển thị mật khẩu hoặc dữ liệu nghiệp vụ.
WHERE pubname IN ('cinematrix_booking_outbox_pub', 'cinematrix_payment_outbox_pub');
