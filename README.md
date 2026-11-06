# CineMatrix - Backend đặt vé xem phim mô phỏng

Spring Boot **4.1.1**, **JDK 17**, Maven multi-module. Repository gồm 7 service (Identity, Catalog, Booking, Payment, Marketing, Media, Notification) và thư viện `commondto`. Package `com.cgv` là định danh mã nguồn cũ; tên sản phẩm trong nghiên cứu là CineMatrix.

Hướng dẫn IntelliJ/Maven, test Booking/Payment trên H2, Redis và PostgreSQL thật, cùng k6 có JWT: [`docs/TESTING_BOOKING_PAYMENT.md`](docs/TESTING_BOOKING_PAYMENT.md). Chỉ ghi kết quả thực đo của mỗi RUN_ID; không coi k6 giữ ghế là test thanh toán VNPay.

### Upload poster Cloudinary

MediaService cấp chữ ký qua `GET /api/v1/media/cloudinary/upload-signature` (JWT có `CONTENT_MANAGER`, `SUPER_ADMIN` hoặc `movie:manage`). FE quản trị gửi ảnh trực tiếp tới Cloudinary rồi lưu `secure_url` trong phim. API secret chỉ đặt ở BE; S3 presigned URL cũ vẫn giữ, nhưng FE quản trị không dùng nữa.

