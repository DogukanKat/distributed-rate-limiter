import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export const http_200_count = new Counter('http_200_count');
export const http_429_count = new Counter('http_429_count');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/hello';
const TENANT = __ENV.TENANT || 'acme';
const USER = __ENV.USER_ID || 'u1';

export const options = {
  scenarios: {
    burst: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.BURST_RPS || 300), // 300 RPS
      timeUnit: '1s',
      duration: __ENV.BURST_DURATION || '10s',
      preAllocatedVUs: Number(__ENV.BURST_VUS || 50),
      maxVUs: Number(__ENV.BURST_MAX_VUS || 100),
      exec: 'hit',
    },
    steady: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.STEADY_RPS || 3), // ~3 RPS
      timeUnit: '1s',
      duration: __ENV.STEADY_DURATION || '70s',
      preAllocatedVUs: Number(__ENV.STEADY_VUS || 50),
      maxVUs: Number(__ENV.STEADY_MAX_VUS || 100),
      startTime: __ENV.STEADY_START || '10s',
      exec: 'hit',
    },
  },

  tags: { expected_response: 'true' },

  discardResponseBodies: true,
  thresholds: {
    'http_req_failed{expected_response:true}': ['rate<0.01'],

    'http_429_count{scenario:burst}': ['count>100'],

    'http_200_count{scenario:steady}': ['count>100'],

    'http_req_duration{scenario:steady}': ['p(95)<50'],
  },

  gracefulStop: '5s',
};

export function hit() {
  const res = http.get(BASE_URL, {
    headers: {
      'X-Tenant-Id': TENANT,
      'X-User-Id': USER,
    },
  });

  if (res.status === 200) http_200_count.add(1);
  else if (res.status === 429) http_429_count.add(1);

  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });

  sleep(0.01);
}
