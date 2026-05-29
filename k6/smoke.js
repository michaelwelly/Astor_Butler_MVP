import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<1000'],
  },
};

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';

export default function () {
  const endpoints = [
    '/api/system/ping',
    '/api/system/readiness',
    '/actuator/health',
    '/v3/api-docs',
  ];

  for (const endpoint of endpoints) {
    const response = http.get(`${baseUrl}${endpoint}`);
    check(response, {
      [`GET ${endpoint} returns 2xx`]: (res) => res.status >= 200 && res.status < 300,
    });
  }
}
