# HƯỚNG DẪN CHI TIẾT: RESET DATABASE VÀ NẠP DỮ LIỆU MẪU (SEED DATA & KEYCLOAK) TRÊN AWS

Tài liệu này cung cấp toàn bộ quy trình chuẩn xác để:
1. **Làm sạch (Reset)** toàn bộ dữ liệu kiểm thử cũ trên hệ thống AWS RDS PostgreSQL.
2. **Khởi tạo lại cấu trúc bảng (Hibernate DDL Auto)** và nạp **Master Seed Data** (Rạp, Phòng chiếu, 480 Ghế ngồi, Phim, Suất chiếu hôm nay, Voucher khuyến mãi).
3. **Reset và Tự động Import lại toàn bộ Keycloak IAM Realm** cùng **4 tài khoản quản trị mẫu doanh nghiệp** (Mật khẩu: `12345678`).

---

## 1. THÔNG TIN KIẾN TRÚC & HẠ TẦNG DỮ LIỆU TRÊN AWS

- **AWS RDS PostgreSQL Engine**: PostgreSQL 16 (Multi-AZ)
- **RDS Endpoint**: `cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com`
- **Port**: `5432`
- **Database Name**: `cgv`
- **Username**: `postgres`
- **Password**: `CGVPassword2026`
- **Kubernetes Namespace**: `cgv-prod`
- **Cấu trúc phân tách Schema (Best Practice)**:
  - **Schema `public`**: Chứa toàn bộ 25 bảng nghiệp vụ của hệ thống Microservices (`cinemas`, `rooms`, `seats`, `movies`, `showtimes`, `bookings`, `payments`, `promotions`, `users`...).
  - **Schema `keycloak`**: Chứa riêng biệt 121 bảng nội bộ của hệ thống xác thực Keycloak IAM (Quản lý User, Role, OAuth2 Social Google/Facebook, Client Keys...).

> [!NOTE]
> Do AWS RDS nằm trong **Private Subnet** (mạng riêng an toàn không mở cổng public ra ngoài Internet), cách tốt nhất và nhanh nhất để tương tác với cơ sở dữ liệu là thông qua một **Pod PostgreSQL Client tạm thời (`pgclient`)** chạy ngay trong cụm EKS.

---

## 2. CÁCH 1: RESET TỰ ĐỘNG CHỈ VỚI 1 DÒNG LỆNH (KHUYÊN DÙNG ⭐)

Hệ thống đã được trang bị sẵn script tự động hóa toàn bộ quy trình:
- Tự bật pod client tạm thời trong cụm EKS.
- Xóa sạch schema `public` và schema `keycloak`.
- Restart các microservices để sinh lại cấu trúc bảng.
- Nạp toàn bộ file `scripts/seed-data.sql`.
- Cập nhật ConfigMap và restart Keycloak để tự động import lại toàn bộ realm và tài khoản mẫu.
- Tự động dọn dẹp pod client sau khi hoàn thành.

### Lệnh thực thi:
Mở Terminal tại thư mục gốc của dự án (`CGV`):
```bash
chmod +x scripts/aws-reset-db-and-keycloak.sh
./scripts/aws-reset-db-and-keycloak.sh
```

---

## 3. CÁCH 2: THỰC HIỆN THỦ CÔNG TỪNG BƯỚC BẰNG TERMINAL (KUBECTL & PSQL)

Nếu bạn muốn kiểm soát chi tiết từng bước, hãy làm theo hướng dẫn dưới đây:

### PHẦN A: RESET & NẠP LẠI DATABASE MICROSERVICES (SCHEMA `public`)

#### Bước A.1: Khởi tạo Pod PostgreSQL Client tạm thời
```bash
kubectl run pgclient --image=postgres:16-alpine --restart=Never -n cgv-prod -- sleep 3600
kubectl wait --for=condition=Ready pod/pgclient -n cgv-prod --timeout=60s
```

#### Bước A.2: Xóa sạch dữ liệu và cấu trúc bảng cũ (Reset Schema `public`)
```bash
kubectl exec -i -n cgv-prod pgclient -- sh -c "PGPASSWORD=CGVPassword2026 psql -h cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com -U postgres -d cgv -c '
    DROP SCHEMA IF EXISTS public CASCADE;
    CREATE SCHEMA public;
    GRANT ALL ON SCHEMA public TO postgres;
    GRANT ALL ON SCHEMA public TO public;
'"
```

