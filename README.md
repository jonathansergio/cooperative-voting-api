# Cooperative Voting API

API REST para gerenciar pautas e sessões de votação em assembleias de cooperativas. Cada associado tem
direito a um voto por pauta, e o resultado é apurado ao fim da sessão.

## Como testar (três passos, só com Docker)

1. **Subir a aplicação:**

   ```bash
   docker compose up
   ```

   Baixa a imagem já publicada e sobe PostgreSQL e API juntos. Nada é compilado, e não é preciso ter Java
   nem Maven na máquina.

2. **Importar no Postman** o arquivo
   [`docs/postman/cooperative-voting-api.postman_collection.json`](docs/postman/cooperative-voting-api.postman_collection.json).

3. **Clicar em Run collection.** Cada requisição verifica o status esperado e passa os identificadores para
   a seguinte; a execução termina toda verde.

Com a aplicação no ar, a API responde em `http://localhost:8080`, o health check em
`http://localhost:8080/actuator/health` e a documentação interativa em
`http://localhost:8080/swagger-ui/index.html`.

Nessa forma de subir, a consulta de elegibilidade do associado roda em modo `stub`, respondendo que todos
podem votar. O motivo é que o serviço indicado no enunciado está hospedado em uma camada gratuita encerrada
em 2022 e provavelmente não responde. O cliente real está implementado, testado e é o padrão da aplicação:
`ELIGIBILITY_MODE=remote` usa ele. Ver [`docs/adr/0005`](docs/adr/0005-integracao-de-elegibilidade.md).

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
| Documentação da API | springdoc-openapi (Swagger UI) |
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

O voto também consulta o serviço externo de elegibilidade do associado: quem não pode votar, ou cujo CPF o
serviço não conhece, recebe `422`. Se esse serviço não responder, a resposta é `503` — a aplicação nunca
supõe que alguém pode votar sem ter confirmado. Detalhes em [`docs/adr/0005`](docs/adr/0005-integracao-de-elegibilidade.md).

A abertura aceita `durationMinutes` no corpo. Sem esse campo, a sessão fica aberta por **um minuto**.

O voto leva `memberId` (o identificador do associado) e `choice`, que é `YES` ou `NO`.

O resultado traz a contagem dos dois lados, o total, o desfecho (`APPROVED`, `REJECTED` ou `TIED`) e
`votingOpen`, dizendo se a votação ainda está em andamento. Ele fica disponível durante a sessão, como
parcial, e continua disponível depois que ela fecha.

Todo erro sai no formato RFC 7807 (`application/problem+json`), vindo de um único tratador central.

Toda resposta traz o cabeçalho `X-Correlation-Id`, que também marca as linhas de log daquela requisição. Se
você mandar esse cabeçalho, o valor é preservado. Detalhes em
[`docs/adr/0006`](docs/adr/0006-logs-e-correlacao-de-requisicoes.md).

### Telas do aplicativo (anexo 1)

Além da API REST, o servidor expõe o contrato de telas que o aplicativo móvel entende, no formato do anexo
do enunciado. Cada resposta é uma tela inteira, e cada botão traz o endereço do passo seguinte, então o
aplicativo caminha pelo fluxo sem conhecer nenhuma regra.

| Método | Rota | Tela |
| --- | --- | --- |
| `GET` | `/api/v1/screens/topics` | `SELECAO` com as pautas em votação |
| `POST` | `/api/v1/screens/topics/{id}/identify` | `FORMULARIO` pedindo o CPF de quem vai votar |
| `POST` | `/api/v1/screens/topics/{id}/choose` | `SELECAO` com "Sim" e "Não" |
| `POST` | `/api/v1/screens/topics/{id}/votes` | `FORMULARIO` de confirmação |

O endereço base dessas URLs é configurável por `SCREENS_BASE_URL`, porque o emulador e um aparelho físico
não alcançam o servidor pelo mesmo endereço.

Um voto recusado também volta como tela, com o motivo em texto: esse cliente desenha telas, não sabe
renderizar documento de erro. A API REST continua respondendo `409` e `422` normalmente. Detalhes em
[`docs/adr/0009`](docs/adr/0009-telas-do-aplicativo.md).

