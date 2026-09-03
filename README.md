# Cooperative Voting API

API REST para gerenciar pautas e sessões de votação em assembleias de cooperativas. Cada associado tem
direito a um voto por pauta, e o resultado é apurado ao fim da sessão.

## Como rodar (caminho mais curto)

```bash
docker compose --profile app up --build
```

Sobe PostgreSQL e a API juntos. A API responde em `http://localhost:8080` e o health check em
`http://localhost:8080/actuator/health`.

Para desenvolver com a aplicação na máquina e só o banco em container:

```bash
make db-up      # sobe o PostgreSQL
make run        # roda a API na porta 8080
make verify     # gate completo: formatação + testes + cobertura
make help       # lista todos os alvos
```

Pré-requisitos, variáveis de ambiente e detalhes de execução: **[`docs/local-setup.md`](docs/local-setup.md)**.

## Stack

| Camada | Escolha |
| --- | --- |
| Runtime | Java 21, Spring Boot 4.1 |
| Build | Maven (via `./mvnw`, não precisa ter Maven instalado) |
| Web | Spring MVC + Bean Validation |
| Persistência | Spring Data JPA · PostgreSQL · Flyway |
| Testes | JUnit 5 · Testcontainers (PostgreSQL real) · ArchUnit |
| Qualidade | Spotless (formatação) · JaCoCo (gate de cobertura) · PIT (mutação) |
| Carga | k6 |
| Observabilidade | Spring Actuator · logs com correlation id |

## Estrutura

O código é agrupado **por domínio**, não por tipo de arquivo. Cada domínio segue
`Controller → Service → Repository`:

```
src/main/java/com/example/voting/
├── topic/         pautas
├── session/       sessões de votação
├── vote/          votos e apuração
├── eligibility/   contrato de elegibilidade do associado (porta)
├── integration/   adaptadores de sistemas externos
├── screens/       contrato de telas do app mobile (Anexo 1)
└── shared/        erros, configuração, logging
```

- `Controller` cuida só de HTTP: valida a entrada, chama **uma** função de serviço e devolve a resposta.
- `Service` tem a regra de negócio e é dono da transação. Lança exceções de domínio, nunca exceções de HTTP.
- `Repository` só acessa dados.

Um teste de ArchUnit garante que essas dependências não sejam invertidas.

## Glossário

O código é escrito em inglês; o enunciado do domínio é em português.

| Português | Código |
| --- | --- |
| Pauta | `Topic` |
| Sessão de votação | `VotingSession` |
| Voto | `Vote` |
| Associado | `Member` |
| Sim / Não | `YES` / `NO` |

## Endpoints

| Método | Rota | O que faz | Respostas |
| --- | --- | --- | --- |
| `POST` | `/api/v1/topics` | Cadastra uma pauta | `201` com a pauta e o cabeçalho `Location`, `400` se o título faltar |
| `GET` | `/api/v1/topics/{id}` | Busca uma pauta | `200` com a pauta, `404` se não existir |
| `POST` | `/api/v1/topics/{id}/sessions` | Abre a sessão de votação da pauta | `201` com a sessão, `404` se a pauta não existir, `409` se já houver sessão |
| `GET` | `/api/v1/sessions/{id}` | Busca a sessão e diz se está aberta | `200` com a sessão, `404` se não existir |
| `POST` | `/api/v1/topics/{id}/votes` | Registra o voto de um associado | `201` com o voto, `400` se a escolha não for `YES`/`NO`, `404` se a pauta não existir, `409` se o associado já votou, `422` se não houver sessão aberta |
| `GET` | `/api/v1/topics/{id}/result` | Apura o resultado da pauta | `200` com a contagem, `404` se a pauta não existir, `422` se a pauta nunca foi posta em votação |

A abertura aceita `durationMinutes` no corpo. Sem esse campo, a sessão fica aberta por **um minuto**.

O voto leva `memberId` (o identificador do associado) e `choice`, que é `YES` ou `NO`.

O resultado traz a contagem dos dois lados, o total, o desfecho (`APPROVED`, `REJECTED` ou `TIED`) e
`votingOpen`, dizendo se a votação ainda está em andamento. Ele fica disponível durante a sessão, como
parcial, e continua disponível depois que ela fecha.

Todo erro sai no formato RFC 7807 (`application/problem+json`), vindo de um único tratador central.

A documentação interativa (OpenAPI/Swagger UI) entra em uma fatia posterior.

## Decisões de projeto

As decisões e seus motivos estão registrados em [`docs/adr/`](docs/adr/), um arquivo curto por decisão.

## Testes de carga

_A preencher._ O script fica em [`load/voting.js`](load/voting.js) e roda com `make load` contra uma API já
no ar.

## Licença

MIT — ver [`LICENSE`](LICENSE).