#### Bước A.3: Kích hoạt Microservices tự động tạo lại các bảng (Hibernate DDL Auto)
Do các dịch vụ Spring Boot có cấu hình `spring.jpa.hibernate.ddl-auto: update`, việc restart pod sẽ giúp Hibernate quét các JPA Entity và tự động tạo lại toàn bộ 25 bảng:
```bash
kubectl rollout restart deployment \
    identity-service \
    catalog-service \
    booking-service \
    payment-service \
    marketing-service \
    -n cgv-prod

# Chờ Catalog Service & Identity Service khởi động xong để hoàn tất tạo bảng
kubectl rollout status deployment/catalog-service -n cgv-prod --timeout=120s
kubectl rollout status deployment/identity-service -n cgv-prod --timeout=120s
```

#### Bước A.4: Nạp Master Seed Data vào Database RDS
```bash
# 1. Copy file seed-data.sql từ máy tính vào pod pgclient
kubectl cp scripts/seed-data.sql cgv-prod/pgclient:/tmp/seed-data.sql

# 2. Thực thi script SQL vào RDS
kubectl exec -i -n cgv-prod pgclient -- sh -c "PGPASSWORD=CGVPassword2026 psql -h cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com -U postgres -d cgv -f /tmp/seed-data.sql"
```

#### Bước A.5: Kiểm tra số lượng dữ liệu đã nạp vào RDS
```bash
kubectl exec -i -n cgv-prod pgclient -- sh -c "PGPASSWORD=CGVPassword2026 psql -h cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com -U postgres -d cgv -c '
    SELECT 
        (SELECT count(*) FROM cinemas) as tong_cum_rap,
        (SELECT count(*) FROM rooms) as tong_phong_chieu,
        (SELECT count(*) FROM seats) as tong_ghe_ngoi,
        (SELECT count(*) FROM movies) as tong_phim,
        (SELECT count(*) FROM showtimes) as tong_suat_chieu,
        (SELECT count(*) FROM promotions) as tong_khuyen_mai;
'"
```
*Kết quả kỳ vọng:* 6 cụm rạp, 6 phòng chiếu, 480 ghế ngồi, 5 phim bom tấn, đầy đủ suất chiếu hôm nay và khuyến mãi hoạt động.

---

### PHẦN B: RESET & NẠP LẠI KEYCLOAK IAM (SCHEMA `keycloak` & REALM)

#### Bước B.1: Cập nhật ConfigMap Realm mới nhất (`keycloak-realm-config`)
Đảm bảo ConfigMap trên cụm EKS chứa đầy đủ 4 tài khoản quản trị mẫu và cấu hình Google/Facebook OAuth:
```bash
kubectl create configmap keycloak-realm-config \
    --from-file=cgv-realm-export.json=keycloak/cgv-realm-export.prod.json \
    -n cgv-prod \
    --dry-run=client -o yaml | kubectl apply -f -
```

#### Bước B.2: Xóa sạch Schema `keycloak` cũ trong RDS
```bash
kubectl exec -i -n cgv-prod pgclient -- sh -c "PGPASSWORD=CGVPassword2026 psql -h cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com -U postgres -d cgv -c '
    DROP SCHEMA IF EXISTS keycloak CASCADE;
    CREATE SCHEMA keycloak;
'"
```

#### Bước B.3: Khởi động lại Pod Keycloak để kích hoạt Auto-Import Realm
Keycloak được cấu hình với cờ `--import-realm`. Khi nhận thấy schema `keycloak` vừa được tạo mới và chưa có realm `cgv-realm`, Keycloak sẽ tự động import file từ ConfigMap:
```bash
kubectl rollout restart deployment/keycloak -n cgv-prod
kubectl rollout status deployment/keycloak -n cgv-prod --timeout=180s
```

#### Bước B.4: Kiểm tra trạng thái Keycloak Pod
```bash
kubectl logs -l app=keycloak -n cgv-prod --tail=50 | grep -i "realm 'cgv-realm' imported"
```

---

