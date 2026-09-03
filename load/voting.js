// Teste de carga (k6). Rode a API primeiro e depois `make load`.
// Por enquanto só verifica que o serviço responde sob carga; os cenários de votação entram
// junto com os endpoints correspondentes.
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
