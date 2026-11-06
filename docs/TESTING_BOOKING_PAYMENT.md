# Chay kiem thu Booking, Payment va k6 (local)

## IntelliJ IDEA

1. Mo thu muc BE `CGV/` (khong mo rieng `bookingservice/`); import root `pom.xml` bang Maven. Chon JDK 17, bat annotation processing cho Lombok. Maven tool window -> Reload All Maven Projects.
2. Chay tung class trong `bookingservice/src/test/java/com/cgv/bookingservice/` va `paymentservice/src/test/java/com/cgv/paymentservice/` bang nut xanh canh ten class. `BookingIntegrityIntegrationTest` va `PaymentCallbackIntegrationTest` dung H2, mock cac phu thuoc ben ngoai; **khong** chung minh PostgreSQL/VNPay that. `SeatLockLuaIntegrationTest`/`SeatLockHttpIntegrationTest` tu bat Redis rieng: cai `redis-server` trong PATH hoac dat `REDIS_SERVER_BIN` trong Run Configuration. HTTP test mock Catalog/DB va decoder JWT.
3. Co the chay Maven tu Terminal IntelliJ (tai root BE):

```bash
mvn -pl bookingservice -am -Dtest=BookingIntegrityIntegrationTest,SeatLockLuaIntegrationTest,SeatLockHttpIntegrationTest,BookingControllerWebMvcTest,SeatLockIdentityWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl paymentservice -am -Dtest=PaymentCallbackIntegrationTest,BookingPaymentAccessTest,PaymentControllerWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Xem tong hop trong `bookingservice/target/surefire-reports/` va `paymentservice/target/surefire-reports/`. Khong lay log cua lan truoc lam ket qua lan nay. Neu chay `mvn test` toan bo, `contextLoads` can PostgreSQL local va cac bien moi truong cua tung service; chi thong bao dat khi tat ca class thuc su dat.

Khi Docker local/PostgreSQL dang san sang, chay du bo: `mvn -pl bookingservice,paymentservice -am test`. `VnpaySignatureTest` kiem HMAC that voi secret test; khong dung credential VNPay that.

`BookingSeatClaimPostgresTest` co y **skip** neu khong dat `CGV_TEST_PG_URL`. De chay PostgreSQL race tren DB local (khong dung production), dat `CGV_TEST_PG_URL=jdbc:postgresql://localhost:5432/cgv`, `CGV_TEST_PG_USER`, `CGV_TEST_PG_PASSWORD` khop **container PostgreSQL dang chay** trong Environment variables cua IntelliJ/Maven. Khong copy password vao file test/bao cao. Test chen hai booking fixture UUID ngau nhien, hai transaction tranh mot `(showtime_id, seat_id)` va xoa fixture trong `finally`; can DB da chay migration V3. Kiem `skipped=0` trong Surefire XML khi muon nhan la da chay PostgreSQL that. Khong tu xoa bang hay reset DB nguoi dung.

## PostgreSQL/Redis/Kong that

Start Docker Compose local theo `README.md`; doi Keycloak, Catalog gRPC, Booking va Gateway san sang. Dung hai JWT **khac subject**, chua het han; luu array JSON tai file ngoai Git (mode 0600). Khong test tren he thong production, khong tao giao dich VNPay that.

```bash
export SMOKE_TOKENS_FILE=/path/to/two-valid-tokens.json
python3 scripts/demo.py prepare
python3 scripts/demo.py smoke
python3 scripts/demo.py prepare
python3 scripts/demo.py integrity
```

`integrity` tao booking PAYMENT_PENDING va SeatClaim trong PostgreSQL; rut TTL Redis 1 giay, chung minh B khong giu/tao don trung, huy A roi B giu lai. Chi huy booking cua fixture neu chua co VNPay PENDING; booking/seat history van con. Moi lan `prepare` tao RUN_ID moi va khong xoa lock nguoi khac. Xem `target/demo-runs/<RUN_ID>/integrity.json` (co `passed` va tung buoc), khong chi nhin HTTP 200. Ca nay chay tuan tu; 2 transaction PostgreSQL tao don dong thoi chua duoc chung minh.

## k6 tren JWT that

`k6/seat-lock-1000.js` mac dinh 1.000 VU/1.000 JWT khac subject, 1 POST/VU tranh **mot ghe**. Co the chay thang nho voi `SEAT_REQUESTS=50` de kiem readiness; thong ke cua 50 khong duoc ghi la 1.000. Chi chay `load` khi Catalog gRPC hoat dong on dinh, JWT con han, ghe chua co claim/lock. Khong gom payment vao chi so nay.

```bash
export TOKENS_FILE=/path/to/distinct-valid-tokens.json
python3 scripts/demo.py prepare
SEAT_REQUESTS=50 python3 scripts/demo.py load
python3 scripts/demo.py prepare
SEAT_REQUESTS=1000 python3 scripts/demo.py load
```

Tuy chon local: `scripts/k6-test-users.py --count 50 --output /private/path/tokens.json` tao tai khoan Keycloak tam (can `K6_ADMIN_USER`, `K6_ADMIN_PASSWORD`, `K6_CLIENT_SECRET` trong environment; **khong** ghi vao Git). Script ghi manifest `.accounts.json` cung duong dan va file JWT quyen 0600. Chay ngay vi JWT co TTL. Nha lock ghe test bang JWT nguoi thang truoc khi xoa tai khoan. Don dung cac tai khoan fixture bang `scripts/k6-test-users.py --cleanup /private/path/tokens.accounts.json` voi cung credentials admin; lenh nay chi xoa cac ID trong manifest. Xoa file JWT sau khi chay. Neu provision loi, van kiem manifest truoc khi cleanup; 1.000 tai khoan co the vuot tai Keycloak local, khong coi so tai khoan vua tao la so request k6. Khong chay tren realm dung chung/production.

Ket qua: `result.json` ghi so request, 200, 409, unexpected, p95/p99 va thresholds; `verification.json` so khop JWT chu thang voi Redis owner/TTL. `k6-raw.json`, `k6-summary.json`, `environment.json`, `k6-console.log` giu de doi chieu. K6 nhieu VU tren mot may khong phai benchmark cho booking/payment end-to-end; VNPay callback trong JUnit duoc gia lap request va chu ky qua config mock, khong co thanh toan that hay hoan tien that.
