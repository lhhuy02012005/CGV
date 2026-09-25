import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ==============================================================================
// BÀI TEST TRANH CHẤP VOUCHER CÒN 1 SLOT DUY NHẤT (VOUCHER RACE CONDITION)
// Kịch bản: Hàng chục/hàng trăm người dùng đồng thời đặt vé áp dụng CÙNG 1 VOUCHER còn 1 slot.
// Quy trình chuẩn CGV:
//   1. Mỗi user giữ 1 ghế riêng biệt của mình (Seat Locking).
//   2. Đồng loạt gửi request tạo Booking kèm mã voucher FLASH1SLOT (chỉ còn 1 slot duy nhất).
// Kỳ vọng:
//  - Đúng 1 người duy nhất áp dụng thành công và tạo đơn vé được giảm giá.
//  - Toàn bộ những người còn lại bị chặn tức thì bởi Redis Lua Script (400 Bad Request).
//  - Tuyệt đối KHÔNG có lỗi máy chủ 500.
//  - Tuyệt đối KHÔNG xảy ra Overselling (Bán lố / Giảm lố voucher).
// ==============================================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';
const CONCURRENT_USERS = parseInt(__ENV.VUS || '30', 10);
const PROMOTION_ID = '31000000-0000-0000-0000-000000000099'; // Voucher FLASH1SLOT (1 slot)

// Custom Metrics
const successfulVoucherBookings = new Counter('cgv_voucher_booking_success');
const blockedVoucherBookings = new Counter('cgv_voucher_booking_blocked');
const unexpectedErrors = new Counter('cgv_unexpected_5xx_errors');

export const options = {
  scenarios: {
    voucher_rush_concurrency: {
      executor: 'per-vu-iterations',
      vus: CONCURRENT_USERS,
      iterations: 1,
      maxDuration: '20s',
    },
  },
  thresholds: {
    // 1. DUY NHẤT 1 người dùng được áp dụng voucher thành công
    cgv_voucher_booking_success: ['count==1'],
    // 2. Không có bất kỳ lỗi 500 nào
    cgv_unexpected_5xx_errors: ['count==0'],
  },
};

export function setup() {
  const headers = { 'Content-Type': 'application/json' };

  // 1. Lấy danh sách suất chiếu
  const showtimesRes = http.get(`${BASE_URL}/api/v1/catalogs/showtimes?page=0&size=50`, { headers });
  let showtimes = [];
  try {
    const json = showtimesRes.json();
    if (json.data && json.data.data) {
      showtimes = json.data.data;
    }
  } catch (e) {}

  if (showtimes.length === 0) {
    throw new Error('[SETUP LỖI] Không tìm thấy suất chiếu nào!');
  }

  // Chọn suất chiếu có phòng
  const selectedShowtime = showtimes.find(s => s.roomResponse && s.roomResponse.id) || showtimes[0];
  const showtimeId = selectedShowtime.id;
  const roomId = selectedShowtime.roomResponse.id;

  // 2. Lấy danh sách ghế trong phòng chiếu
  const seatsRes = http.get(`${BASE_URL}/api/v1/catalogs/seats/room/${roomId}?page=0&size=150`, { headers });
  let seatList = [];
  try {
    const json = seatsRes.json();
    if (json.data && json.data.data) {
      seatList = json.data.data.filter(s => s.isActive).map(s => s.id);
    }
  } catch (e) {}

  if (seatList.length < CONCURRENT_USERS) {
    throw new Error(`[SETUP LỖI] Phòng chiếu chỉ có ${seatList.length} ghế, không đủ cho ${CONCURRENT_USERS} VUs!`);
  }

  console.log(`[SETUP HOÀN TẤT] Bắt đầu bài test tranh chấp Voucher FLASH1SLOT:`);
  console.log(` - Suất chiếu ID: ${showtimeId}`);
  console.log(` - Voucher ID: ${PROMOTION_ID}`);
  console.log(` - Số ghế khả dụng trong phòng: ${seatList.length}`);
  console.log(` - Số người dùng tham gia tranh chấp: ${CONCURRENT_USERS} VUs`);

  return {
    showtimeId: showtimeId,
    seatList: seatList,
  };
}

export default function (data) {
  const vuIndex = __VU - 1;
  const userId = `k6-voucher-user-${__VU}-${Date.now()}`;
  const seatId = data.seatList[vuIndex];

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-User-Id': userId,
  };

  // Bước 1: Giữ ghế riêng biệt của user
  const lockPayload = JSON.stringify({
    showtimeId: data.showtimeId,
    seatIds: [seatId],
  });
  const lockRes = http.post(`${BASE_URL}/api/v1/bookings/seat-locks`, lockPayload, { headers });

  if (lockRes.status !== 200 && lockRes.status !== 201) {
    console.error(`[LOCK LỖI] VU #${__VU} không giữ được ghế: ${lockRes.status} - ${lockRes.body}`);
    return;
  }

  // Bước 2: Đồng loạt gửi yêu cầu tạo booking áp dụng CÙNG 1 Voucher (chỉ còn 1 slot)
  const bookingPayload = JSON.stringify({
    showtimeId: data.showtimeId,
    seatIds: [seatId],
    promotionId: PROMOTION_ID,
  });

  const res = http.post(`${BASE_URL}/api/v1/bookings`, bookingPayload, { headers });

  if (res.status === 201 || res.status === 200) {
    successfulVoucherBookings.add(1);
    console.log(`>>> [CHIẾN THẮNG VOUCHER] VU #${__VU} (User: ${userId}) áp dụng Voucher 1-SLOT thành công!`);
  } else if (res.status === 400 || res.status === 409 || res.status === 422) {
    blockedVoucherBookings.add(1);
  } else if (res.status >= 500) {
    unexpectedErrors.add(1);
    console.error(`[LỖI 5XX] VU #${__VU} gặp lỗi máy chủ: HTTP ${res.status} - ${res.body}`);
  }

  check(res, {
    'Phản hồi hợp lệ (200, 201 hoặc 400)': (r) => [200, 201, 400, 409, 422].includes(r.status),
    'Không sập hệ thống (status < 500)': (r) => r.status < 500,
  });
}

export function handleSummary(data) {
  return {
    'k6-voucher-race-condition-report.html': htmlReport(data),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const success = data.metrics['cgv_voucher_booking_success']?.values?.count || 0;
  const blocked = data.metrics['cgv_voucher_booking_blocked']?.values?.count || 0;
  const errors = data.metrics['cgv_unexpected_5xx_errors']?.values?.count || 0;

  return `
================================================================================
             KẾT QUẢ KIỂM THỬ TRANH CHẤP VOUCHER CONCURRENCY (K6)
================================================================================
 - Tổng số người tranh chấp: ${CONCURRENT_USERS} VUs
 - Số người áp dụng thành công (Có voucher): ${success} (Kỳ vọng: 1)
 - Số người bị từ chối (Hết lượt voucher):   ${blocked} (Kỳ vọng: ${CONCURRENT_USERS - 1})
 - Lỗi hệ thống 5xx:                         ${errors} (Kỳ vọng: 0)
================================================================================
  KẾT LUẬN: ${success === 1 && errors === 0 ? '>>> THÀNH CÔNG: KHÔNG XẢY RA OVERSELLING VOUCHER! REDIS LUA HOẠT ĐỘNG HOÀN HẢO! <<<' : '>>> THẤT BẠI: CÓ RACE CONDITION HOẶC LỖI HỆ THỐNG! <<<'}
================================================================================
`;
}
