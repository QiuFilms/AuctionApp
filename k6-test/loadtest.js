import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,              // liczba użytkowników wirtualnych
  duration: '30s',      // czas trwania testu
};

const BASE_URL = 'https://localhost:8080';

export default function () {
  const params = {
    headers: {
      'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0MiIsImlhdCI6MTc4MzI0ODMyNCwiZXhwIjoxNzgzMzM0NzI0fQ.1IbwWjgar3G-0qfS1-7n2ri1eh_fdtsKCYfVB2OrGpI',
    },
  };

  const res = http.get(`${BASE_URL}/api/inventory`, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'transaction time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);
}