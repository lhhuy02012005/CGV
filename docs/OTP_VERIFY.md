# Lỗi xác thực OTP khi đăng ký trên Spring Boot 4

## Nguyên nhân kiểm chứng từ log

POST /api/v1/auth/register/verify trả 500 sau khi verifyAndRegister đã kiểm tra OTP đúng. registerOnKeycloakAndLocal gọi Keycloak lấy service-account token; SpringDecoder dùng Jackson 3 nhưng KeycloakClient.exchangeToken khai báo com.fasterxml.jackson.databind.JsonNode của Jackson 2. Log ghi DecodeException / InvalidDefinitionException tại bước đọc token, trước khi tạo user Keycloak.

## Thay đổi

- repository/httpclient/KeycloakClient.java: dùng tools.jackson.databind.JsonNode.
- service/impl/AuthenticationServiceImpl.java và RoleServiceImpl.java: đồng bộ cùng kiểu Jackson 3.
- config/FeignConfig.java: đổi logger FULL sang BASIC để log Feign không ghi body chứa token, mật khẩu hoặc OTP. Encoder form hiện có vẫn dùng ObjectMapper Jackson 2; việc encode request và decode response là hai bước riêng. Không thay cách kiểm tra OTP hoặc TTL 5 phút.

Các đường dẫn Java trên thuộc identityservice/src/main/java/com/cgv/identityservice/.

## Kiểm chứng

Ba lớp FeignConfigTest (4 ca), KeycloakTokenDecodingTest (2 ca), RegistrationOtpTest (3 ca) đã chạy: tổng 9 ca, không failure/error/skipped. Test decoder dùng SpringDecoder thật và return type lấy trực tiếp từ KeycloakClient; kiểm tra JSON token và header Location của response tạo user. Test nghiệp vụ kiểm tra OTP đúng, OTP sai và giữ OTP khi Keycloak lỗi.

```bash
mvn -pl identityservice -am \
  -Dtest=FeignConfigTest,KeycloakTokenDecodingTest,RegistrationOtpTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Đã build JAR và tạo lại riêng container identity-service. Kiểm tra runtime qua Kong bằng tài khoản tên ngẫu nhiên ở example.invalid và OTP test được nạp vào Redis:

- OTP sai: HTTP 400, OTP test còn trong Redis.
- OTP đúng: HTTP 200; tạo user thật trên Keycloak và PostgreSQL local; OTP bị xóa sau thành công.
- Login, refresh token, logout: HTTP 200.
- Dọn riêng tài khoản, OTP và tier cache thuộc phép thử.

Phép thử runtime không gửi email và không dùng OTP/tài khoản của người dùng. Nó kiểm chứng bước verify, tạo tài khoản và đăng nhập, chưa kiểm thử lại dịch vụ phát email.

Log thật: target/otp-tests.log, target/otp-runtime-verification.json và target/otp-runtime-verification.log.

## Thử lại trên FE

Tải lại trang và thực hiện xác nhận đăng ký. Nếu mã cũ đã quá 5 phút, bấm Gửi lại, nhập mã mới. Các lần lỗi decode trước đó dừng trước bước tạo tài khoản và trước khi xóa OTP.

Nguồn cơ chế Jackson mặc định: https://docs.spring.io/spring-boot/4.0/reference/features/json.html
