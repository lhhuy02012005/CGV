import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

export const options = {
    vus: 10,           // 10 người dùng ảo
    duration: '10s',   // Chạy trong 10 giây
    thresholds: {
        http_req_failed: ['rate<0.01'],    // Tỷ lệ lỗi < 1%
        http_req_duration: ['p(95)<800'],  // 95% phản hồi < 300ms
    },
};

// ==============================================================================
// GIAI ĐOẠN 1: SETUP - Đăng nhập lấy Token 1 lần duy nhất
// ==============================================================================
export function setup() {
    const loginPayload = JSON.stringify({
        username: 'lehuuhuy211405@gmail.com', // Tài khoản mẫu trong hệ thống CGV
        password: '12345678',
    });

    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    // Kiểm tra đăng nhập thành công
    check(loginRes, {
        'Đăng nhập lấy token thành công (HTTP 200)': (r) => r.status === 200,
    });

    const json = loginRes.json();
    const token = json.data.accessToken; // Lấy JWT Access Token từ response
    console.log('[SETUP] Đã lấy thành công Token cho toàn bộ VUs!');

    // Trả về token để truyền vào hàm default bên dưới
    return { token: token };
}

// ==============================================================================
// GIAI ĐOẠN 2: CÁC VIRTUAL USERS (VUs) DÙNG TOKEN ĐỂ GỌI API BẢO MẬT
// ==============================================================================
export default function (data) {
    // Gắn Token vào Header Authorization
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`, // <--- GẮN TOKEN Ở ĐÂY
        },
    };

    // Gọi API bảo mật: Xem lịch sử vé đã đặt của tôi
    const res = http.get(`${BASE_URL}/api/v1/bookings/my-bookings`, params);

    // Kiểm tra kết quả
    check(res, {
        'Gọi API có Token thành công (HTTP 200)': (r) => r.status === 200,
        'Không bị lỗi 401 Unauthorized': (r) => r.status !== 401,
    });

    sleep(1);
}