Đặt `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong `.env` (không commit); Docker Compose tự đọc `.env` khi chạy từ thư mục BE. Build JAR MediaService trước, rồi chạy `docker compose -f docker-compose-dev.yml up -d --build --no-deps media-service`. Nếu chạy IntelliJ, đặt ba biến trong Run Configuration của MediaService. Thiếu biến, endpoint ký trả 503; Cloudinary phải cho phép signed upload, tài khoản cần đủ quota. Giá trị cloud name xem tại Cloudinary Console. Không đưa API secret vào `VITE_*`, Git hoặc báo cáo. Các môi trường Kubernetes cần cấp ba biến tương ứng qua Secret/ConfigMap trước khi deploy. S3 mặc định tắt; nếu cần dùng endpoint S3 cũ, đặt `AWS_S3_ENABLED=true` cùng bucket và thông tin xác thực AWS hợp lệ.

## 1. Phạm vi chạy được trong gói bàn giao

Lát cắt demo: **giữ ghế → tranh chấp → kiểm tra chủ sở hữu → nhả ghế → chạy k6 1.000 request tranh cùng một ghế**.

Luồng dùng Kong, Booking, Catalog gRPC, PostgreSQL và Redis Lua. Bản hiện tại bắt JWT khi giữ/nhả ghế, tạo/xem/hủy đơn và tạo URL VNPay; `X-User-Id` không cấp quyền. Không còn phiên khách hoặc cờ bỏ qua đăng nhập. Kết quả k6 ngày 22/10/2026 đo JAR cũ với chủ mô phỏng; không đại diện bản này.

FE không lưu phiên guest hay mã đơn trong `sessionStorage`. Khi quay từ VNPay, mã đơn nằm trong `vnp_TxnRef`; khi mở lại suất chiếu, FE gọi `GET /api/v1/bookings/my-pending?showtimeId=<UUID>` bằng JWT. API chỉ trả đơn `PAYMENT_PENDING` của chính chủ; lỗi tra cứu thì FE chặn đặt mới cho đến khi kiểm tra lại. Redis TTL chỉ dùng giữ ghế tạm, không là phiên đăng nhập.

## 2. Chuẩn bị trước khi chạy

| Thành phần | Cần chuẩn bị |
| --- | --- |
| Hệ điều hành | macOS, Linux hoặc Windows chạy WSL2; dùng terminal trong thư mục BE. |
| Java | **JDK 17**; `java -version` và Java mà Maven sử dụng cần cùng phiên bản. |
| Maven | Maven 3.9 trở lên, lệnh `mvn` trong PATH. |
| Python | Python 3.10 trở lên, lệnh `python3`; script chỉ dùng thư viện chuẩn. |
| Docker | Docker Engine và Compose v2 trở lên. Với Docker Desktop, mở ứng dụng và chờ Engine sẵn sàng. |
| k6 | Script dùng k6 trong PATH nếu có; nếu chưa cài thì tự dùng image `grafana/k6:1.3.0`. |
| Kết nối mạng | Lần đầu cần tải Maven dependencies và Docker images. Các lần sau có thể dùng cache. |
| Tài nguyên | Điểm bắt đầu đề xuất cho stack local: cấp khoảng 4 CPU, 8 GiB RAM cho Docker và còn khoảng 15 GiB đĩa. Đây là cấu hình chuẩn bị, không phải mức tối thiểu đã benchmark. |
| Cổng máy | Dành cổng 5432, 6379, 8000, 8082, 8083, 8180, 9090, 9092, 9094, 9096, 9200 cho stack. Nếu đã chạy bản CineMatrix khác, kiểm tra tránh trùng cổng/container. |

Lát cắt giữ ghế không gọi nhà cung cấp email, thanh toán hoặc lưu trữ tệp. Các chức năng đó có cấu hình dịch vụ ngoài riêng. Frontend là tùy chọn cho lát cắt này; các lệnh HTTP/script đủ để thực hiện demo và đo k6.

Từ thư mục BE, kiểm tra môi trường:

```bash
python3 scripts/demo.py doctor
```

Nếu Docker báo không kết nối được daemon, mở Docker Desktop rồi chạy lại. Nếu Java sai phiên bản, chỉnh `JAVA_HOME`/`PATH` về JDK 17 trước khi build.

## 3. Tạo cấu hình local

```bash
python3 scripts/demo.py init
```

Lệnh tạo **`.env.demo`** từ `.env.demo.example`, dùng tài khoản PostgreSQL local trong Compose và lấy client secret của `CGV_App` từ realm JSON local để đồng bộ cấu hình. Lệnh giữ nguyên `.env` hiện có và không in secret. Nếu `.env.demo` đã tồn tại, nó được giữ lại.

Các lệnh `demo.py` gọi Compose với `--env-file .env.demo`. Tệp này dành cho demo local, không đưa vào gói bàn giao. Không cần chép thông tin tài khoản dịch vụ ngoài vào để chạy giữ ghế.

## 4. Build và khởi động

```bash
python3 scripts/demo.py start
```

Script thực hiện tuần tự:

1. Kiểm tra Java, Maven, Docker và Compose.
2. Chạy `mvn -B -DskipTests package` tại POM gốc; build `commondto` và các service theo reactor. Lệnh này build ứng dụng, không phải kết quả kiểm thử.
3. Build image và khởi động PostgreSQL, Redis, 3 Kafka broker, Keycloak, Elasticsearch, CatalogService, BookingService và Kong.
4. Chờ tối đa 300 giây: Redis phản hồi PONG, các bảng cần thiết đã có, Gateway gọi được API đọc ghế đã đặt.
5. In `READY` khi đường đi của lát cắt sẵn sàng. Container trạng thái `Up` riêng lẻ chưa đủ để kết luận ứng dụng đã sẵn sàng.

Nếu chỉ cần kiểm tra sau khi hệ thống đã chạy:

```bash
python3 scripts/demo.py wait
python3 scripts/demo.py status
```

Các địa chỉ local:

| Địa chỉ | Công dụng |
| --- | --- |
| `http://localhost:8000/api/v1/bookings/seat-locks` | POST giữ ghế, DELETE nhả ghế qua Kong. |
| `http://localhost:8082/api/v1/actuator/health` | Health của BookingService. |
| `http://localhost:8082/api/v1/swagger-ui.html` | Tài liệu API của BookingService. |
| `http://localhost:8180` | Keycloak local. |
| `http://localhost:9200` | Elasticsearch local của Catalog. |

