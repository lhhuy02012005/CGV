#!/usr/bin/env python3
# Mô tả công cụ; __doc__ được dùng làm phần giới thiệu khi gọi --help.
"""Validate, register and inspect Outbox connectors without logging credentials."""
# Thư viện chuẩn đọc tham số --local, --check, --status từ command line.
import argparse
# Đọc template JSON và mã hóa/giải mã nội dung REST.
import json
# Đọc biến môi trường của tiến trình Python, không tự đọc file .env.
import os
# Xử lý đường dẫn file độc lập với thư mục terminal đang đứng.
from pathlib import Path
# Regex chỉ thay placeholder có tên bắt đầu CDC_.
import re
# Đồng hồ monotonic đo hạn chờ và sleep giữa các lần kiểm tra.
import time
# Các lớp lỗi REST để xử lý mà không in response có thể chứa mật khẩu.
import urllib.error
# HTTP client chuẩn Python dùng để giao tiếp REST Kafka Connect.
import urllib.request
import http.client

# Từ file script đi lên hai cấp để lấy BE root, nơi chứa thư mục debezium.
ROOT = Path(__file__).resolve().parent.parent

# Hàm dùng chung gửi REST: base là URL Connect, path là endpoint, data là config nếu có.
def request(base, method, path, data=None):
    # GET không có body; PUT config/validate dùng bytes UTF-8 của JSON.
    body = None if data is None else json.dumps(data).encode()
    # Tạo request HTTP với URL và method; POST/PUT/GET không được dùng lẫn tùy endpoint.
    req = urllib.request.Request(base + path, body, method=method,
                                 # Khai báo body là application/json để Connect đọc được cấu hình.
                                 headers={"Content-Type": "application/json"})
    # Bắt lỗi HTTP từ API Connect ở khối bên dưới.
    try:
        # Giới hạn một lần chờ HTTP 30 giây; with đóng response sau khi đọc.
        with urllib.request.urlopen(req, timeout=30) as response:
            # Đọc JSON trả về để dùng error_count hoặc trạng thái connector/task.
            return json.load(response)
    # Xử lý HTTP status lỗi, ví dụ 400 do cấu hình hoặc 404 chưa đăng ký connector.
    except urllib.error.HTTPError as error:
        # Connect responses can contain database.password. Never print the body.
        # Báo mã lỗi và endpoint, không đưa response body hoặc mật khẩu vào thông báo lỗi.
        raise RuntimeError(f"Connect HTTP {error.code} for {method} {path}") from None
    except (urllib.error.URLError, TimeoutError, ConnectionError, http.client.HTTPException) as error:
        # Worker restart/transport failures should report the endpoint without dumping config or secrets.
        raise RuntimeError(f"Connect transport error ({type(error).__name__}) for {method} {path}; check worker status") from None

# Chuẩn bị config từ template; thao tác này chưa đăng ký connector.
def render(path, env):
    # Hàm thay một placeholder mà regex tìm thấy.
    def replace(match):
        # Lấy tên biến, ví dụ CDC_DB_HOST; bỏ ký hiệu ${...}.
        key = match.group(1)
        # Local tắt TLS nên không cần đường dẫn CA. Nhánh này chỉ dành trường CA.
        if key == "CDC_DB_SSLROOTCERT" and env.get("CDC_DB_SSLMODE") == "disable":
            # Thay CA bằng chuỗi rỗng cho kết nối local disable.
            return ""
        # Từ chối chạy nếu biến bắt buộc không có hoặc rỗng; không âm thầm dùng secret production giả.
        if not env.get(key):
            # Thông báo tên biến cần điền, không thông báo giá trị bí mật.
            raise ValueError(f"Missing environment variable: {key}")
        # Dùng giá trị biến môi trường thay placeholder tương ứng.
        return env[key]
    # Đọc file .json hợp lệ; không đọc bản giải thích .annotated.jsonc.
    config = json.loads(path.read_text())
    # Chỉ thay ${CDC_*}; giữ nguyên ${routedByValue} để Debezium tự dùng khi xử lý event.
    return {key: re.sub(r"\$\{(CDC_[A-Z_]+)\}", replace, value)
            # Thực hiện thay biến cho từng thuộc tính trong config; giá trị template hiện đều là chuỗi.
            for key, value in config.items()}

