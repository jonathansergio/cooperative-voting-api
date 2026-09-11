# Rodando localmente

Fonte da verdade para pré-requisitos, configuração e comandos. Qualquer mudança em variável de ambiente,
serviço de apoio, ferramenta obrigatória ou passo de execução atualiza este arquivo na mesma alteração.

## 1. Pré-requisitos

| Ferramenta | Versão | Observação |
| --- | --- | --- |
| JDK | 21 | `java -version` deve apontar para 21 |
| Docker + Compose | qualquer versão atual | usado pelo banco, pelos testes e pelo teste de carga |
| Maven | — | **não é necessário**; use `./mvnw` |

Os testes usam **Testcontainers**, que sobe um PostgreSQL descartável por execução. Docker precisa estar
rodando para `make test` e `make verify`.

## 2. Subindo tudo em containers

```bash
docker compose up
```

Sobe banco e API. A API só inicia depois que o health check do banco passa.

A imagem da API é baixada de `ghcr.io/jonathansergio/cooperative-voting-api`, publicada automaticamente a
cada mudança na `main`. Se ela não puder ser baixada, o Compose constrói a partir do código deste
repositório. Para forçar a construção local, depois de alterar o código:

```bash
make up      # docker compose up --build -d
make down

## 3. Desenvolvendo na máquina

```bash
make db-up   # sobe só o PostgreSQL na porta 5432
make run     # roda a API na porta 8080
```

Com a aplicação no ar, a documentação interativa fica em
<http://localhost:8080/swagger-ui/index.html> e o documento OpenAPI em
<http://localhost:8080/v3/api-docs>.

## 4. Variáveis de ambiente

Todas têm default para desenvolvimento local, então nada precisa ser exportado para rodar.

| Variável | Default | Para que serve |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/voting` | JDBC do PostgreSQL |
| `DATABASE_USER` | `voting` | usuário do banco |
| `DATABASE_PASSWORD` | `voting` | senha do banco |
| `DATABASE_POOL_SIZE` | `30` | tamanho do pool HikariCP (valor medido, ver ADR 0008) |
| `PORT` | `8080` | porta HTTP da API |
| `SCREENS_BASE_URL` | `http://localhost:8080` | endereço base nas URLs das telas do aplicativo |
| `ELIGIBILITY_MODE` | `remote` | `remote` consulta o serviço de elegibilidade de verdade; `stub` responde que todos podem votar |
| `ELIGIBILITY_BASE_URL` | `https://user-info.herokuapp.com` | endereço do serviço de elegibilidade |
| `ELIGIBILITY_CONNECT_TIMEOUT` | `2s` | tempo máximo para abrir a conexão |
| `ELIGIBILITY_READ_TIMEOUT` | `3s` | tempo máximo para a resposta |

### Sobre o serviço de elegibilidade

O endereço que o enunciado indica é um aplicativo em camada gratuita da Heroku, plano encerrado em 2022, e
provavelmente está fora do ar. O cliente real está implementado e é o padrão da aplicação, mas o
`docker compose` sobe com `ELIGIBILITY_MODE=stub` para que a stack funcione sem depender de terceiros.

Para exercitar a integração de verdade:

```bash
ELIGIBILITY_MODE=remote ELIGIBILITY_BASE_URL=https://user-info.herokuapp.com make run
```

Se o serviço não responder, o voto recebe `503`. A aplicação nunca supõe que um associado pode votar quando
não conseguiu confirmar.

## 5. Comandos

| Comando | O que faz |
| --- | --- |
| `make verify` | gate completo: formatação + testes + gate de cobertura (mesmo que o CI roda) |
| `make test` | só a suíte de testes |
| `make lint` | confere formatação sem reescrever arquivos |
| `make format` | formata o código |
| `make cov` | roda `verify` e aponta o relatório de cobertura |
| `make mutation` | teste de mutação sobre todo o domínio, sob demanda; os testes gerados com banco ficam de fora dele e rodam no `make verify` |
| `make build` | empacota o jar sem rodar testes |
| `make load` | teste de carga com k6 contra uma API já no ar |
| `make up` / `make down` | sobe/derruba a stack inteira em containers |
| `make db-up` / `make db-down` | sobe/derruba só o banco |
| `make help` | lista os alvos |

## 6. Banco de dados

O schema é versionado com **Flyway**, em `src/main/resources/db/migration`. As migrations rodam sozinhas no
start. `spring.jpa.hibernate.ddl-auto` é `validate`: o Hibernate confere que as entidades batem com o schema
criado pelo Flyway, mas nunca altera o banco.

Para começar do zero:

```bash
docker compose down -v && make db-up
```
