# ADR 0005 — Integração com o serviço de elegibilidade

**Status:** aceito

## Contexto

A tarefa bônus 1 pede que o voto só seja aceito se o serviço externo disser que o associado pode votar:

```
GET https://user-info.herokuapp.com/users/{cpf}
404               -> CPF inválido
200 ABLE_TO_VOTE  -> pode votar
200 UNABLE_TO_VOTE -> não pode votar
```

Dois fatos moldam a decisão. Primeiro, esse endereço é um aplicativo em camada gratuita da Heroku, plano que
foi encerrado em 2022; a chance de ele estar no ar durante a avaliação é baixa. Segundo, o próprio enunciado
pede que o domínio das URLs de callback seja alterável por configuração.

## Decisão

### Porta e adaptador, só aqui

Este é o único sistema externo de verdade da aplicação, e é o único lugar que ganha porta e adaptador. O
domínio declara o que precisa saber em `eligibility/`:

```java
public interface EligibilityChecker {
    EligibilityStatus statusOf(String memberId);
}
```

Tudo que é específico do fornecedor — HTTP, formato da resposta, o significado do 404 — fica em
`integration/userinfo/`. O `record` que espelha o payload nunca sai desse pacote; o domínio só enxerga o
enum de três valores. Trocar de fornecedor é escrever outro adaptador.

Nos outros pontos do sistema não há porta nem adaptador. Criar interfaces para o próprio banco seria
cerimônia sem contrapartida, e o enunciado penaliza over engineering explicitamente.

### Configuração, nunca host fixo

O endereço vem de `voting.eligibility.base-url`, com variável de ambiente por cima. Os dois timeouts,
conexão e leitura, são sempre definidos: uma chamada sem prazo prende uma thread de requisição pelo tempo
que o serviço remoto quiser.

### Dois modos

`voting.eligibility.mode` escolhe entre `remote`, que é o padrão e fala com o serviço de verdade, e `stub`,
que responde que todos podem votar.

O modo `stub` não é um atalho para não implementar a integração: o cliente real está escrito, testado e é o
padrão. Ele existe porque a aplicação precisa ser executável mesmo com o serviço do enunciado fora do ar, e
porque testes automatizados não devem depender de uma rede que responde ao acaso. O `docker compose` sobe
com `stub` para que a avaliação funcione sem depender de terceiros; trocar para `remote` é uma variável de
ambiente.

### Falha do serviço não vira palpite

Se o serviço não responde, o voto não é aceito nem recusado por mérito: a resposta é 503. Tratar
indisponibilidade como "pode votar" abriria um buraco na regra, e tratar como "não pode votar" recusaria
associados legítimos por um problema que não é deles.

## Como isso é testado

O contrato do adaptador é fixado contra um transporte simulado, com `MockRestServiceServer`, cobrindo os
quatro casos: pode votar, não pode votar, 404, e falha do serviço. Não foi usada nenhuma biblioteca nova de
mock de HTTP, porque o próprio `spring-test` já resolve.

As regras de negócio que dependem da elegibilidade são testadas pela API, com um dublê que a suíte programa
para responder o que cada teste precisa.
