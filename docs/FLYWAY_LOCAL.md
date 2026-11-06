# Flyway và khởi động Docker local

Năm service có migration dùng chung PostgreSQL database `cgv`, schema `public`, nhưng có lịch sử riêng:

| Service | Bảng lịch sử |
|---|---|
| Identity | flyway_identity_schema_history |
| Catalog | flyway_catalog_schema_history |
| Marketing | flyway_marketing_schema_history |
| Booking | flyway_booking_schema_history |
| Payment | flyway_payment_schema_history |

Flyway bật trực tiếp trong dev/prod; Hibernate `ddl-auto=validate` kiểm tra Entity, không tự thay đổi bảng. Media/Notification không có Entity/migration trong source và cũng đặt validate. Hai file profile `application-migration.yml` cũ đã bỏ; chạy profile dev hoặc prod bình thường.

## Database local mới

Dev dùng baseline version 0: khi schema public đã có bảng của service khác, service mới vẫn chạy V1 của chính nó. V1 tạo các bảng bằng IF NOT EXISTS; V2 nạp dữ liệu mẫu; các migration tiếp theo đồng bộ Instant/TIMESTAMPTZ và những cột Entity thay đổi. V1/V2 không bị sửa checksum trong lần khắc phục này.

```bash
mvn clean install -DskipTests
docker compose -f docker-compose-dev.yml up -d --build
```

Migration chạy khi Spring Boot khởi động, không phải khi chỉ bật container postgres. Identity bổ sung starter Kafka để tạo KafkaTemplate theo Boot 4. Starter Flyway của Boot 4 được bổ sung cho Identity/Marketing; flyway-core riêng trước đó chưa kích hoạt auto-configuration tương ứng.

## Database local đã dùng chung lịch sử sai

Database đã kiểm tra có baseline V1 và V2 checksum 105936488, khớp Catalog. Đã sao lưu public rồi chuyển tên lịch sử cũ thành flyway_catalog_schema_history. Giữ bản ghi V2 của Catalog; không sửa checksum hoặc chạy lại V2 đó. Các service còn lại tạo lịch sử riêng và chạy migration trên schema đã được đối chiếu. Không dùng repair chung để luân phiên đổi checksum V2 giữa service.

Backup trước thay đổi: `/private/tmp/cinematrix-service-fix/cgv-public-before-fix.dump` (chỉ dữ liệu/schema nghiệp vụ public; Keycloak không bị thay đổi). Đường dẫn này là bản sao phục hồi local của lần sửa, không phải thành phần ứng dụng.

Không chạy lại thao tác chuyển lịch sử trên một database khác mà chưa đối chiếu owner/version/checksum. Prod không tự baseline: trước khi kích hoạt trên RDS cũ cần audit, backup và baseline từng lịch sử phù hợp; lần sửa này không triển khai hoặc migrate AWS.

## Kiểm chứng

Bộ chọn lọc FeignConfigTest, CdcEventDeserializationTest, BookingIntegrityIntegrationTest và PaymentCallbackIntegrationTest có 15 ca đạt. Ngoài ra đã chạy SQL Flyway thật và Hibernate validate cho năm service trên hai database PostgreSQL tạm: một database mới và một bản sao database local; cả 10 lượt đạt, sau đó dọn database tạm.

Log kiểm chứng được lưu ở `target/service-fix-tests.log` và `target/postgres-migration-verification.log`. Đây là kiểm chứng migration/bean/contract và nghiệp vụ cục bộ, không phải benchmark API toàn hệ thống.

Healthcheck Kafka dùng `/opt/kafka/bin/kafka-topics.sh`, thay lệnh thiếu đường dẫn đã báo not found. Sau khi thay image service, restart Kong để cập nhật DNS/upstream tới container mới.

```bash
docker compose -f docker-compose-dev.yml ps
curl --fail http://localhost:8082/api/v1/actuator/health
curl --fail http://localhost:8085/api/v1/actuator/health
```

## Bộ nhớ Docker local

Compose dev giới hạn heap ứng dụng 256 MiB và ActiveProcessorCount=2; Connect có heap 256 MiB. Elasticsearch giữ heap 512 MiB, tắt ML vì demo tìm phim không dùng ML jobs, và có restart policy. Các giới hạn chỉ áp dụng Docker local, chưa đánh giá tải production. Chúng khắc phục OOMKilled Elasticsearch đã quan sát khi toàn stack khởi động trên Docker Desktop khoảng 8 GiB.

## Elasticsearch và Kong

Boot 4 kéo Elasticsearch Java client 9.4.5; server 8.11.0 trả health thiếu trường `unassigned_primary_shards` mà client 9 yêu cầu. Compose dev dùng server 9.4.5 và volume `es_9_data` mới; volume `es_data` bản 8 được giữ nguyên. Index `movies` cũ có 0 document tại lúc kiểm tra. Định nghĩa và dữ liệu index được sao lưu trong thư mục backup local. PostgreSQL vẫn giữ dữ liệu phim. Kibana đồng bộ 9.4.5, chạy khi cần qua profile `observability`, chưa được khởi động/kiểm chứng trong lần sửa.

ElasticsearchConfig bỏ header compatible-with=8; client dùng mặc định của bản 9. Route Catalog trực tiếp dùng service URL không thêm `/api/v1`; route alias `/api/v1/catalogs` vẫn strip prefix và dùng URL `/api/v1`. Hai đường dẫn `/api/v1/movies/now-showing` và `/api/v1/catalogs/movies/now-showing` đã trả 200 qua Kong. Bảy service trả health 200/UP. API Booking/Payment yêu cầu đăng nhập trả 401 khi kiểm tra không có token, đã hết 502 trong các request kiểm tra này. Chưa kiểm chứng phiên đăng nhập hiện có trên trình duyệt hoặc thanh toán VNPay bên ngoài.

## CDC local đã kiểm chứng

Worker trước đó tự dừng vì storage topic dùng cleanup.policy=delete; config topic còn có 3 partition. Đã đổi ba storage topic sang compact, giữ topic config cũ và tạo `cinematrix_connect_configs` có đúng một partition. Compose dev trỏ tới topic mới; tên offset/status vẫn giữ nguyên. Script init topic chuẩn bị compact và fail khi thao tác không thành công. Khi bootstrap một môi trường khác cần đối chiếu tên storage topic trước khi chạy.

Script register bổ sung `name` cho API validate và chờ status 404 tạm thời ngay sau PUT trong giới hạn 90 giây. Cả hai connector và task 0 trả RUNNING. Đã kiểm tra streaming bằng một INSERT Outbox thuộc booking mẫu CONFIRMED: nhận record ở payment.completed có đúng key paymentId, header __TypeId__/outbox-event-id và payload bookingId; Booking consumer commit offset, không tạo thêm booking Outbox hoặc đổi trạng thái thanh toán/booking. Dòng Outbox test đã được dọn. Log nằm ở `target/cdc-stream-verification.log`; đây không phải phép đo latency hay kiểm thử giao dịch VNPay thật.

Snapshot cũng phát hai sự kiện thanh toán cũ của booking a074df41-4cf3-45fc-b4fc-9d5dd4e3b584 đã CANCELLED; Booking từ chối xác nhận đúng theo guard hiện tại. Giữ nguyên dữ liệu để đối soát thanh toán; không tự chuyển booking đã hủy sang CONFIRMED. Cấu hình error handler mặc định hiện retry rồi recover/log; không khai là đã có luồng hoàn tiền hoặc xử lý DLT hoàn chỉnh. Cần kiểm thử đơn mới trong khoảng thời gian thanh toán còn hiệu lực.
