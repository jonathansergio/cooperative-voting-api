# ADR 0006 — Logs e correlação de requisições

**Status:** aceito

## Contexto

O enunciado avalia "logs da aplicação". A pergunta não é se existe log, e sim se o log serve para alguma
coisa quando um problema aparece.

O cenário que a solução precisa aguentar é uma assembleia com muitos associados votando ao mesmo tempo.
Nesse cenário, log sem correlação é quase inútil: as linhas de dezenas de requisições simultâneas se
intercalam, e reconstruir o que aconteceu com um voto específico vira adivinhação.

## Decisão

### Toda requisição tem um identificador

Um filtro coloca um `correlationId` no MDC no começo de cada requisição e o devolve no cabeçalho
`X-Correlation-Id`. Se quem chamou já mandou esse cabeçalho, o valor é preservado, para que um rastro
atravesse sistemas. Se não mandou, um identificador novo é gerado.

O padrão de log inclui esse valor, então toda linha produzida durante a requisição sai marcada. Como o
cabeçalho volta na resposta, quem recebeu um erro consegue dizer exatamente qual requisição investigar.

O identificador é removido do MDC no `finally`. A thread volta para o pool e vai atender outra pessoa; um
MDC não limpo marcaria a requisição seguinte com o identificador da anterior.

### Uma linha por requisição, com o desfecho

Método, caminho, status e duração. É o suficiente para responder "o que aconteceu" e "estava lento".

**O corpo nunca é registrado.** Nesta API o corpo do voto carrega o CPF do associado, e log é copiado,
enviado para agregadores e lido por muita gente. Pelo mesmo motivo, o identificador do associado não aparece
nas linhas de log.

O health check é excluído: ele é consultado o tempo todo pelo orquestrador e afogaria as requisições que
importam.

### Falha de sistema externo é registrada com a causa

Quando o serviço de elegibilidade não responde, a exceção original vai para o log em nível `warn`, com a
mensagem do fornecedor. Essa mensagem nunca vai para a resposta HTTP: quem chamou recebe apenas 503 e o
motivo em linguagem de domínio.

## O que não foi feito

Não há log de evento de negócio ("voto registrado", "sessão aberta"). A linha por requisição já registra o
desfecho de cada operação com seu status, e duplicar isso em nível de serviço acrescentaria ruído sem
acrescentar informação. Se auditoria de votos virar requisito, ela não deve ser resolvida com log, e sim com
uma trilha persistida, que é uma decisão diferente.

Não foi adotado log em JSON. Ele é o formato certo quando existe um agregador do outro lado; aqui, tornaria
a saída do `docker compose up` ilegível para quem só quer executar o projeto.
