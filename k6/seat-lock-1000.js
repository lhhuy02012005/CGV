import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8000').replace(/\/$/, '');
const showtimeId = __ENV.SHOWTIME_ID;
const seatId = __ENV.SEAT_ID;
const requests = Number(__ENV.SEAT_REQUESTS || 1000);
const tokens = new SharedArray('authenticated users', () => {
  if (!__ENV.TOKENS_FILE) throw new Error('TOKENS_FILE must point to JSON array of distinct users\' JWTs.');
  return JSON.parse(open(__ENV.TOKENS_FILE));
});
const totalRequests = new Counter('seat_requests');
const successfulLocks = new Counter('seat_success');
const conflictedLocks = new Counter('seat_conflict');
const unexpectedResponses = new Counter('seat_unexpected');

http.setResponseCallback(http.expectedStatuses(200, 409));

export const options = {
  scenarios: {
    one_seat_contention: {
      executor: 'per-vu-iterations',
      vus: requests,
      iterations: 1,
      maxDuration: '60s',
      gracefulStop: '0s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
  thresholds: {
    seat_requests: [`count==${requests}`],
    seat_success: ['count==1'],
    seat_conflict: [`count==${requests - 1}`],
    seat_unexpected: ['count==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (!uuid.test(showtimeId || '') || !uuid.test(seatId || '')) {
    throw new Error('SHOWTIME_ID and SEAT_ID must be real UUIDs from the application.');
  }
  if (!Number.isSafeInteger(requests) || requests < 2) throw new Error('SEAT_REQUESTS must be an integer >= 2.');
  if (tokens.length < requests) throw new Error(`Need ${requests} valid JWTs for distinct accounts.`);
  const runId = __ENV.RUN_ID || `seat-${Date.now()}`;
  console.log(`RUN_ID=${runId} SHOWTIME_ID=${showtimeId} SEAT_ID=${seatId}`);
  return { runId, showtimeId, seatId };
}

export default function (data) {
  const token = tokens[__VU - 1];
  const response = http.post(
    `${baseUrl}/api/v1/bookings/seat-locks`,
    JSON.stringify({ showtimeId: data.showtimeId, seatIds: [data.seatId] }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-Load-Test-Run': data.runId,
      },
      tags: { name: 'one_seat_contention', run_id: data.runId },
      timeout: '15s',
      redirects: 0,
    },
  );
  let validSuccess = false;
  if (response.status === 200) {
    try {
      const body = response.json();
      validSuccess = body.status === 200
        && body.data.showtimeId === data.showtimeId
        && Array.isArray(body.data.seatIds)
        && body.data.seatIds.length === 1
        && body.data.seatIds[0] === data.seatId
        && typeof body.data.expiresAt === 'string'
        && Number.isFinite(Date.parse(body.data.expiresAt))
        && Date.parse(body.data.expiresAt) > Date.now();
    } catch (_) {
      validSuccess = false;
    }
  }
  const conflict = response.status === 409;
  totalRequests.add(1);
  successfulLocks.add(validSuccess ? 1 : 0);
  conflictedLocks.add(conflict ? 1 : 0);
  unexpectedResponses.add(validSuccess || conflict ? 0 : 1);
  check(response, { 'valid seat hold or conflict': () => validSuccess || conflict });
  if (validSuccess) console.log(`WINNER_VU=${__VU}`);
}

export function handleSummary(data) {
  const count = (name) => data.metrics[name]?.values.count ?? 0;
  const timing = data.metrics.http_req_duration?.values || {};
  const allPassed = count('seat_requests') === requests
    && count('seat_success') === 1
    && count('seat_conflict') === requests - 1
    && count('seat_unexpected') === 0
    && Object.values(data.metrics).every((metric) =>
      Object.values(metric.thresholds || {}).every((threshold) => threshold.ok));
  const report = {
    run_id: __ENV.RUN_ID || null,
    scenario: 'one_seat_contention',
    baseUrl,
    showtimeId,
    seatId,
    requests: count('seat_requests'),
    success: count('seat_success'),
    conflicts: count('seat_conflict'),
    unexpected: count('seat_unexpected'),
    http_p95_ms: timing['p(95)'] ?? null,
    http_p99_ms: timing['p(99)'] ?? null,
    http_requests_per_second: data.metrics.http_reqs?.values.rate ?? null,
    thresholds_passed: allPassed,
  };
  const outputs = {
    stdout: `${JSON.stringify(report, null, 2)}\n`,
  };
  if (__ENV.K6_RESULT_PATH) outputs[__ENV.K6_RESULT_PATH] = JSON.stringify(report, null, 2);
  return outputs;
}
