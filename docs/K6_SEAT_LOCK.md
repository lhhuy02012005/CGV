# K6: 1.000 request tranh cùng một ghế

Hướng dẫn đầy đủ về phần mềm, tài nguyên, cổng và khởi động nằm trong README.md ở thư mục gốc BE. Từ thư mục đó:

```bash
python3 scripts/demo.py doctor
python3 scripts/demo.py init
python3 scripts/demo.py start
python3 scripts/demo.py prepare
python3 scripts/demo.py smoke
python3 scripts/demo.py load
python3 scripts/demo.py results
```

`prepare` chọn ghế hợp lệ. `smoke`/`integrity` cần `SMOKE_TOKENS_FILE` là JSON array gồm hai JWT tài khoản; `load` cần `TOKENS_FILE` là JSON array gồm 1.000 JWT từ 1.000 tài khoản khác nhau. Cấp các token hợp lệ từ Keycloak và đặt biến môi trường trước khi chạy; không lưu token trong Git. Kết quả cũ ngày 22/10/2026 không đại diện phiên bản JWT.

Kịch bản k6 có 1.000 VU, mỗi VU POST cùng ghế với JWT khác chủ. Tiêu chí là 1/999 và đối chiếu chủ Redis bằng `sub` token thắng; chưa chạy lại k6 bản này. Không suy ra kết quả mới từ phép đo dùng X-User-Id cũ.

Output nằm trong target/demo-runs/RUN_ID, không ghi đè lần trước. `result.json` lấy số đo từ k6; `verification.json` kiểm tra chúng cùng `redis-after.json`. Không đổi tiêu chí hoặc chỉnh output để một lần thất bại thành đạt. RUN_ID trước/sau sửa DTO được giữ như hai lần độc lập.

Chụp console k6 có người thắng, số request/phân vị và kết quả đối chiếu Redis. Dùng `results` để xem dữ liệu đã lưu. Sau TTL, key live có thể mất; redis-after là trạng thái đã chụp ngay sau phép đo. Ảnh và bảng trong báo cáo phải cùng RUN_ID.

Phạm vi: một BookingService và một Redis, HTTP qua Kong. Đây là giữ ghế tạm, chưa tạo booking/thanh toán; không suy ra năng lực nhiều instance, chịu lỗi Redis hoặc thời gian riêng của Lua. http_req_duration gồm sending + waiting + receiving, không gồm DNS/kết nối ban đầu. Phân vị gộp chủ yếu phản ánh các phản hồi xung đột.
