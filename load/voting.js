// Teste de carga do caminho mais quente da API: o registro de votos.
//
// Rode a aplicação e depois `make load`. O cenário cria uma pauta, abre uma sessão longa o
// bastante para durar o teste inteiro, e então dispara votos de associados sempre distintos,
// que é como uma assembleia real se comporta.
//
// Variáveis: BASE_URL, PEAK_VUS, DURATION.
import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PEAK_VUS = Number(__ENV.PEAK_VUS || 200);
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const votesAccepted = new Counter('votes_accepted');
const votesRejected = new Counter('votes_rejected');

export const options = {
  stages: [
    { duration: '20s', target: PEAK_VUS },
    { duration: __ENV.DURATION || '1m', target: PEAK_VUS },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    // Um voto recusado sob carga é um defeito, não um detalhe: cada associado desta simulação
    // é diferente e a sessão está aberta, então todo voto tem de ser aceito.
    votes_rejected: ['count == 0'],
    http_req_failed: ['rate < 0.01'],
    'http_req_duration{operation:vote}': ['p(95) < 300'],
    'http_req_duration{operation:result}': ['p(95) < 300'],
  },
};

export function setup() {
  const topic = http.post(
    `${BASE_URL}/api/v1/topics`,
    JSON.stringify({ title: 'Pauta de carga', description: 'Gerada pelo teste de carga' }),
    JSON_HEADERS,
  );
  if (topic.status !== 201) {
    fail(`não consegui cadastrar a pauta: ${topic.status} ${topic.body}`);
  }
  const topicId = topic.json('id');

  // Longa o bastante para a sessão não fechar no meio do teste e passar a recusar votos.
  const session = http.post(
    `${BASE_URL}/api/v1/topics/${topicId}/sessions`,
    JSON.stringify({ durationMinutes: 60 }),
    JSON_HEADERS,
  );
  if (session.status !== 201) {
    fail(`não consegui abrir a sessão: ${session.status} ${session.body}`);
  }
  return { topicId };
}

export default function (data) {
  const memberId = `1${String(__VU).padStart(4, '0')}${String(__ITER).padStart(6, '0')}`;
  const vote = http.post(
    `${BASE_URL}/api/v1/topics/${data.topicId}/votes`,
    JSON.stringify({ memberId, choice: __ITER % 3 === 0 ? 'NO' : 'YES' }),
    { ...JSON_HEADERS, tags: { operation: 'vote' } },
  );
  check(vote, { 'voto aceito': (r) => r.status === 201 })
    ? votesAccepted.add(1)
    : votesRejected.add(1);

  // A apuração é consultada durante a votação, então entra na medição junto.
  if (__ITER % 10 === 0) {
    const result = http.get(`${BASE_URL}/api/v1/topics/${data.topicId}/result`, {
      tags: { operation: 'result' },
    });
    check(result, { 'resultado apurado': (r) => r.status === 200 });
  }
}

export function teardown(data) {
  const result = http.get(`${BASE_URL}/api/v1/topics/${data.topicId}/result`);
  console.log(`resultado final da pauta ${data.topicId}: ${result.body}`);
}
