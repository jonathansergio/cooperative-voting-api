# ADR 0009 — Contrato de telas do aplicativo (anexo 1)

**Status:** aceito

## Contexto

O anexo 1 do enunciado descreve o formato das mensagens que o aplicativo móvel entende. São dois tipos de
tela, `FORMULARIO` e `SELECAO`, e o aplicativo apenas desenha o que recebe: cada botão traz a URL para onde
postar e o corpo que deve acompanhar o clique.

O enunciado diz que o foco da avaliação é a comunicação entre o backend e o aplicativo, e que o formato
dessas mensagens está detalhado nesse anexo. Ignorá-lo deixaria de fora justamente a parte que o enunciado
destaca.

## Decisão

### Um módulo de apresentação, separado da API REST

As telas vivem em `screens/`, sob `/api/v1/screens`, e não substituem nada. A API REST continua igual, com
suas respostas de domínio e seus códigos de status.

O módulo de telas **não tem regra de negócio nenhuma**: ele consulta os mesmos serviços que a API REST
consulta e decide apenas o que mostrar. É a camada de serviço pagando o que prometeu — uma segunda
apresentação sobre o mesmo domínio custou controllers e objetos de saída, e nenhuma regra duplicada.

O fluxo tem quatro passos, e o aplicativo caminha por ele sem conhecer nenhuma regra:

1. `GET /api/v1/screens/topics` — `SELECAO` com as pautas em votação;
2. `POST /api/v1/screens/topics/{id}/identify` — `FORMULARIO` pedindo o CPF;
3. `POST /api/v1/screens/topics/{id}/choose` — `SELECAO` com "Sim" e "Não";
4. `POST /api/v1/screens/topics/{id}/votes` — registra e devolve a confirmação.

### Nomes de campo em português, código em inglês

Os campos que vão no fio são `tipo`, `titulo`, `itens`, `texto`, `valor`, `botaoOk`, `botaoCancelar`. Não é
escolha nossa: é o contrato que o aplicativo já entende, e renomear qualquer um deles quebraria todas as
cópias instaladas.

O código continua em inglês, como todo o resto do projeto, e `@JsonProperty` faz a ponte. A alternativa
seria escrever os identificadores Java em português, o que espalharia a exceção por todo o módulo em vez de
concentrá-la em uma anotação por campo.

### O domínio da URL é configurável

Os botões levam URLs absolutas, e o endereço base vem de `voting.screens.base-url`. Isso atende uma dica
explícita do enunciado: o emulador e um aparelho físico não alcançam o servidor pelo mesmo endereço, e sem
configuração seria preciso recompilar para testar nos dois.

### Voto recusado também é tela

Este cliente não sabe desenhar um documento de erro: ele desenha telas. Um voto recusado volta como
`FORMULARIO` com o motivo em texto, e não como 409 ou 422.

A tradução acontece **só para as recusas que o domínio declara** — associado que já votou, pauta que não
aceita votos, serviço de elegibilidade indisponível. Qualquer outra exceção continua subindo e virando 500:
defeito não pode ser mostrado ao associado como se fosse resposta esperada.

O texto mostrado é escrito em português no módulo de telas, e não copiado da mensagem da exceção. As
mensagens de domínio são em inglês, como todo o código, e nunca foram feitas para serem lidas por um
associado.

## Consequências

- Três tipos do domínio de votos passaram a ser públicos para que este módulo os usasse. É o preço de ter
  duas apresentações, e é pequeno: continuam sendo o mesmo serviço e as mesmas regras.
- Mensagens de recusa mais específicas — distinguir "sessão fechada" de "associado não habilitado" —
  exigiriam códigos de erro nas exceções de domínio. Não foram criados porque nada no enunciado depende
  dessa distinção; se passar a depender, é aí que entram.
- Os tipos de campo `INPUT_NUMERO` e `INPUT_DATA` estão declarados no modelo, porque fazem parte do contrato
  do anexo, mas nenhuma tela deste fluxo precisa deles.
