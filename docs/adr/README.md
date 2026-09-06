# Decisões de arquitetura

Um arquivo por decisão, curto, explicando o contexto, o que foi decidido e o que foi descartado no caminho.
O enunciado avalia a "explicação breve do porquê das escolhas"; é aqui que ela está.

| ADR | Decisão | Em uma frase |
| --- | --- | --- |
| [0001](0001-camadas-por-dominio.md) | Camadas por domínio | `Controller → Service → Repository` agrupados por domínio, e camada só existe onde há responsabilidade. |
| [0002](0002-sessao-com-estado-derivado.md) | Sessão com estado derivado | Aberta ou fechada é calculado na leitura, sem coluna de status e sem agendador. |
| [0003](0003-unicidade-do-voto-no-banco.md) | Unicidade do voto no banco | Restrição `unique`, insere e captura a violação; consultar antes de inserir é corrida. |
| [0004](0004-apuracao-do-resultado.md) | Apuração do resultado | Contagem agregada no banco, disponível durante a votação com `votingOpen`. |
| [0005](0005-integracao-de-elegibilidade.md) | Integração de elegibilidade | Porta e adaptador só aqui, URL configurável, e falha do serviço vira 503 em vez de palpite. |
| [0006](0006-logs-e-correlacao-de-requisicoes.md) | Logs e correlação | Identificador por requisição em todas as linhas, e o corpo nunca é registrado. |
| [0007](0007-versionamento-da-api.md) | Versionamento da API | Versão no caminho, com a régua do que exige uma v2. |
| [0008](0008-desempenho-sob-carga.md) | Desempenho sob carga | Pool dimensionado por medição, e o que foi investigado sem precisar mudar. |
| [0009](0009-telas-do-aplicativo.md) | Telas do aplicativo | Contrato do anexo 1 como módulo de apresentação, sem regra de negócio. |
