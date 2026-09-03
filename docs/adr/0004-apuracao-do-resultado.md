# ADR 0004 — Apuração do resultado

**Status:** aceito

## Contexto

O enunciado pede "contabilizar os votos e dar o resultado da votação na pauta". Três perguntas ficaram em
aberto: como contar, quando o resultado pode ser consultado e o que responder em caso de empate.

## Como contar

A contagem é feita por consulta agregada no banco, uma por lado:

```java
votes.countByTopicIdAndChoice(topicId, Choice.YES)
votes.countByTopicIdAndChoice(topicId, Choice.NO)
```

O que **não** é feito: carregar os votos da pauta e contar em memória. O enunciado pede que a solução se
comporte bem com centenas de milhares de votos, e trazer essas linhas para a aplicação só para descartá-las
depois de somar é o caminho mais direto para estourar memória e tempo de resposta.

A migration V4 cria o índice `(topic_id, choice)`, que atende exatamente essas duas contagens. O custo da
apuração passa a depender do tamanho do índice, não da quantidade de votos trazida para a memória.

## Quando o resultado pode ser consultado

**Sempre que a pauta tiver uma sessão**, aberta ou fechada. A resposta traz um campo `votingOpen` dizendo em
qual dos dois casos o chamador está.

O caminho alternativo seria recusar o resultado enquanto a sessão estiver aberta. Foi descartado por dois
motivos. O enunciado não pede sigilo durante a votação, e o foco declarado da avaliação é a comunicação com
o aplicativo, que precisa mostrar a parcial na tela enquanto a assembleia acontece. Se sigilo vier a ser um
requisito, o campo `votingOpen` já é o lugar onde essa regra entraria.

Pauta que nunca teve sessão não tem resultado: responde 422. Não é erro de preenchimento, é o estado da
pauta que não permite a pergunta.

## Empate

O desfecho é `APPROVED`, `REJECTED` ou `TIED`, calculado por comparação simples entre os dois lados. Pauta
sem nenhum voto cai em `TIED`, porque zero é igual a zero. Um quarto valor para "ninguém votou" foi
considerado e descartado: acrescentaria estado à API sem que nada no enunciado dependa dessa distinção, e o
campo `total` já responde a pergunta para quem precisar dela.

O desempate por regra de negócio (voto de minerva, quórum mínimo) não existe no enunciado e não foi
inventado.
