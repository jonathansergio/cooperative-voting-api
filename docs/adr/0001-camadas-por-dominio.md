# ADR 0001 — Camadas por domínio: Controller → Service → Repository

**Status:** aceito

## Contexto

A API tem poucos conceitos (pauta, sessão, voto) e precisa ser lida rápido por quem nunca viu o código. Duas
organizações comuns competem: agrupar por tipo de arquivo (`controllers/`, `services/`, `entities/`) ou
agrupar por domínio (`topic/`, `session/`, `vote/`).

## Decisão

Agrupar **por domínio**. Cada pacote de domínio contém o próprio controller, service, repository, entidade e
DTOs. Dentro do domínio, a dependência é sempre em uma direção:

- **Controller** — só HTTP. Valida a entrada, chama uma função de serviço, devolve a resposta. Nenhuma regra
  de negócio.
- **Service** — regra de negócio, dono da transação. Lança exceções de domínio (`NotFoundException`,
  `ConflictException`, `UnprocessableException`); nunca lança exceção de HTTP. Um handler central traduz para
  a resposta de erro.
- **Repository** — só acesso a dados.

Uma camada só existe onde há responsabilidade. **Não criamos service vazio** que apenas repassa uma chamada
do controller para o repository.

## Consequências

- Ler uma funcionalidade inteira exige abrir um diretório, não quatro.
- O service é framework-agnóstico, o que o torna o alvo natural dos testes e do teste de mutação.
- Como a regra é fácil de violar sem querer, um teste de ArchUnit verifica as direções de dependência a cada
  build.

## Alternativas consideradas

- **Agrupar por tipo de arquivo:** familiar, mas espalha uma funcionalidade por vários diretórios e não
  escala junto com o domínio.
- **Arquitetura hexagonal completa** (portas e adaptadores em todo lugar): traria interfaces e mapeadores
  demais para um domínio deste tamanho. Usamos porta/adaptador só onde há de fato um sistema externo — a
  consulta de elegibilidade do associado.
