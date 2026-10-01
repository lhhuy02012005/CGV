# TỔNG HỢP DANH SÁCH TÀI KHOẢN TEST TRONG HỆ THỐNG CGV (LOCAL & AWS PROD)

Tài liệu này tổng hợp toàn bộ các tài khoản thử nghiệm có sẵn trong hệ thống xác thực **Keycloak IAM** và cơ sở dữ liệu **PostgreSQL** (`users` table), áp dụng cho cả môi trường **Local Development (Docker)** và **Production (AWS EKS & RDS)**.

---

## 1. BẢNG TỔNG HỢP NHANH TẤT CẢ TÀI KHOẢN (QUICK REFERENCE)

| STT | Loại tài khoản | Email / Username | Mật khẩu | Role Keycloak | Hạng hội viên | Môi trường | Giao diện đăng nhập |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **1** | **Super Admin** | `superadmin.headquarters.enterprise@cgv.vn` | `12345678` | `SUPER_ADMIN` | `PLATINUM` | **Local & Prod** | [FE Admin](https://cgvfeadmin.vercel.app) |
| **2** | **Cinema Manager** | `cinemamanager.vincom.dongkhoi@cgv.vn` | `12345678` | `CINEMA_MANAGER` | `GOLD` | **Local & Prod** | [FE Admin](https://cgvfeadmin.vercel.app) |
| **3** | **Ticket Staff** | `ticketstaff.boxoffice.crescentmall@cgv.vn` | `12345678` | `TICKET_STAFF` | `SILVER` | **Local & Prod** | [FE Admin](https://cgvfeadmin.vercel.app) |
| **4** | **Content Lead** | `marketing.contentlead.digital@cgv.vn` | `12345678` | `CONTENT_MANAGER` | `GOLD` | **Local & Prod** | [FE Admin](https://cgvfeadmin.vercel.app) |
| **5** | **Khách hàng VIP** | `lhhuy.2005@gmail.com` | `12345678` | `MEMBER_USER` | `GOLD` | **Local & Prod** | [FE User](https://cgvfeuser.vercel.app) |
| **6** | **Khách hàng VVIP** | `admin@gmail.com` | `12345678` | `SUPER_ADMIN` | `PLATINUM` | **Local & Prod** | [FE User](https://cgvfeuser.vercel.app) / Admin |
| **7** | **Khách hàng mới** | `lehuuhuy211405@gmail.com` | `12345678` | `MEMBER_USER` | `MEMBER` | **Local & Prod** | [FE User](https://cgvfeuser.vercel.app) |
| **8** | **Mock SuperAdmin** | `admin_test` *(hoặc `admin`)* | *Tùy ý* | `SUPER_ADMIN` | — | **Chỉ Local (Mock)** | [FE Admin Local](http://localhost:5174) |

---

## 2. CHI TIẾT CÁC TÀI KHOẢN DOANH NGHIỆP (ENTERPRISE ADMIN & STAFF)

Tất cả các tài khoản doanh nghiệp đều có chung mật khẩu: **`12345678`**  
Được cấu hình sẵn trong Keycloak (`cgv-realm-export.prod.json` & `cgv-realm-export.json`) và bảng `users` trong database.

### 2.1. Super Admin (Tổng Quản Trị Hệ Thống)
- **Email**: `superadmin.headquarters.enterprise@cgv.vn`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Enterprise CGV SuperAdmin`
- **Nhóm Keycloak (Group)**: `/CGV_BoardOfDirectors`
- **Vai trò (Realm Roles)**: `SUPER_ADMIN`
- **Quyền hạn chi tiết**:
  - Toàn quyền truy cập mọi tính năng trên Dashboard quản trị.
  - Quản lý danh mục phim, kiểm duyệt và phát hành phim mới.
  - Quản lý mạng lưới rạp chiếu, sơ đồ phòng chiếu và ma trận ghế.
  - Quản lý lịch chiếu toàn quốc, kiểm soát giá vé và phụ thu định dạng (IMAX, 4DX, Sweetbox).
  - Phân quyền tài khoản nhân viên, xem báo cáo doanh thu tài chính tổng hợp.

### 2.2. Cinema Manager (Quản Lý Cụm Rạp)
- **Email**: `cinemamanager.vincom.dongkhoi@cgv.vn`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Dong Khoi Manager Vincom`
- **Nhóm Keycloak (Group)**: `/CGV_Vincom_DongKhoi_Managers`
- **Vai trò (Realm Roles)**: `CINEMA_MANAGER`
- **Cụm rạp phụ trách**: **CGV Vincom Đồng Khởi** (Quận 1, TP.HCM)
- **Quyền hạn chi tiết**:
  - Quản lý các phòng chiếu (`Cinema 1 IMAX Laser`, `Cinema 2 Standard`) tại rạp Đồng Khởi.
  - Sắp xếp và điều chỉnh lịch chiếu phim tại cụm rạp được phân công.
  - Theo dõi tỷ lệ lấp đầy ghế (Occupancy Rate) và doanh thu theo thời gian thực tại rạp.

### 2.3. Ticket Staff (Nhân Viên Quầy Vé & Soát Vé Box Office)
- **Email**: `ticketstaff.boxoffice.crescentmall@cgv.vn`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Crescent Staff BoxOffice`
- **Nhóm Keycloak (Group)**: `/CGV_Vincom_DongKhoi_Staffs`
- **Vai trò (Realm Roles)**: `TICKET_STAFF`
- **Cụm rạp làm việc**: **CGV Crescent Mall** (Quận 7, TP.HCM)
- **Quyền hạn chi tiết**:
  - Bán vé tại quầy (Box Office POS): chọn suất chiếu, chọn ghế, in vé.
  - Quét mã QR vé vào rạp, kiểm tra tính hợp lệ của vé khách hàng.
  - Xử lý các sự cố về ghế và hỗ trợ khách hàng tại chỗ.

### 2.4. Content Lead (Trưởng Phòng Nội Dung & Chiến Dịch Tiếp Thị)
- **Email**: `marketing.contentlead.digital@cgv.vn`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Campaign Lead Marketing Content`
- **Nhóm Keycloak (Group)**: `/CGV_MarketingTeam`
- **Vai trò (Realm Roles)**: `CONTENT_MANAGER`
- **Quyền hạn chi tiết**:
  - Quản lý nội dung phim: trailer, hình ảnh poster, banner trên web/app (Media S3).
  - Thiết lập và quản lý các chiến dịch khuyến mãi (Voucher giảm giá, Combo bắp nước).
  - Quản lý tin tức điện ảnh và sự kiện ưu đãi thành viên.

---

## 3. CHI TIẾT TÀI KHOẢN KHÁCH HÀNG / HỘI VIÊN (END-USERS)

### 3.1. Hội viên Hạng Vàng (GOLD Member)
- **Email**: `lhhuy.2005@gmail.com`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Le Huu Huy`
- **ID trong Database (`users`)**: `c3effc5c-2ee6-4837-bc8a-8d0c9ab0e909`
- **Hạng hội viên**: `GOLD` (Tích lũy chi tiêu: `5,500,000 VND`)
- **Đặc quyền**: Tích 7% điểm thưởng CGV, miễn phí bắp nước sinh nhật.
- **Sử dụng cho test**: Đặt vé xem phim online, chọn và giữ ghế realtime bằng WebSocket/Redis, tích lũy điểm thưởng, nhận vé điện tử qua Email Brevo.

### 3.2. Hội viên Hạng Bạch Kim (PLATINUM Member)
- **Email**: `admin@gmail.com`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `Huy Admin`
- **ID trong Database (`users`)**: `2434983f-0796-423b-a134-e4a24b9e8643`
- **Hạng hội viên**: `PLATINUM` (Tích lũy chi tiêu: `16,000,000 VND`)
- **Đặc quyền**: Tích 10% điểm, quyền vào phòng chờ VIP và vé xem phim miễn phí.

### 3.3. Hội viên Mới Đăng Ký (Tiêu Chuẩn)
- **Email**: `lehuuhuy211405@gmail.com`
- **Mật khẩu**: `12345678`
- **Họ và tên**: `HuyLe`
- **Hạng hội viên**: `MEMBER`
- **Sử dụng cho test**: Quy trình đăng ký tài khoản mới qua OTP Email, xác thực tài khoản và cập nhật hồ sơ cá nhân.

---

## 4. TÀI KHOẢN HẠ TẦNG & QUẢN TRỊ KỸ THUẬT (DEVOPS & DATABASE)

### 4.1. Keycloak Master Admin Console
Dùng để đăng nhập trang quản trị Keycloak để cấu hình Client, Realm, Identity Providers:
- **Môi trường Local**:
  - URL: `http://localhost:8180`
  - Username: `admin` | Password: `admin`
- **Môi trường AWS Production**:
  - URL: `https://d2z63drupmmdh8.cloudfront.net/admin`
  - Username: `admin` | Password: `admin`

### 4.2. Cơ sở dữ liệu PostgreSQL
- **Môi trường Local (Docker)**:
  - Host: `localhost` | Port: `5432`
  - Database: `cgv` | Username: `postgres` | Password: `postgrespassword`
- **Môi trường AWS RDS (PostgreSQL Multi-AZ)**:
  - Endpoint: `cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com`
  - Port: `5432` | Database: `cgv`
  - Username: `postgres` | Password: `CGVPassword2026`

### 4.3. Redis Caching & Distributed Locks
- **Local**: `localhost:6379` (Password rỗng hoặc `CGVRedis2026`)
- **AWS Prod**: `redis-service.cgv-prod.svc.cluster.local:6379` | Password: `CGVRedis2026`

### 4.4. Cổng thanh toán VNPay Sandbox
- **Mã định danh (TMN Code)**: `3DGV00B5`
- **Khóa bảo mật (Hash Secret)**: `FFTPIPSBYRKNOYYYLZKHAKOWUBSZGFBC`
- **Thông tin thẻ test VNPay Sandbox**:
  - Ngân hàng: `NCB`
  - Số thẻ: `9704198526191432198`
  - Tên chủ thẻ: `NGUYEN VAN A`
  - Ngày phát hành: `07/15`
  - Mã OTP: `123456`

---

## 5. HƯỚNG DẪN TEST ĐĂNG NHẬP QUA API & FRONTEND

### 5.1. Đăng nhập trên giao diện Web (UI)
- **Dành cho Ban Quản trị / Nhân viên**: Truy cập [CGV Admin Portal](https://cgvfeadmin.vercel.app), nhập email của vai trò tương ứng và mật khẩu `12345678`.
- **Dành cho Khán giả / Khách hàng**: Truy cập [CGV Cinema Web](https://cgvfeuser.vercel.app), đăng nhập bằng email `lhhuy.2005@gmail.com` hoặc dùng Social Login (Google / Facebook).

### 5.2. Lấy JWT Token qua cURL (Production Gateway)
```bash
# Đăng nhập Super Admin
curl -X POST "https://d2z63drupmmdh8.cloudfront.net/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin.headquarters.enterprise@cgv.vn",
    "password": "12345678"
  }'

# Đăng nhập Ticket Staff
curl -X POST "https://d2z63drupmmdh8.cloudfront.net/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ticketstaff.boxoffice.crescentmall@cgv.vn",
    "password": "12345678"
  }'
```

### 5.3. Lấy JWT Token trên Local Development (Docker Gateway)
```bash
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin.headquarters.enterprise@cgv.vn",
    "password": "12345678"
  }'
```
