import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ==============================================================================
// CẤU HÌNH KIỂM THỬ TRANH CHẤP ĐỒNG THỜI (RACE CONDITION / ANTI-DOUBLE BOOKING)
// ==============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

// Số lượng người dùng đồng thời cùng tranh chấp 1 ghế
const CONCURRENT_USERS = parseInt(__ENV.VUS || '100', 10);

// ID Suất chiếu & Ghế kiểm thử (Suất chiếu hôm nay, ghế VIP D5)
const SHOWTIME_ID = __ENV.SHOWTIME_ID || '21000000-0000-0000-0000-000000000001';

// Custom Metrics để kiểm chứng tính toàn vẹn (Data Integrity)
const successfulLocks = new Counter('cgv_seat_lock_success');
const blockedLocks = new Counter('cgv_seat_lock_conflict_blocked');
const unexpectedErrors = new Counter('cgv_unexpected_5xx_errors');

export const options = {
  // Mô hình chạy: Toàn bộ VUs kích hoạt và gửi request tranh chấp cùng 1 thời điểm
  scenarios: {
    seat_rush_concurrency: {
      executor: 'per-vu-iterations',
      vus: CONCURRENT_USERS,
      iterations: 1,
      maxDuration: '10s',
    },
  },
  thresholds: {
    // ĐIỀU KIỆN TIÊN QUYẾT CỦA BÀI TEST:
    // 1. Chỉ DUY NHẤT 1 người được giữ ghế thành công (count == 1)
    cgv_seat_lock_success: ['count==1'],
    // 2. Không được có bất kỳ lỗi 500 nào xảy ra trong lúc tranh chấp cao điểm
    cgv_unexpected_5xx_errors: ['count==0'],
  },
};

// ==============================================================================
// GIAI ĐOẠN SETUP: TỰ ĐỘNG TÌM PHIM CÓ LỊCH CHIẾU HÔM NAY VÀ GHẾ TRỐNG KHẢ DỤNG
// ==============================================================================
export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  
  // 1. Tự động lấy danh sách suất chiếu
  const showtimesRes = http.get(`${BASE_URL}/api/v1/catalogs/showtimes?page=0&size=100`, { headers });
  let showtimes = [];
  try {
    const json = showtimesRes.json();
    if (json.data && json.data.data) {
      showtimes = json.data.data;
    }
  } catch (e) {}

  if (showtimes.length === 0) {
    throw new Error('[SETUP LỖI] Không tìm thấy suất chiếu nào trong Catalog Service!');
  }

  // Ưu tiên tìm suất chiếu HÔM NAY
  const todayStr = new Date().toISOString().slice(0, 10);
  let selectedShowtime = showtimes.find(s => s.startTime && s.startTime.startsWith(todayStr));
  
  // Nếu hôm nay không có, lấy suất chiếu mới nhất
  if (!selectedShowtime) {
    selectedShowtime = showtimes.slice().sort((a, b) => new Date(b.startTime) - new Date(a.startTime))[0];
  }

  const showtimeId = selectedShowtime.id;
  const movieId = selectedShowtime.movieId;
  const roomId = selectedShowtime.roomResponse ? selectedShowtime.roomResponse.id : null;

  if (!showtimeId || !roomId) {
    throw new Error('[SETUP LỖI] Suất chiếu không hợp lệ hoặc thiếu phòng chiếu!');
  }

  // 2. Lấy thông tin tên phim
  let movieTitle = 'CGV Movie';
  try {
    const movieRes = http.get(`${BASE_URL}/api/v1/catalogs/movies/${movieId}`, { headers });
    const movieJson = movieRes.json();
    if (movieJson.data && movieJson.data.title) {
      movieTitle = movieJson.data.title;
    }
  } catch (e) {}

  // 3. Lấy danh sách ghế đang bị giữ hoặc đã được đặt để tránh chọn trùng
  let lockedSeats = [];
  let bookedSeats = [];
  try {
    const lockedRes = http.get(`${BASE_URL}/api/v1/bookings/seat-locks/${showtimeId}`, { headers });
    const lockedJson = lockedRes.json();
    if (lockedJson.data && Array.isArray(lockedJson.data)) {
      lockedSeats = lockedJson.data;
    }
  } catch (e) {}

  try {
    const bookedRes = http.get(`${BASE_URL}/api/v1/bookings/seat-locks/booked/${showtimeId}`, { headers });
    const bookedJson = bookedRes.json();
    if (bookedJson.data && Array.isArray(bookedJson.data)) {
      bookedSeats = bookedJson.data;
    }
  } catch (e) {}

  // 4. Tự động lấy danh sách ghế trong phòng và chọn ghế còn TRỐNG HOÀN TOÀN
  let targetSeatId = null;
  let targetSeatName = '';
  const seatsRes = http.get(`${BASE_URL}/api/v1/catalogs/seats/room/${roomId}?page=0&size=100`, { headers });
  try {
    const json = seatsRes.json();
    if (json.data && json.data.data && json.data.data.length > 0) {
      const availableSeats = json.data.data.filter(s => 
        s.isActive && !lockedSeats.includes(s.id) && !bookedSeats.includes(s.id)
      );
      const seat = availableSeats.length > 0 ? availableSeats[0] : json.data.data[0];
      targetSeatId = seat.id;
      targetSeatName = `${seat.rowChar || ''}${seat.seatNumber || ''} (${seat.seatTypeName || 'STANDARD'})`;
    }
  } catch (e) {}

  if (!targetSeatId) {
    throw new Error(`[SETUP LỖI] Không tìm thấy ghế trống trong phòng chiếu ${roomId}!`);
  }

  console.log(`[SETUP HOÀN TẤT] Tìm thấy phim có lịch chiếu:`);
  console.log(` - Phim: "${movieTitle}" (ID: ${movieId})`);
  console.log(` - Suất chiếu ID: ${showtimeId} (Thời gian: ${selectedShowtime.startTime})`);
  console.log(` - Ghế mục tiêu: ${targetSeatName} (ID: ${targetSeatId})`);

  return { seatId: targetSeatId, showtimeId: showtimeId, seatName: targetSeatName, movieTitle: movieTitle };
}

