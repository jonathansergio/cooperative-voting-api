# ADR 0003 — Um voto por associado é garantido pelo banco, não pelo código

**Status:** aceito

## Contexto

Cada associado pode votar uma única vez por pauta. É a regra mais fácil de implementar errado no desafio
inteiro, porque a forma intuitiva parece correta:

```java
if (votes.existsByTopicIdAndMemberId(topicId, memberId)) {
    throw new ConflictException(...);
}
votes.save(new Vote(...));
```

Esse código funciona em teste manual e falha em produção. Duas requisições do mesmo associado chegando ao
mesmo tempo executam a consulta antes de qualquer uma das duas inserir. As duas encontram a tabela vazia, as
duas passam pelo `if`, as duas inserem. O associado votou duas vezes.

Não é um caso teórico: o enunciado pede que a solução se comporte bem com centenas de milhares de votos,
e é exatamente sob concorrência que esse tipo de defeito aparece.

## Decisão

A regra vive no banco:

```sql
constraint votes_one_per_member_per_topic unique (topic_id, member_id)
```

O código **nunca consulta antes de inserir**. Ele insere, e trata a violação de integridade como a resposta:

```java
try {
    return VoteResponse.from(votes.saveAndFlush(new Vote(topicId, memberId, choice, clock.instant())));
} catch (DataIntegrityViolationException alreadyVoted) {
    throw new ConflictException("Member %s has already voted on topic %d".formatted(memberId, topicId));
}
```

O `saveAndFlush` é deliberado: força a ida ao banco dentro do `try`. Um `save` comum adiaria a escrita para o
fim da transação, e a violação estouraria fora do bloco, longe de onde o significado dela é conhecido.

## Como isso é verificado

Um teste dispara dezesseis votos do mesmo associado ao mesmo tempo, soltos juntos por uma barreira, e exige
que exatamente um seja aceito e que a tabela contenha exatamente uma linha. Com a versão "consultar antes de
inserir" esse teste falha; é o que o torna útil.

## Consequências

- A garantia não depende de o código estar certo, nem de quantas instâncias da aplicação estão no ar.
- Uma escrita a menos por voto, já que não há consulta prévia. No caminho mais quente do sistema.
- O tratamento de erro fica um pouco menos direto: é preciso capturar uma exceção de infraestrutura e
  traduzi-la para significado de domínio. É um preço pequeno e pago em um lugar só.

## A mesma postura em outros lugares

A restrição de uma sessão por pauta (ADR 0002) segue o mesmo padrão. Já a existência da pauta é conferida
com consulta antes, porque pauta não é apagada e quem chama precisa distinguir 404 de 409.