## 5. Migration và dữ liệu demo

Flyway chạy khi ứng dụng khởi động. Identity/Catalog/Booking/Payment/Marketing có bảng lịch sử riêng; trong lát cắt, Catalog và Booking áp dụng migration của chúng. Hibernate dùng **`ddl-auto: validate`**: kiểm tra schema, không tự tạo/sửa bảng.

### Phim, poster và lịch cuối năm 2026

Catalog migration `catalogservice/src/main/resources/db/demo/V4__year_end_2026_demo_catalog.sql` chỉ được profile `dev` nạp; profile `prod` không đọc thư mục `db/demo`. Không dùng chung database đã chạy `dev` với `prod` vì lịch sử Flyway của DB demo có V4. V4 sửa poster hỏng của ba phim cũ, bổ sung ba phim và nạp lịch từ **04/11 đến 31/12/2026** tại sáu phòng demo. Phim/poster là nội dung phim thực; **ngày/giờ chiếu, rạp, giá vé là dữ liệu giả lập cho bài tập, không phải lịch CGV công bố**. Phim cũ là suất chiếu lại giả lập; phim mới chỉ được xếp lịch từ ngày phát hành. Ảnh lấy từ TMDb/Rotten Tomatoes qua URL ngoài, cần mạng và có thể thay đổi hoặc bị chặn hotlink; chỉ dùng cho demo, kiểm tra quyền sử dụng trước khi phát hành công khai. Ngày phát hành dùng theo thị trường Bắc Mỹ, không khẳng định là ngày khởi chiếu tại Việt Nam.