// ==============================================================================
// HÀM CHÍNH: HÀNG TRĂM NGƯỜI CÙNG GỬI REQUEST GIỮ ĐÚNG 1 GHẾ NÀY
// ==============================================================================
export default function (data) {
  // Mỗi Virtual User (VU) đại diện cho 1 khách hàng riêng biệt với User ID khác nhau
  const userId = `k6-user-test-${__VU}-${Date.now()}`;

  const payload = JSON.stringify({
    showtimeId: data.showtimeId,
    seatIds: [data.seatId],
  });

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-User-Id': userId, // Giả lập User ID đã xác thực
  };

  // Gửi request giữ ghế
  const res = http.post(`${BASE_URL}/api/v1/bookings/seat-locks`, payload, { headers });

  if (res.status === 200 || res.status === 201) {
    // Người duy nhất giữ ghế thành công
    successfulLocks.add(1);
    console.log(`>>> [CHIẾN THẮNG] VU #${__VU} (User: ${userId}) giữ ghế thành công!`);
  } else if (res.status === 400 || res.status === 409 || res.status === 422) {
    // Bị hệ thống khóa phân tán (Distributed Lock / Redis / DB) chặn lại
    blockedLocks.add(1);
  } else if (res.status >= 500) {
    // Lỗi máy chủ không mong muốn
    unexpectedErrors.add(1);
    console.error(`[LỖI 5XX] VU #${__VU} gặp lỗi máy chủ: HTTP ${res.status} - ${res.body}`);
  }

  check(res, {
    'Phản hồi hợp lệ (200, 400 hoặc 409)': (r) => [200, 201, 400, 409, 422].includes(r.status),
    'Không bị sập 500': (r) => r.status < 500,
  });
}

// ==============================================================================
// XUẤT BÁO CÁO HTML SAU KHI TEST HOÀN TẤT
// ==============================================================================
export function handleSummary(data) {
  return {
    'k6-race-condition-report.html': htmlReport(data),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const successCount = data.metrics.cgv_seat_lock_success ? data.metrics.cgv_seat_lock_success.values.count : 0;
  const blockedCount = data.metrics.cgv_seat_lock_conflict_blocked ? data.metrics.cgv_seat_lock_conflict_blocked.values.count : 0;
  const errors5xx = data.metrics.cgv_unexpected_5xx_errors ? data.metrics.cgv_unexpected_5xx_errors.values.count : 0;
  const passedAntiDoubleBooking = (successCount === 1 && errors5xx === 0);

  return '\n============================================================\n'
    + '       KẾT QUẢ KIỂM THỬ TRANH CHẤP ĐỒNG THỜI (CONCURRENCY)\n'
    + '============================================================\n'
    + `Số người dùng tranh chấp (VUs):  ${CONCURRENT_USERS}\n`
    + `Số lượt giữ ghế THÀNH CÔNG:      ${successCount}  (Kỳ vọng: Đúng 1)\n`
    + `Số lượt BỊ CHẶN AN TOÀN:         ${blockedCount}  (Kỳ vọng: ${CONCURRENT_USERS - 1})\n`
    + `Số lỗi 5xx Server Error:         ${errors5xx}  (Kỳ vọng: 0)\n`
    + '------------------------------------------------------------\n'
    + `KẾT LUẬN: ${passedAntiDoubleBooking ? 'ĐẠT CHUẨN! Hệ thống chống Double-Booking tuyệt đối an toàn.' : 'THẤT BẠI! Cần kiểm tra lại Redisson Lock hoặc Transaction Isolation.'}\n`
    + 'Báo cáo chi tiết: k6-race-condition-report.html\n'
    + '============================================================\n';
}
