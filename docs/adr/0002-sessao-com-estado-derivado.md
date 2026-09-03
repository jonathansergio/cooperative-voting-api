# ADR 0002 — O estado da sessão de votação é derivado, não armazenado

**Status:** aceito

## Contexto

Uma sessão de votação fica aberta por um tempo determinado na abertura, ou por um minuto quando nada é
informado. Depois disso ela está fechada e não aceita mais votos. A pergunta de projeto é como o sistema
sabe que a sessão fechou.

O caminho intuitivo é guardar uma coluna `status` e ter um agendador que, de tempos em tempos, procura
sessões vencidas e troca o valor para `CLOSED`.

## Decisão

A sessão guarda apenas `opened_at` e `closes_at`. O estado aberto/fechado é **calculado a cada leitura**:

```java
boolean isOpenAt(Instant moment) {
    return moment.isBefore(closesAt);
}
```

Não existe coluna de status, não existe agendador, não existe job.

O momento atual vem de um `Clock` injetado, nunca de `Instant.now()`. É o que permite testar o fechamento
avançando o relógio em vez de esperar um minuto de tempo real. Um teste de ArchUnit impede que alguém volte
a chamar `Instant.now()` direto.

## Consequências

- **Não há janela de inconsistência.** Com agendador, existe sempre um intervalo entre o vencimento e a
  passagem do job em que a sessão está vencida mas ainda marcada como aberta. Aqui esse intervalo não
  existe.
- **Reinício da aplicação não perde nada.** Um agendador que estava fora do ar durante o vencimento deixaria
  sessões marcadas como abertas indefinidamente. Como o estado é calculado, o reinício é irrelevante.
- **Não há corrida entre o job e a votação.** Um voto que chega no exato instante do vencimento é decidido
  pela mesma comparação que qualquer outra leitura.
- **Uma peça a menos.** Sem agendador não há configuração de periodicidade, nem preocupação com múltiplas
  instâncias rodando o mesmo job ao mesmo tempo.
- O custo é uma comparação de instantes por leitura, que é irrelevante.

## Também decidido aqui

**Uma pauta tem no máximo uma sessão de votação**, garantido por uma restrição `unique (topic_id)` no banco.
O serviço não consulta antes de inserir para checar se já existe: duas requisições simultâneas passariam
pela consulta e as duas inseririam. O código insere, captura a violação de integridade e responde 409. O
banco é o único árbitro.

A existência da pauta, essa sim, é conferida antes, porque pauta não é apagada e a diferença entre
"pauta não existe" (404) e "pauta já tem sessão" (409) precisa chegar clara para quem chamou.