# Điểm xử lý chính của công cụ đăng ký connector.
def main():
    # Khởi tạo parser và dùng mô tả ở docstring cho --help.
    parser = argparse.ArgumentParser(description=__doc__)
    # --local cung cấp mặc định của Compose dev; biến môi trường vẫn có thể ghi đè.
    parser.add_argument("--local", action="store_true", help="Use Compose dev database credentials")
    # --check chỉ parse JSON và thay biến, không gọi REST và không xác nhận DB/Kafka chạy được.
    parser.add_argument("--check", action="store_true", help="Resolve config offline; no REST request")
    # --status chỉ GET trạng thái; không đăng ký hoặc cập nhật cấu hình.
    parser.add_argument("--status", action="store_true", help="Read only connector/task status")
    # Đọc các cờ từ lệnh người dùng đã nhập.
    args = parser.parse_args()
    # Sao chép môi trường để thêm mặc định mà không sửa os.environ.
    env = dict(os.environ)
    # URL REST Connect nhìn từ máy chạy script; mặc định localhost:8088, bỏ dấu / cuối.
    base = env.get("CONNECT_URL", "http://127.0.0.1:8088").rstrip("/")
    # Chọn nhánh Compose dev khi có --local.
    if args.local:
        # Hostname postgres là DNS container mà worker truy cập; không phải hostname Python phải kết nối DB trực tiếp.
        defaults = {"CDC_DB_HOST": "postgres", "CDC_DB_PORT": "5432",
                    # User/mật khẩu demo local khớp prepare-local-role.sql; không dùng mật khẩu này cho production.
                    "CDC_DB_USER": "cdc_user", "CDC_DB_PASSWORD": "cdc-local-only",
                    # Database cgv và TLS disable chỉ là mặc định local.
                    "CDC_DB_NAME": "cgv", "CDC_DB_SSLMODE": "disable"}
        # Ghép dict: các biến môi trường bên phải được ưu tiên hơn mặc định local bên trái.
        env = defaults | env
    # Nhánh không có --local: thông số production cần được cung cấp qua môi trường.
    else:
        # Nếu chưa có cổng DB thì dùng 5432.
        env.setdefault("CDC_DB_PORT", "5432")
        # Nếu chưa có tên database thì dùng cgv.
        env.setdefault("CDC_DB_NAME", "cgv")
        # Nếu chưa có chế độ TLS thì dùng verify-full; cần CA trong container Connect.
        env.setdefault("CDC_DB_SSLMODE", "verify-full")

    # Xử lý lần lượt hai connector độc lập cho Booking và Payment.
    for service in ("booking", "payment"):
        # Tên connector cố định được Kafka Connect lưu; dùng lại tên khi cập nhật cấu hình.
        name = f"cinematrix-{service}-outbox"
        # Endpoint REST ứng với connector hiện tại.
        path = f"/connectors/{name}"
        # Nếu --status thì chỉ kiểm tra trạng thái, không cần render mật khẩu DB.
        if args.status:
            # Lấy trạng thái connector và task qua REST.
            status = request(base, "GET", path + "/status")
        # Nếu không phải --status thì chuẩn bị config, rồi kiểm tra offline hoặc đăng ký.
        else:
            # Đọc đúng template của service và thay biến môi trường.
            config = render(ROOT / "debezium" / f"{service}-outbox.json", env)
            # Plugin validation requires the connector name, also present in the REST path.
            config["name"] = name
            # Nhánh --check kết thúc ở kiểm tra offline.
            if args.check:
                # Chỉ in tên connector, bảng và slot; không in config đầy đủ vì config có mật khẩu.
                print(f"{name}: config resolved; table={config['table.include.list']}; slot={config['slot.name']}")
                # Sang connector còn lại; bỏ qua REST validate, PUT config và GET status.
                continue
            # Gọi API validate plugin PostgreSQL; đây là yêu cầu thật tới worker khi đăng ký.
            result = request(base, "PUT", "/connector-plugins/io.debezium.connector.postgresql.PostgresConnector/config/validate", config)
            # Nếu Connect báo lỗi cấu hình thì dừng trước khi PUT config.
            if result.get("error_count", 0):
                # Thu thập tên các trường cấu hình có lỗi.
                fields = [row["definition"]["name"] for row in result.get("configs", [])
                          # Chỉ chọn trường có danh sách errors khác rỗng.
                          if row.get("value", {}).get("errors")]
                # Báo tên trường cần sửa, không in response validate đầy đủ có thể chứa secret.
                raise RuntimeError(f"{name}: invalid configuration fields: {', '.join(fields)}")
            # PUT cùng tên sẽ tạo hoặc cập nhật connector; tên mới khác sẽ tạo connector khác.
            request(base, "PUT", path + "/config", config)
            # Đặt hạn kiểm tra trạng thái 90 giây bằng đồng hồ monotonic; không phải số đo độ trễ CDC.
            deadline = time.monotonic() + 90
            # Lặp GET status để đợi worker phân công task và khởi động connector.
            while True:
                # Lấy trạng thái hiện tại sau đăng ký.
                try:
                    status = request(base, "GET", path + "/status")
                except RuntimeError as error:
                    # The distributed worker may not expose status immediately after PUT.
                    if str(error) == f"Connect HTTP 404 for GET {path}/status" and time.monotonic() < deadline:
                        time.sleep(2)
                        continue
                    raise
                # Lấy trạng thái của từng task, vì connector RUNNING chưa chắc task RUNNING.
                states = [task["state"] for task in status.get("tasks", [])]
                # Chỉ chấp nhận khi connector RUNNING, đã có task và mọi task đều RUNNING.
                if status["connector"]["state"] == "RUNNING" and states and all(s == "RUNNING" for s in states):
                    # Dừng vòng đợi khi điều kiện trạng thái đạt.
                    break
                # Dừng nếu task/connector FAILED hoặc vượt hạn kiểm tra; không thử đăng ký tên mới để che lỗi.
                if "FAILED" in states or status["connector"]["state"] == "FAILED" or time.monotonic() > deadline:
                    # Yêu cầu xem log Connect để tìm lỗi DB/publication/slot; khi chia sẻ log phải che credential.
                    raise RuntimeError(f"{name}: not RUNNING; inspect Connect logs (redact credentials)")
                # Đợi 2 giây trước lần GET tiếp theo; không phải vòng polling bảng Outbox.
                time.sleep(2)
        # A connector can be RUNNING while its task has failed. Show both.
        # Chuẩn bị output trạng thái chỉ gồm tên và state, không chứa cấu hình DB.
        print(json.dumps({"name": name, "connector": status["connector"]["state"],
                          # In id/state của task và căn lề JSON cho dễ xem/chụp; dữ liệu lấy từ API, không tự tạo trạng thái giả.
                          "tasks": [{"id": t["id"], "state": t["state"]} for t in status.get("tasks", [])]}, indent=2))
        # Kiểm tra lại trạng thái cho cả nhánh --status: thiếu task hoặc task lỗi là thất bại.
        if status["connector"]["state"] != "RUNNING" or not status.get("tasks") or any(t["state"] != "RUNNING" for t in status["tasks"]):
            # Thoát với lỗi nếu trạng thái chưa đạt; không coi chỉ có connector RUNNING là thành công.
            raise RuntimeError(f"{name}: connector/task is not RUNNING")

# Chạy main chỉ khi gọi file trực tiếp; import phục vụ kiểm tra không tự đăng ký.
if __name__ == "__main__":
    # Bắt lỗi dự kiến để kết thúc command line gọn.
    try:
        # Chạy quy trình đã chọn bằng các cờ.
        main()
    # Bắt lỗi config, trạng thái hoặc lỗi kết nối REST; không nuốt lỗi rồi báo thành công.
    except (RuntimeError, ValueError, urllib.error.URLError) as error:
        # Kết thúc với thông báo lỗi; không sửa Compose hoặc DB để tự vượt lỗi kết nối.
        raise SystemExit(str(error))
