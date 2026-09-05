# ADR 0007 — Versionamento da API

**Status:** aceito

## Contexto

O enunciado pergunta diretamente: como versionar a API e que estratégia usar.

A pergunta importa aqui por um motivo concreto, e não teórico. O cliente desta API é um aplicativo móvel
instalado no telefone dos associados. Não existe botão para atualizar todo mundo de uma vez: depois de uma
mudança no servidor, versões antigas do aplicativo continuam em uso por semanas. Qualquer alteração que
quebre o contrato quebra a votação de quem ainda não atualizou.

Então a estratégia de versionamento não é enfeite: é o que permite mudar o servidor sem derrubar quem está
com o aplicativo velho.

## Decisão

### Versão no caminho da URL

Toda rota vive sob `/api/v1`. É assim desde o primeiro endpoint, não foi acrescentado depois.

Alternativas consideradas:

- **Cabeçalho próprio** (`X-API-Version: 1`). Funciona, mas a versão fica invisível: não aparece no log de
  acesso, não aparece na barra do navegador, não aparece quando alguém cola um `curl` num chamado. Quem
  está depurando precisa lembrar de perguntar qual versão o cliente mandou.
- **Negociação de conteúdo** (`Accept: application/vnd.voting.v1+json`). É a opção mais fiel ao REST, e a
  mais fácil de errar. Um cliente móvel que esquece o cabeçalho cai numa versão padrão sem perceber, e o
  erro só aparece em produção.
- **Parâmetro de consulta** (`?version=1`). Polui a URL, some quando alguém copia o caminho e complica cache.

A versão no caminho ganha por ser **visível em todo lugar**: no código, no log, na documentação, no teste,
no relatório de erro. Para um contrato que precisa ser lido e conferido por gente dos dois lados, essa é a
propriedade que mais vale.

### O que muda sem trocar de versão

Mudança **aditiva** não quebra cliente e entra na v1:

- acrescentar um campo novo na resposta (o cliente antigo ignora o que não conhece);
- acrescentar um endpoint novo;
- acrescentar um campo **opcional** na requisição;
- afrouxar uma validação.

### O que exige uma v2

Tudo que faz um cliente correto parar de funcionar:

- remover ou renomear um campo da resposta;
- mudar o tipo de um campo, ou o significado dele com o mesmo nome;
- tornar obrigatório um campo de requisição que era opcional;
- mudar o código de status devolvido para uma situação que já existia;
- **acrescentar um valor novo a um enum existente**. Este é o caso que costuma passar batido: um aplicativo
  que trata `YES` e `NO` de forma exaustiva quebra ao receber um terceiro valor.

O formato de erro faz parte do contrato pelos mesmos critérios. Todas as respostas de falha são RFC 7807, e
mudar isso seria mudança de versão.

### Como as duas versões conviveriam

`/api/v1` e `/api/v2` no ar ao mesmo tempo, cada uma com seus controllers e seus objetos de entrada e saída.
As regras de negócio ficam nos serviços, que são compartilhados: o que se duplica é a tradução para HTTP,
não o domínio. É barato justamente porque controller não tem regra nenhuma (ADR 0001).

A retirada da versão antiga é anunciada, não executada de surpresa: respostas da v1 passam a trazer o
cabeçalho `Sunset` com a data, a documentação marca a versão como obsoleta, e a remoção só acontece quando as
métricas de uso mostrarem que o tráfego residual é aceitável.

## Consequências

- A versão aparece em qualquer lugar onde a requisição apareça, o que torna o suporte mais simples.
- Uma v2 custa controllers e DTOs novos, não domínio novo.
- Manter duas versões no ar tem custo real de manutenção, e é por isso que a régua do que exige v2 está
  escrita acima: para não criar versão nova por mudança que não precisava.
