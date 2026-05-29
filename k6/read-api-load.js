import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  stages: [
    { duration: '20s', target: 5 },
    { duration: '40s', target: 10 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(50)<250', 'p(95)<750', 'p(99)<1500'],
  },
};

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';

export default function () {
  const response = http.get(`${baseUrl}/api/system/ping`);

  check(response, {
    'system ping is ok': (res) => res.status === 200 && res.json('status') === 'ok',
  });

  sleep(1);
}