Clone mới: chạy `python3 scripts/demo.py init`, rồi `python3 scripts/demo.py start` ở mục 3–4. `start` build Catalog JAR, build lại image và Flyway nạp V1–V4; **không cần import SQL tay, không xóa Docker volume**. Với stack Compose đã chạy nhưng chưa có V4:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -B -pl catalogservice -am -DskipTests package # macOS; Linux/WSL: đặt JAVA_HOME tới JDK 17
docker compose --env-file .env.demo -f docker-compose-dev.yml up -d --build --no-deps catalog-service
docker compose --env-file .env.demo -f docker-compose-dev.yml logs --tail=100 catalog-service
docker exec -i cgv-postgres psql -v ON_ERROR_STOP=1 -U postgres -d cgv < scripts/check-demo-catalog-2026.sql
```

Migration chỉ chạy một lần trên mỗi DB; restart không nạp trùng hoặc đặt lại vé đã bán. `showing_status` của phim chưa phát hành là trạng thái lưu trong DB: đến 20/11 (Hunger Games) và 18/12 (Avengers), người quản trị cần đổi sang `NOW_SHOWING` qua API quản trị; lịch đã nạp không tự đổi nhãn phim. Sau thay đổi trực tiếp trong DB, cache Catalog (`movies:detail`, `movies:now-showing`, `movies:coming-soon`, `showtimes`/`cinemaSchedule`) có thể còn dữ liệu cũ đến hết TTL; không `FLUSHALL` Redis vì Redis còn giữ khóa ghế.

V1 tạo schema, V2 nạp seed, các phiên bản sau đồng bộ schema hiện tại. Với database đã ghi nhận V1/V2, restart hoặc build lại không chạy lại seed. Baseline 0 trong profile dev cho phép V1 vẫn được thực hiện khi schema dùng chung đã có bảng của dịch vụ khác; baseline không xác nhận bảng cũ đã đúng.

### Danh mục 40 phim để xem phân trang và tìm kiếm

Sau khi Catalog và Elasticsearch đã khởi động, chạy từ thư mục BE:

```bash
docker exec -i cgv-postgres psql -v ON_ERROR_STOP=1 -U postgres -d cgv < scripts/seed-search-movies.sql
python3 scripts/sync-movie-search.py
curl -s 'http://localhost:8000/api/v1/catalogs/movies?page=4&size=12'
curl -s 'http://localhost:8000/api/v1/catalogs/movies/search?keyword=Incepton&size=50'
```

SQL nạp 40 phim có thật với UUID cố định; ngày phát hành tham khảo Wikidata (ưu tiên Hoa Kỳ), poster tham chiếu Wikipedia/TMDb. Không phải ngày chiếu tại CGV; phim lưu `ENDED`, không có suất chiếu hay vé. `end_date` để trống, tác vụ đồng bộ không tự đưa phim lưu trữ trở lại `NOW_SHOWING`. Script chỉ cập nhật bản ghi cũ còn synopsis demo; không ghi đè phim sửa tay. Ảnh hotlink thuộc quyền tác giả/nguồn ngoài, có thể mất hoặc bị giới hạn tải; không tuyên bố poster CGV chính thức. Vì ghi SQL không phát sự kiện Spring, script thứ hai nạp lại phim từ PostgreSQL sang index `movies` có analyzer hiện hữu; không xóa Redis hoặc dữ liệu vé. Trên DB local ngày 06/11/2026: 9 phim cũ + 40 phim tham khảo = 49 phim/49 tài liệu index; API cỡ 12 trả 5 trang. FE hiển thị phân trang ở danh mục, ô tìm kiếm gọi API Elasticsearch. Bộ 49 phim chỉ chứng minh chức năng; không đủ để kết luận Elasticsearch nhanh hơn SQL. Kết quả lệ thuộc dữ liệu local và số bản ghi khác khi chạy trên máy mới.

Admin quản lý tài khoản qua Identity/Keycloak: tạo, sửa, thay vai trò, khóa/mở và xóa mềm. Xóa mềm giữ đơn/vé trong DB, tắt tài khoản Keycloak; không xóa vật lý. Chỉ role có thật trong realm (`SUPER_ADMIN`, `CINEMA_MANAGER`, `TICKET_STAFF`, `CONTENT_MANAGER`, `MEMBER_USER`) được chọn; màn hình hiển thị `USER` thay `MEMBER_USER`. Smoke test tạo một tài khoản tạm và để lại bản ghi `DELETED`:

```bash
CGV_TEST_ADMIN_EMAIL=... CGV_TEST_ADMIN_PASSWORD=... python3 scripts/check-admin-user-flow.py
```

Đăng nhập thường và Google vẫn được giữ; cấu hình Facebook đã bỏ. Khóa/logout Keycloak chặn login mới, nhưng JWT đã phát có thể còn hiệu lực ở dịch vụ chỉ xác thực chữ ký cho tới khi hết hạn; môi trường thật cần cơ chế kiểm thu hồi tập trung.

Chuẩn bị dữ liệu cho một lần demo:

```bash
python3 scripts/demo.py prepare
```

Script chọn suất chiếu tương lai và một ghế thực sự thuộc phòng của suất chiếu, chưa có allocation trong Booking và chưa có khóa Redis. Nếu các suất chiếu seed đều đã qua ngày, script tạo **một suất chiếu fixture dành cho demo** từ phim/phòng/giá có sẵn, bắt đầu sau khoảng một giờ. Việc này được ghi rõ bằng `created_showtime_fixture` trong `session.json`; không sửa V1/V2 và không tạo booking/payment giả.

UUID, nhãn ghế và đường dẫn output được in ra. Mỗi lần `prepare` tạo một thư mục mới `target/demo-runs/<RUN_ID>/`. `target/demo-current.json` trỏ tới lần đang chọn. Script không xóa khóa của người đang giữ ghế.

## 6. Trình diễn luồng giữ/nhả ghế

```bash
python3 scripts/demo.py smoke
```

Trình tự kiểm tra thực tế:

1. A giữ ghế: HTTP 200, Redis ghi chủ A và TTL dương, tối đa 240 giây.
2. B giữ cùng ghế: HTTP 409; chủ sở hữu vẫn là A.
3. B yêu cầu nhả ghế A: API có thể trả HTTP 200 nhưng Redis vẫn giữ khóa A; chỉ HTTP status không đủ xác nhận đã xóa khóa.
4. A nhả ghế: Redis không còn khóa.
5. B giữ lại được, sau đó B nhả ghế.

Kết quả từng bước và JSON phản hồi được lưu vào `smoke.json`. Script dọn các khóa do chính phiên demo A/B vừa tạo qua API nhả ghế. Đây là kiểm tra HTTP trên ứng dụng đang chạy, chưa tạo booking hoặc thanh toán.

## 6a. Kiểm tra ghế đã vào đơn sau TTL

Sau khi `start`, chạy `prepare` riêng cho phép thử này; không dùng ghế đã bị giữ từ lần k6 khác:

```bash
python3 scripts/demo.py prepare
python3 scripts/demo.py integrity
```

`integrity` dùng Kong, Catalog, Redis, PostgreSQL thật và hai JWT khác chủ: A giữ ghế, tạo đơn PAYMENT_PENDING/SeatClaim; rút TTL xuống 1 giây, B vẫn bị 409. Kịch bản này chỉ an toàn khi A chưa tạo Payment PENDING; nếu đã có giao dịch VNPay chưa đối soát thì không tự hủy để giải phóng. Output trong `target/demo-runs/<RUN_ID>/integrity.json`; chưa chạy lại bản JWT.

Để chạy `smoke`/`integrity`, đặt `SMOKE_TOKENS_FILE` tới JSON array chứa hai JWT hợp lệ của hai tài khoản khác nhau. `load` cần `TOKENS_FILE` là JSON array của 1.000 JWT hợp lệ từ 1.000 tài khoản khác nhau; không lưu file token trong Git. Dùng `export SMOKE_TOKENS_FILE=/path/to/two-tokens.json` và `export TOKENS_FILE=/path/to/1000-tokens.json` trước khi chạy. Script đọc `sub` để đối chiếu Redis; BE tự xác minh chữ ký. Token hết hạn sẽ làm phép thử thất bại. Payment vẫn đối chiếu chủ, giá và hạn của đơn trong DB; chưa chạy thanh toán VNPay sandbox end-to-end với bản JWT.

## 7. Chạy k6: 1.000 request tranh một ghế

Sau `prepare` và `smoke`:

```bash
python3 scripts/demo.py load
```

Kịch bản tạo 1.000 VU, mỗi VU gửi đúng một POST tới cùng showtimeId/seatId, với định danh chủ sở hữu khác nhau. Không retry, không theo redirect và không gửi request giữ ghế trong `setup`.

Tiêu chí chấp nhận, **không phải số liệu mặc định**:

- `requests = 1000`
- `success = 1`
- `conflicts = 999`
- `unexpected = 0`
- Tất cả threshold đạt; k6 exit code bằng 0.
- Redis ngay sau chạy có đúng `WINNER_USER_ID`; TTL còn dương và không quá 240 giây.

Script kiểm tra hash JAR local với JAR đang chạy trong container trước khi đo. Nếu không khớp, chạy lại `start`. Nếu một tiêu chí thất bại, script trả mã lỗi và giữ nguyên output. Một thư mục đã có output k6 không bị ghi đè: chạy `prepare` để tạo lần mới.

Các tệp trong thư mục của lần chạy:

| Tệp | Nội dung |
| --- | --- |
| `session.json` | RUN_ID, UUID suất chiếu/ghế, nhãn ghế và nguồn dữ liệu demo. |
| `smoke.json` | Phản hồi từng bước giữ/nhả ghế nếu đã chạy smoke. |
| `environment.json` | Thời điểm, hệ điều hành, CPU/RAM, tài nguyên Docker, image ID và snapshot tài nguyên. |
| `source-manifest.json`, `runtime-jar.json` | Hash source và đối chiếu JAR local/container. |
| `load-generator.json` | Phiên bản k6, đối số chạy và thời điểm bắt đầu. |
| `k6-console.log`, `k6-summary.json`, `k6-raw.json` | Console, metrics tổng hợp và mẫu đo chi tiết. |
| `result.json` | Các bộ đếm và P95/P99/request rate lấy từ k6. |
| `redis-before.json`, `redis-after.json` | Chủ sở hữu và TTL trước/sau lần chạy. |
| `verification.json` | Đối chiếu tiêu chí, người thắng và Redis; trạng thái đạt/thất bại. |

P95/P99 là thời gian HTTP của k6 qua Gateway/Booking, bao gồm gửi, chờ và nhận phản hồi; không bao gồm DNS/kết nối ban đầu trong metric `http_req_duration`. Phân vị gộp chủ yếu phản ánh 999 phản hồi xung đột nếu kịch bản đạt, không phải thời gian riêng của Lua hoặc giao dịch thanh toán.

Kết quả tham chiếu và lỗi DTO đã sửa được ghi tại [LOCAL_RUN_20261022.md](docs/LOCAL_RUN_20261022.md); các số liệu trong đó chỉ thuộc RUN_ID được nêu.

## 8. Chụp màn hình để đưa vào báo cáo

- Ảnh môi trường: `doctor` và `status`, kèm ghi nhận tài nguyên Docker Desktop.
- Ảnh demo: console của `smoke`, thấy các bước A/B và trạng thái Redis.
- Ảnh k6: console phần JSON kết quả, `WINNER_USER_ID` và phần đối chiếu Redis ngay sau chạy.

Hiển thị lại **output đã lưu** để chụp mà không chạy lại phép đo:

```bash
python3 scripts/demo.py results
```

`redis-after.json` là ảnh chụp dữ liệu ngay sau lần đo. Khi xem lại sau 240 giây, khóa live có thể đã hết hạn; điều đó không thay đổi bản ghi đã lưu. Ảnh đưa vào báo cáo phải thuộc cùng RUN_ID với bảng kết quả, không lấy ảnh của lần khác.

## 9. Lỗi thường gặp

| Hiện tượng | Cách xử lý |
| --- | --- |
| Docker daemon chưa sẵn sàng | Mở Docker Desktop, đợi Engine rồi chạy `doctor`. |
| Java không phải 17 | Chỉnh JAVA_HOME/PATH; kiểm tra cả `java -version` và `mvn -v`. |
| Cổng/container trùng | Dùng `docker ps` kiểm tra stack đang chạy; không khởi động một bản thứ hai cùng tên/cổng. |
| Build lỗi tải dependency | Kiểm tra mạng tới Maven repository; đọc lỗi build, chưa kết luận service đã sẵn sàng. |
| Gateway 502 hoặc readiness timeout | Đọc log Booking/Catalog/Kong bên dưới; kiểm tra migration và kết nối PostgreSQL/Redis. |
| Flyway checksum/schema validation lỗi | Giữ log và dữ liệu; đối chiếu migration đã áp dụng. Không xóa volume hoặc sửa lịch sử để bỏ qua lỗi. |
| Không có seed Catalog | Chờ Catalog khởi động, kiểm tra lịch sử Flyway; `prepare` cần phim/phòng/ghế có thật. |
| Ghế đã có khóa/đã có output | Chạy `prepare` để chọn ghế/lần chạy mới. |
| K6 có timeout/5xx/thiếu request | Giữ output thất bại, kiểm tra tài nguyên và log trước khi sửa/chạy lại. |

```bash
docker compose --env-file .env.demo -f docker-compose-dev.yml logs --tail=100 booking-service catalog-service kong
docker exec cgv-postgres psql -U postgres -d cgv -c "SELECT version, description, success FROM flyway_booking_schema_history ORDER BY installed_rank;"
docker exec cgv-redis redis-cli PING
```

## 10. Dừng và chạy lại

```bash
python3 scripts/demo.py stop
```

Lệnh dừng các thành phần của lát cắt, giữ volume PostgreSQL/Redis và kết quả trong `target`. Lần sau dùng `start`, `prepare`, `smoke`, rồi `load`. Đừng dùng `down -v` như bước khởi động thông thường vì nó xóa dữ liệu volume.

## 11. Outbox/CDC và phạm vi mở rộng

Luồng thanh toán của BE ghi Payment/Booking và Outbox trong transaction cục bộ; Debezium đọc WAL để phát Kafka, không dùng OutboxRelay polling. Cấu hình và trình tự đăng ký connector có tại `docs/CDC_LOCAL.md`. Lát cắt giữ ghế không tự đăng ký connector hoặc thực hiện thanh toán. Không suy kết quả k6 thành bằng chứng Saga/CDC đã chạy thành công.

## 12. Các script cần dùng

Khi thử thanh toán bằng tài khoản đăng nhập, cấu hình `BREVO_API_KEY` và `EMAIL` cho NotificationService và IPN HTTPS công khai `/api/v1/payments/vnpay-ipn` tại VNPay. Return URL không thay thế IPN. `vnp_ExpireDate` theo hạn đơn; Payment còn `PENDING` thì giữ claim ghế tới khi đối soát. Email/QR lấy địa chỉ từ claim `email` của JWT (nếu có); thiếu claim này không bảo đảm gửi thư, nhưng chủ đơn xem vé trong tài khoản. Dự án chưa tự gọi QUERYDR/REFUND; không hứa hoàn tiền hoặc tự nhả ghế sau 10 phút. Không dùng k6 giữ ghế làm bằng chứng thanh toán.

| Script | Vai trò | Khi nào chạy |
| --- | --- | --- |
| `scripts/demo.py` | Kiểm tra môi trường, tạo cấu hình local, build/khởi động, chuẩn bị ghế, kiểm tra HTTP và chạy k6 | Theo trình tự trong README này |
| `scripts/init-kafka-topics.sh` | Tạo topic nghiệp vụ, DLT và topic nội bộ Kafka Connect | Khi chuẩn bị Kafka/CDC theo `docs/CDC_LOCAL.md` |
| `scripts/register-outbox-cdc.py` | Đăng ký/cập nhật hai connector Outbox và đọc trạng thái | Khi chạy luồng Outbox/CDC, sau khi database/topic/worker đã sẵn sàng |

`scripts/init-keycloak-schema.sql` là SQL khởi tạo schema Keycloak khi volume PostgreSQL còn mới; Docker Compose mount và gọi qua cơ chế init của image PostgreSQL. Các migration và dữ liệu mẫu của dịch vụ nằm trong `src/main/resources/db/migration/` và được Flyway quản lý khi ứng dụng khởi động.

## 13. Chuẩn bị source và kết quả để nộp

Bộ nộp gồm tám module (bảy dịch vụ và CommonDTO), POM gốc, Dockerfile/cấu hình Docker local, các script ở trên, kịch bản `k6/seat-lock-1000.js`, README và cấu hình mẫu `.env.demo.example`. Kèm báo cáo LaTeX/PDF, ảnh được báo cáo sử dụng và bài lab. Cấu hình CDC nằm trong `debezium/`; hướng dẫn tại `docs/CDC_LOCAL.md`.

Chọn output của từng lần đo cần nộp từ `target/demo-runs/RUN_ID/`, giữ nguyên tên và nội dung để phân biệt các lần chạy. Không chép toàn bộ build cache, `.env`, `.env.demo`, credential hoặc cấu hình production vào bộ source. Chỉ sử dụng cấu hình mẫu local; thông tin nhà cung cấp ngoài được cấu hình riêng khi cần. Đóng gói và đặt tên theo quy định học phần. Các bước chạy lại vẫn là `doctor`, `init`, `start`, `prepare`, `smoke`, `load`, `results` từ thư mục Backend.
