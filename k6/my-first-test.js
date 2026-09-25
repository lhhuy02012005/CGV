import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. CẤU HÌNH THÔNG SỐ TEST (OPTIONS)
export const options = {
    vus: 10,           // Giả lập 10 người dùng truy cập đồng thời
    duration: '10s',   // Thời gian chạy bài test là 10 giây
    thresholds: {
        http_req_failed: ['rate<0.01'],    // Tỷ lệ request lỗi phải dưới 1%
        http_req_duration: ['p(95)<500'],  // 95% số request phải phản hồi dưới 500ms
    },
};

// 2. HÀM SETUP (Chạy 1 lần duy nhất trước khi test)
export function setup() {
    const loginPayload = JSON.stringify({
        username: 'lehuuhuy211405@gmail.com',
        password: '12345678',
    });

    const loginRes = http.post('http://localhost:8000/api/v1/auth/login', loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    // Kiểm tra xem đăng nhập có thành công không
    check(loginRes, {
        'Đăng nhập thành công (200)': (r) => r.status === 200,
    });

    // Lấy token trả về từ response để truyền cho các VU ở dưới dùng chung
    const json = loginRes.json();
    return { token: json.data.accessToken };
}

// 3. HÀM CHÍNH (Được lặp lại bởi các Virtual Users)
export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`, // Gắn token lấy từ setup()
        },
    };

    // Gửi request GET tới API cần test
    const res = http.get('http://localhost:8000/api/v1/bookings/my-bookings', params);

    // Dùng hàm check để xác thực kết quả trả về
    check(res, {
        'API trả về status 200': (r) => r.status === 200,
        'Thời gian phản hồi hợp lệ': (r) => r.timings.duration < 400,
    });

    sleep(1); // Giả lập user đọc trang trong 1 giây trước khi gửi request tiếp theo
}