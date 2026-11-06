# Outbox CDC trong Compose local

Đây là cấu hình mở rộng cho luồng thanh toán, độc lập với phép đo giữ ghế. Kết quả k6 không xác nhận các bước CDC đã chạy. BE dùng hai bảng Outbox và Debezium, không dùng OutboxRelay polling.

Luồng: transaction lưu Payment/Booking cùng row Outbox → commit PostgreSQL → WAL logical → Debezium PostgreSQL connector → Outbox Event Router → Kafka → consumer nghiệp vụ. CDC có thể phát lặp; consumer cần cơ chế chống lặp phù hợp.

## Các tệp

| Tệp | Vai trò |
| --- | --- |
| `debezium/booking-outbox.json` | Chọn outbox_booking_events, slot/publication riêng, định tuyến booking.created/booking.confirmed. |
| `debezium/payment-outbox.json` | Chọn outbox_payment_events, định tuyến payment.completed/payment.failed. |
| `debezium/prepare-local-role.sql` | Lệnh psql chuẩn bị cdc_user, quyền replication và đọc hai bảng local. |
| `debezium/publications.sql` | Hai publication, mỗi publication giới hạn một bảng Outbox. |
| `scripts/register-outbox-cdc.py` | Render template, validate REST, PUT cấu hình và kiểm tra connector/task. |

File `.json` được dùng để đăng ký; `.annotated.jsonc` chứa chú thích để học cấu hình, không POST trực tiếp thay cho JSON chuẩn.

## Trình tự từ thư mục BE

Trước hết hoàn thành `demo.py init/start` để có JAR và các phụ thuộc. Khởi động thêm Payment để Flyway tạo bảng Payment, rồi worker Connect:

```bash
docker compose --env-file .env.demo -f docker-compose-dev.yml up -d --build payment-service
docker exec -i -e KAFKA_BOOTSTRAP_SERVERS=cgv-kafka-1:19092 cgv-kafka-1 sh < scripts/init-kafka-topics.sh
docker compose --env-file .env.demo -f docker-compose-dev.yml up -d debezium-connect
curl --fail http://127.0.0.1:8088/connector-plugins
docker exec cgv-postgres psql -U postgres -d cgv -c 'SHOW wal_level;'
docker exec cgv-postgres psql -U postgres -d cgv -c "SELECT to_regclass('public.outbox_booking_events'), to_regclass('public.outbox_payment_events');"
```

Script khởi tạo topic chạy bên trong broker thứ nhất, dùng địa chỉ broker trong mạng Docker và replication factor 3. Các topic nội bộ Connect dùng cleanup.policy=compact; config topic có một partition.

Chờ log Flyway/ứng dụng hoàn tất và cả hai bảng có tên trước khi cấp quyền. `wal_level` cần là `logical` theo command của PostgreSQL trong Compose. Cổng 127.0.0.1:8088 trên máy ánh xạ tới REST 8083 trong worker; bootstrap broker dùng tên/cổng trong mạng Docker.

```bash
docker exec -i -e CDC_DB_PASSWORD=cdc-local-only cgv-postgres psql -U postgres -d cgv < debezium/prepare-local-role.sql
docker exec -i cgv-postgres psql -U postgres -d cgv < debezium/publications.sql
python3 scripts/register-outbox-cdc.py --local --check
python3 scripts/register-outbox-cdc.py --local
python3 scripts/register-outbox-cdc.py --status
```

`cdc-local-only` chỉ là mật khẩu demo khớp mặc định `--local`. `--check` chỉ kiểm tra parse/thay biến ở local, không gọi REST. Lệnh `--local` thực hiện REST validate/đăng ký và đợi trạng thái task. `--status` đọc trạng thái, không đăng ký lại.

## Quan sát

```bash
docker exec cgv-postgres psql -U postgres -d cgv -c "SELECT slot_name, active, confirmed_flush_lsn FROM pg_replication_slots;"
docker exec cgv-kafka-1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server cgv-kafka-1:19092 --describe --group booking-service-group
```

Connector RUNNING cần đi kèm task RUNNING. Slot/offset theo dõi tiến độ CDC; consumer offset theo dõi việc đọc Kafka. UUID event trong header, aggregate key và bookingId trong payload có vai trò khác nhau. CDC không cập nhật is_published để đánh dấu gửi; các cột metadata polling cũ không là nguồn tiến độ streaming.

Nếu worker/task lỗi, giữ log và kiểm tra quyền, publication, slot, Kafka storage topics/type mapping. Không xóa volume hoặc slot như bước sửa lỗi mặc định. Việc tái lập thanh toán và phục hồi khi broker/worker ngừng cần kịch bản/output riêng ngoài demo giữ ghế.