### PHẦN C: DỌN DẸP TÀI NGUYÊN (CLEANUP)
Sau khi nạp dữ liệu xong, xóa pod client tạm thời để giải phóng tài nguyên cụm:
```bash
kubectl delete pod pgclient -n cgv-prod --grace-period=0 --force
```

---

## 4. CÁCH 3: KẾT NỐI VÀ THAO TÁC QUA GIAO DIỆN DBEAVER / PGADMIN TỪ MÁY LOCAL

Nếu bạn muốn xem trực quan các bảng hoặc chạy script bằng công cụ đồ họa (GUI):

### Bước 1: Mở cầu nối mạng (Port-Forward) từ máy tính vào RDS
Chạy lệnh sau trên terminal của máy tính:
```bash
# Bật pod client nếu chưa có
kubectl run dbeaver-bridge --image=postgres:16-alpine --restart=Never -n cgv-prod -- sleep 86400

# Chuyển tiếp cổng 5432 từ cụm EKS về máy tính cá nhân
kubectl port-forward -n cgv-prod pod/dbeaver-bridge 5432:5432
```
*(Giữ cửa sổ terminal này luôn mở khi đang dùng DBeaver)*

### Bước 2: Cấu hình kết nối trên DBeaver / pgAdmin
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `cgv`
- **Username**: `postgres`
- **Password**: `CGVPassword2026`

### Bước 3: Thực thi Script trên DBeaver
1. Mở file `scripts/seed-data.sql` trong DBeaver (File -> Open File).
2. Chọn kết nối vừa tạo.
3. Nhấn tổ hợp phím `Alt + X` (Execute SQL Script) để nạp toàn bộ dữ liệu.

---

## 5. DANH SÁCH TÀI KHOẢN MẪU & THÔNG TIN ĐĂNG NHẬP

Tất cả các tài khoản doanh nghiệp đều có chung mật khẩu: **`12345678`**

| Vai trò (Role) | Email đăng nhập | Quyền hạn trên hệ thống | Giao diện sử dụng |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `superadmin.headquarters.enterprise@cgv.vn` | Toàn quyền quản trị hệ thống, quản lý rạp, phim, suất chiếu, người dùng, khuyến mãi, cấu hình | [Admin Portal](https://cgvfeadmin.vercel.app) |
| **Cinema Manager** | `cinemamanager.vincom.dongkhoi@cgv.vn` | Quản lý lịch chiếu, quản lý phòng chiếu, theo dõi doanh thu cụm rạp được phân công | [Admin Portal](https://cgvfeadmin.vercel.app) |
| **Ticket Staff** | `ticketstaff.boxoffice.crescentmall@cgv.vn` | Bán vé tại quầy (POS Box Office), quét mã QR vé, in vé, kiểm soát phòng chiếu | [Admin Portal](https://cgvfeadmin.vercel.app) |
| **Content Lead** | `marketing.contentlead.digital@cgv.vn` | Quản lý danh mục phim, poster, banner trailer, tạo chiến dịch voucher khuyến mãi | [Admin Portal](https://cgvfeadmin.vercel.app) |
| **Thành viên cá nhân** | `lhhuy.2005@gmail.com` | Đặt vé xem phim, giữ ghế thời gian thực, tích điểm hạng GOLD, nhận email vé qua Brevo | [User Web](https://cgvfeuser.vercel.app) |

---

## 6. KIỂM THỬ NHANH SAU KHI RESET HỆ THỐNG (SANITY CHECKS)

### 1. Kiểm tra API Catalog qua CloudFront Gateway:
```bash
# Tìm rạp gần tôi (Tọa độ Quận 1 TP.HCM)
curl -s "https://d2z63drupmmdh8.cloudfront.net/api/v1/cinemas/nearby?lat=10.7778&lon=106.7025" | head -n 30

# Lấy danh sách phim đang chiếu
curl -s "https://d2z63drupmmdh8.cloudfront.net/api/v1/movies/now-showing" | head -n 30
```

### 2. Kiểm tra đăng nhập tài khoản quản trị (Nhận mã JWT 200 OK):
```bash
curl -X POST "https://d2z63drupmmdh8.cloudfront.net/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin.headquarters.enterprise@cgv.vn","password":"12345678"}'
```

---
*Tài liệu được cập nhật chuẩn xác theo kiến trúc microservices và cụm AWS EKS / RDS Production của hệ thống CGV.*
