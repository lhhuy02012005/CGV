import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ==============================================================================
// CẤU HÌNH BIẾN MÔI TRƯỜNG & METRICS
// ==============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

// Custom Metrics để đo lường chi tiết
const moviesTrend = new Trend('cgv_movies_duration');
const searchTrend = new Trend('cgv_elasticsearch_search_duration');
const cinemasTrend = new Trend('cgv_cinemas_duration');
const showtimesTrend = new Trend('cgv_showtimes_duration');
const seatsTrend = new Trend('cgv_seats_duration');
const errorRate = new Rate('cgv_error_rate');
const successfulRequests = new Counter('cgv_successful_requests');

// ==============================================================================
// THIẾT LẬP KỊCH BẢN NẠP TẢI (STAGES & THRESHOLDS)
// ==============================================================================
export const options = {
  stages: [
    { duration: '15s', target: 50 },   // 1. Khởi động: Tăng dần từ 0 lên 50 VUs trong 15s
    { duration: '30s', target: 200 },  // 2. Nạp tải: Tăng từ 50 lên 200 VUs trong 30s
    { duration: '1m',  target: 500 },  // 3. Đỉnh tải: Đẩy lên 500 VUs và duy trì trong 1 phút
    { duration: '15s', target: 0 },    // 4. Hạ tải: Giảm dần về 0 VU trong 15s
  ],
  thresholds: {
    // 95% requests phải phản hồi dưới 400ms, 99% dưới 800ms
    http_req_duration: ['p(95)<400', 'p(99)<800'],
    // Tỷ lệ lỗi toàn hệ thống không được vượt quá 1%
    cgv_error_rate: ['rate<0.01'],
    http_req_failed: ['rate<0.01'],
  },
};

// ==============================================================================
// GIAI ĐOẠN SETUP: TỰ ĐỘNG TÌM PHIM CÓ LỊCH CHIẾU HÔM NAY VÀ RẠP CHIẾU
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

  // 2. Lấy thông tin tên phim
  let movieTitle = 'CGV Movie';
  try {
    const movieRes = http.get(`${BASE_URL}/api/v1/catalogs/movies/${movieId}`, { headers });
    const movieJson = movieRes.json();
    if (movieJson.data && movieJson.data.title) {
      movieTitle = movieJson.data.title;
    }
  } catch (e) {}

  // 3. Tự động lấy danh sách rạp
  const cinemasRes = http.get(`${BASE_URL}/api/v1/catalogs/cinemas?page=0&size=10`, { headers });
  let cinemaId = null;
  try {
    const json = cinemasRes.json();
    if (json.data && json.data.data && json.data.data.length > 0) {
      cinemaId = json.data.data[0].id;
    }
  } catch (e) {}

  console.log(`[SETUP HOÀN TẤT] Tự động lấy phim có lịch chiếu:`);
  console.log(` - Phim: "${movieTitle}" (ID: ${movieId})`);
  console.log(` - Suất chiếu ID: ${showtimeId} (Thời gian: ${selectedShowtime.startTime})`);
  console.log(` - Phòng chiếu ID: ${roomId}`);
  console.log(` - Rạp chiếu ID: ${cinemaId}`);

  return { movieId, movieTitle, cinemaId, showtimeId, roomId };
}

// ==============================================================================
// HÀM CHÍNH (VIRTUAL USER HÀNH ĐỘNG)
// ==============================================================================
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  // Hành vi 1: Xem trang chủ / danh sách phim đang chiếu
  group('1. Lấy danh sách phim', function () {
    const res = http.get(`${BASE_URL}/api/v1/catalogs/movies?page=0&size=20&sort=releaseDate,desc`, { headers });
    moviesTrend.add(res.timings.duration);
    const passed = check(res, {
      'Phim - HTTP 200': (r) => r.status === 200,
      'Phim - Có dữ liệu': (r) => r.body && r.body.length > 0,
    });
    if (passed) successfulRequests.add(1);
    else errorRate.add(1);
  });

  sleep(0.5);

  // Hành vi 2: Tìm kiếm phim qua Elasticsearch (Full-text / Fuzzy Search)
  group('2. Tìm kiếm phim qua Elasticsearch', function () {
    const keywords = ['Avatar', 'Dune', 'Kung Fu', 'Hành tinh', 'Chiến binh'];
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];
    const res = http.get(`${BASE_URL}/api/v1/catalogs/movies/search?keyword=${encodeURIComponent(keyword)}`, { headers });
    searchTrend.add(res.timings.duration);
    const passed = check(res, {
      'Elasticsearch - HTTP 200': (r) => r.status === 200,
      'Elasticsearch - Phản hồi < 400ms': (r) => r.timings.duration < 400,
    });
    if (passed) successfulRequests.add(1);
    else errorRate.add(1);
  });

  sleep(0.5); // Khách xem kết quả tìm kiếm trong 500ms

  // Hành vi 3: Tra cứu danh sách rạp chiếu
  group('3. Lấy danh sách cụm rạp', function () {
    const res = http.get(`${BASE_URL}/api/v1/catalogs/cinemas?page=0&size=50`, { headers });
    cinemasTrend.add(res.timings.duration);
    const passed = check(res, {
      'Rạp - HTTP 200': (r) => r.status === 200,
    });
    if (passed) successfulRequests.add(1);
    else errorRate.add(1);
  });

  sleep(0.5);

  // Hành vi 3: Tra cứu lịch chiếu của phim
  group('3. Lấy lịch chiếu của phim', function () {
    const res = http.get(`${BASE_URL}/api/v1/catalogs/showtimes?movieId=${data.movieId}`, { headers });
    showtimesTrend.add(res.timings.duration);
    const passed = check(res, {
      'Suất chiếu - HTTP 200': (r) => r.status === 200,
    });
    if (passed) successfulRequests.add(1);
    else errorRate.add(1);
  });

  sleep(0.5);

  // Hành vi 4: Mở phòng chiếu xem sơ đồ ghế
  group('4. Lấy sơ đồ ghế phòng chiếu', function () {
    const res = http.get(`${BASE_URL}/api/v1/catalogs/seats/room/${data.roomId}?page=0&size=150`, { headers });
    seatsTrend.add(res.timings.duration);
    const passed = check(res, {
      'Ghế - HTTP 200': (r) => r.status === 200,
    });
    if (passed) successfulRequests.add(1);
    else errorRate.add(1);
  });

  sleep(1); // Thời gian suy nghĩ của người dùng trước khi lặp lại
}

// ==============================================================================
// XUẤT BÁO CÁO HTML SAU KHI TEST HOÀN TẤT
// ==============================================================================
export function handleSummary(data) {
  return {
    'k6-load-test-report.html': htmlReport(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  return '\n====== BÁO CÁO KIỂM THỬ TẢI K6 ĐÃ HOÀN TẤT ======\n'
    + `Tổng số requests: ${data.metrics.http_reqs.values.count}\n`
    + `Tỷ lệ thành công: ${(100 - (data.metrics.http_req_failed ? data.metrics.http_req_failed.values.rate * 100 : 0)).toFixed(2)}%\n`
    + `Thời gian phản hồi TB: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`
    + `95th Percentile: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`
    + `Báo cáo HTML trực quan đã được lưu vào file: k6-load-test-report.html\n`
    + '===================================================\n';
}
