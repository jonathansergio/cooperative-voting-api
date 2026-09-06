# ADR 0008 — Desempenho sob carga

**Status:** aceito

## Contexto

A tarefa bônus 2 pede que a aplicação se comporte bem em cenários com centenas de milhares de votos, e
sugere teste de carga como forma de observar isso. A pergunta prática é: o desenho aguenta, e onde ele dobra.

## Como foi medido

Um cenário k6 (`load/voting.js`) cadastra uma pauta, abre uma sessão longa o bastante para não fechar no
meio do teste, e dispara votos de associados sempre distintos, que é como uma assembleia real se comporta.
A cada dez iterações a apuração é consultada, porque durante a votação o aplicativo mostra a parcial.

Duzentos usuários simultâneos, noventa segundos por execução, banco zerado antes de cada uma. Um voto
recusado conta como falha do teste: os associados são todos diferentes e a sessão está aberta, então todo
voto tem de entrar.

As medições usam o modo `stub` da consulta de elegibilidade. Isso é deliberado: mede o que **esta**
aplicação faz, sem embutir a latência de um serviço de terceiro que está fora do ar. A consequência disso
está descrita mais abaixo.

## O que a medição mostrou

Uma execução de noventa segundos registra mais de 250 mil votos, ou seja, a ordem de grandeza que o
enunciado pede acontece em um minuto e meio.

| Pool de conexões | Votos por segundo | p95 do voto | p95 da apuração |
| --- | --- | --- | --- |
| 20 | 2.390 | 157,9 ms | 204,1 ms |
| **30** | **2.856** | **123,2 ms** | **222,4 ms** |
| 50 | 2.909 | 114,4 ms | 298,1 ms |

Nenhuma execução teve voto recusado ou requisição com erro.

## Decisão

**Pool de conexões passa de 20 para 30.** O valor anterior tinha sido escolhido sem medida nenhuma. Trinta
entrega 19% mais votos por segundo que vinte, e mantém a apuração com 78 ms de folga no orçamento de 300 ms.

**Cinquenta foi descartado.** Compra 2% de vazão a mais que trinta e paga com 76 ms na apuração, que passa a
terminar a 298 ms num orçamento de 300. Escolher um valor cuja margem medida é de dois milissegundos não é
decisão, é sorte: em uma máquina mais lenta ele quebra.

**Nada além disso foi alterado.** As outras suspeitas foram investigadas e não se confirmaram:

- **A apuração não carrega votos para a memória.** O plano de execução confirma varredura apenas por índice:
  `Index Only Scan using votes_topic_choice_idx`, `Heap Fetches: 0`, 11 ms para contar 144 mil linhas em uma
  tabela de 216 mil. O índice criado na V4 faz o trabalho.
- **O registro do voto já é uma escrita só.** Não há consulta antes de inserir (ADR 0003), então não havia
  ida ao banco para economizar.

Otimizar o que a medida não apontou seria trocar código legível por código complicado sem contrapartida.

## O limite real: a consulta de elegibilidade

Os números acima são com o modo `stub`. Com o serviço de verdade, **cada voto carrega uma chamada HTTP de
saída**, e a latência dela entra inteira na latência do voto. A partir daí o teto deixa de ser desta
aplicação e passa a ser o do serviço de terceiro.

O que já está feito a respeito: os dois timeouts são sempre definidos, então uma chamada lenta não prende
uma thread indefinidamente, e o cliente HTTP reaproveita conexões em vez de abrir uma por voto.

O que **não** adianta: cache por CPF. Cada associado vota uma única vez por pauta, então a segunda consulta
para o mesmo CPF praticamente não acontece.

Se esse limite se tornar um problema real, as saídas passam por mudar o contrato com o fornecedor — consulta
em lote, ou verificação antecipada dos associados aptos antes da assembleia começar. Nenhuma delas é decisão
que este exercício comporta tomar sozinho, e por isso estão registradas aqui como caminho, não executadas.

## Como reproduzir

```bash
make db-up
ELIGIBILITY_MODE=stub make run
make load
```

`PEAK_VUS` e `DURATION` ajustam a carga.
