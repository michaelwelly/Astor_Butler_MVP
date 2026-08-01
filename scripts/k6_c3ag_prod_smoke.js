import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.C3AG_BASE_URL || "http://51.250.31.97:3001";
const backendUrl = __ENV.C3AG_BACKEND_URL || "http://51.250.31.97:8089";

export const options = {
  scenarios: {
    read_only_smoke: {
      executor: "constant-vus",
      vus: Number(__ENV.C3AG_K6_VUS || 2),
      duration: __ENV.C3AG_K6_DURATION || "5m",
      gracefulStop: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<1500"],
    checks: ["rate>0.98"],
  },
};

export default function () {
  const pages = ["/", "/film", "/wedding", "/podcast", "/product-covers/film.jpg", "/clio-avatar.jpg"];
  for (const path of pages) {
    const res = http.get(`${baseUrl}${path}`, {
      tags: { kind: "frontend", path },
      timeout: "10s",
    });
    check(res, {
      "frontend status is 200": (r) => r.status === 200,
      "frontend response has body": (r) => r.body && r.body.length > 0,
    });
    sleep(0.5);
  }

  const health = http.get(`${backendUrl}/actuator/health`, {
    tags: { kind: "backend", path: "/actuator/health" },
    timeout: "5s",
  });
  check(health, {
    "backend health is 200": (r) => r.status === 200,
    "backend health is up-ish": (r) => r.body && r.body.includes("UP"),
  });

  sleep(2);
}