### Documentação interativa

Com a aplicação no ar:

- **Swagger UI** — <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI (JSON)** — <http://localhost:8080/v3/api-docs>

Cada operação traz o resumo, os campos e todos os códigos de resposta que o cliente precisa tratar. Um teste
verifica que o documento continua descrevendo todos os endpoints, então ele não envelhece em silêncio.

## Versionamento

A versão fica no caminho: toda rota vive sob `/api/v1`, desde o primeiro endpoint. Mudança aditiva (campo
novo na resposta, endpoint novo, campo opcional na requisição) continua na v1. Mudança que quebra um cliente
correto — remover ou renomear campo, mudar tipo ou significado, tornar obrigatório o que era opcional,
acrescentar valor a um enum existente — exige uma v2, servida em paralelo e reaproveitando os mesmos
serviços.

O motivo de tratar isso a sério: o cliente é um aplicativo instalado no telefone dos associados, e não há
como forçar todo mundo a atualizar de uma vez. A régua completa está em
[`docs/adr/0007`](docs/adr/0007-versionamento-da-api.md).

## Collection do Postman

[`docs/postman/cooperative-voting-api.postman_collection.json`](docs/postman/cooperative-voting-api.postman_collection.json)
traz o roteiro completo em quatro pastas:

1. **Fluxo principal** — cadastrar pauta, abrir sessão, votar e apurar;
2. **Recusas** — cada uma com o status que devolve: voto repetido e sessão duplicada (`409`), escolha
   inválida e pauta sem título (`400`), pauta inexistente (`404`), voto e resultado de pauta sem sessão
   (`422`);
3. **Telas do aplicativo** — o fluxo do anexo 1, incluindo o voto recusado devolvido como tela;
4. **Operação** — health check, documento OpenAPI e correlação de requisição.

Cada requisição verifica o status e o conteúdo esperados e guarda os identificadores para as seguintes, então
**Run collection** executa tudo em sequência e termina verde. Cada execução cria a própria pauta, então pode
ser repetida quantas vezes quiser. Se a API estiver em outro endereço, altere a variável `baseUrl` na aba
**Variables** da collection.

## Decisões de projeto

Um arquivo curto por decisão em [`docs/adr/`](docs/adr/), com índice em
[`docs/adr/README.md`](docs/adr/README.md): camadas, estado derivado da sessão, unicidade do voto no banco,
apuração, integração externa, logs, versionamento, desempenho e o contrato de telas.

## Testes de carga

O cenário em [`load/voting.js`](load/voting.js) cadastra uma pauta, abre uma sessão e dispara votos de
associados sempre distintos, consultando a apuração no meio da votação.

```bash
make db-up
ELIGIBILITY_MODE=stub make run
make load
```

Duzentos usuários simultâneos, noventa segundos, banco zerado antes de cada execução. **Uma execução
registra mais de 250 mil votos**, sem nenhum voto recusado e sem nenhuma requisição com erro.

| Pool de conexões | Votos por segundo | p95 do voto | p95 da apuração |
| --- | --- | --- | --- |
| 20 | 2.390 | 157,9 ms | 204,1 ms |
| **30** (padrão) | **2.856** | **123,2 ms** | **222,4 ms** |
| 50 | 2.909 | 114,4 ms | 298,1 ms |

O pool ficou em 30 por medida, não por chute: entrega 19% mais votos por segundo que 20 e mantém a apuração
com folga no orçamento de 300 ms. Cinquenta compra 2% de vazão a mais e paga 76 ms na apuração, terminando a
dois milissegundos do limite.

A apuração não carrega votos para a memória: o plano de execução confirma `Index Only Scan` com
`Heap Fetches: 0`, contando 144 mil linhas em 11 ms. O que fica dentro dessas medições, o que ficou de fora e
qual é o limite real do sistema estão em [`docs/adr/0008`](docs/adr/0008-desempenho-sob-carga.md).

## Licença

MIT — ver [`LICENSE`](LICENSE).
