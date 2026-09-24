import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ==============================================================================
// CẤU HÌNH KIỂM THỬ HIỆU NĂNG TÌM KIẾM ELASTICSEARCH (CGV MOVIES)
// ==============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

// Custom Metrics đo lường tốc độ Elasticsearch
const esLatencyTrend = new Trend('elasticsearch_latency');
const esSuccessCounter = new Counter('elasticsearch_success');
const esErrorRate = new Rate('elasticsearch_errors');

export const options = {
  stages: [
    { duration: '5s', target: 20 },   // Khởi động 20 VUs
    { duration: '15s', target: 50 },  // 50 VUs liên tục tìm kiếm
    { duration: '5s', target: 0 },    // Hạ tải
  ],
  thresholds: {
    // 95% lượt tìm kiếm Elasticsearch phải phản hồi dưới 350ms, 99% dưới 600ms
    elasticsearch_latency: ['p(95)<350', 'p(99)<600'],
    // Tỷ lệ lỗi tìm kiếm phải < 1%
    elasticsearch_errors: ['rate<0.01'],
  },
};

// Danh sách các từ khóa tìm kiếm đa dạng: Tiếng Việt, Tiếng Anh, từ khóa mờ (Fuzzy)
const SEARCH_KEYWORDS = [
  'Avatar',
  'Dune',
  'Kung Fu',
  'Hành tinh',
  'Chiến binh',
  'Cameron',
  'Villeneuve',
  'Panda',
  'Khám phá',
  'Tình cảm',
];

export default function () {
  // Chọn ngẫu nhiên 1 từ khóa để giả lập hành vi tìm kiếm thực tế của người dùng
  const keyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
  const url = `${BASE_URL}/api/v1/catalogs/movies/search?keyword=${encodeURIComponent(keyword)}&page=0&size=10`;

  const headers = {
    'Accept': 'application/json',
  };

  const startTime = Date.now();
  const res = http.get(url, { headers });
  const latency = Date.now() - startTime;

  esLatencyTrend.add(latency);

  const passed = check(res, {
    'Elasticsearch trả về HTTP 200': (r) => r.status === 200,
    'Elasticsearch có dữ liệu JSON hợp lệ': (r) => {
      try {
        const body = r.json();
        return body && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    },
    'Thời gian phản hồi tìm kiếm < 300ms': () => latency < 300,
  });

  if (passed) {
    esSuccessCounter.add(1);
  } else {
    esErrorRate.add(1);
  }

  sleep(0.3); // Nghỉ 300ms trước lần tìm kiếm tiếp theo
}

export function handleSummary(data) {
  return {
    'k6-elasticsearch-report.html': htmlReport(data),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const latency = data.metrics.elasticsearch_latency ? data.metrics.elasticsearch_latency.values : {};
  const avg = latency.avg !== undefined ? latency.avg.toFixed(2) : 'N/A';
  const p95 = latency['p(95)'] !== undefined ? latency['p(95)'].toFixed(2) : 'N/A';
  const p99 = latency['p(99)'] !== undefined ? latency['p(99)'].toFixed(2) : 'N/A';
  const total = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;
  const errors = data.metrics.elasticsearch_errors ? data.metrics.elasticsearch_errors.values : {};
  const failed = errors.rate !== undefined ? (errors.rate * 100).toFixed(2) : '0.00';

  return '\n============================================================\n'
    + '       KẾT QUẢ KIỂM THỬ HIỆU NĂNG TÌM KIẾM ELASTICSEARCH\n'
    + '============================================================\n'
    + `Tổng số lượt tìm kiếm:          ${total}\n`
    + `Thời gian phản hồi trung bình:   ${avg} ms\n`
    + `95% lượt tìm kiếm phản hồi dưới: ${p95} ms (Kỳ vọng: < 200ms)\n`
    + `99% lượt tìm kiếm phản hồi dưới: ${p99} ms (Kỳ vọng: < 400ms)\n`
    + `Tỷ lệ lỗi tìm kiếm:              ${failed} %\n`
    + 'Báo cáo HTML trực quan: k6-elasticsearch-report.html\n'
    + '============================================================\n';
}
