-- Chuẩn bị tài khoản CDC trên PostgreSQL Compose local; chạy bằng psql với quyền quản trị.
-- Không dùng nguyên file này trên RDS: RDS cấp quyền thông qua role rds_replication.
-- Đây là lệnh psql, không phải SQL server: gặp lỗi thì dừng thay vì chạy tiếp các GRANT.
\set ON_ERROR_STOP on
-- psql đọc biến môi trường trong tiến trình psql; docker exec cần truyền -e CDC_DB_PASSWORD=... .
-- cdc_password là biến psql, không phải cột database. Không in giá trị biến để chụp minh chứng.
\getenv cdc_password CDC_DB_PASSWORD
-- Chạy một khối PL/pgSQL; $$ bao quanh nội dung khối, không phải shell command substitution.
DO $$ BEGIN
  -- Chỉ tạo role nếu chưa tồn tại; pg_roles là danh mục role của PostgreSQL.
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'cdc_user') THEN
    -- LOGIN cho phép kết nối; REPLICATION cho phép mở kết nối logical replication.
    CREATE ROLE cdc_user LOGIN REPLICATION;
  -- Kết thúc kiểm tra tồn tại.
  END IF;
-- Kết thúc khối DO.
END $$;
-- format %L trích dẫn giá trị mật khẩu thành SQL literal; :'cdc_password' là biến psql có quoting.
-- \gexec thực thi câu ALTER ROLE vừa được SELECT tạo ra. Chạy lại sẽ đổi mật khẩu role về giá trị đã truyền.
SELECT format('ALTER ROLE cdc_user WITH LOGIN REPLICATION PASSWORD %L', :'cdc_password') \gexec
-- Cho phép cdc_user kết nối database cgv. Quyền replication và quyền SQL đọc bảng là hai loại quyền riêng.
GRANT CONNECT ON DATABASE cgv TO cdc_user;
-- Cho phép truy cập các đối tượng thuộc schema public; USAGE không tự cấp SELECT mọi bảng.
GRANT USAGE ON SCHEMA public TO cdc_user;
-- Chỉ cấp quyền đọc hai bảng Outbox để snapshot; không cấp INSERT/UPDATE/DELETE nghiệp vụ.
-- Hai bảng phải tồn tại trước lệnh này; file không tự tạo bảng hoặc nạp dữ liệu mẫu.
GRANT SELECT ON public.outbox_booking_events, public.outbox_payment_events TO cdc_user;
