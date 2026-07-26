# EF-05 — Testes TDD do frontend

Implementar um teste por ciclo vermelho/verde. Componentes recebem serviços falsos; detalhes HTTP são
verificados somente nos testes dos serviços.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `FE-ITEM-01` | Requisitos > Detalhe da lista | Agrupa por categoria normalizada, ordena grupos/itens e exibe contagem e snapshots. |
| `FE-ITEM-02` | Requisitos > permissões/estado | Habilita gestão para proprietário/participante de ativa e mantém concluída somente para consulta. |
| `FE-ITEM-03` | Requisitos > Produto | Sugere até dez ativos desde o primeiro caractere e exige seleção válida. |
| `FE-ITEM-04` | Requisitos > Cadastrar produto | Abre cadastro e, no retorno bem-sucedido, preserva campos e seleciona o produto criado. |
| `FE-ITEM-05` | Requisitos > Quantidade | Valida vazio, zero, negativo e máximo; formata vírgula e remove zeros desnecessários. |
| `FE-ITEM-06` | Requisitos > Unidade/Categoria/Observação | Aplica opções, disponibilidade e limite de 240 com mensagens normativas. |
| `FE-ITEM-07` | Requisitos > Adicionar item | Bloqueia reenvio, envia snapshots, volta/destaca no sucesso e preserva formulário no erro. |
| `FE-ITEM-08` | Requisitos > Produto já está na lista | Exibe dados existentes; cancelar preserva; editar abre destino; soma depende de unidade/limite. |
| `FE-ITEM-09` | Requisitos > Editar item | Carrega campos, permite todas as alterações, preserva marcação e desabilita salvar sem mudança. |
| `FE-ITEM-10` | Requisitos > Editar > duplicidade/conflito | Trata mesclagem sem item extra e conflito sem sobrescrever, oferecendo recarga. |
| `FE-ITEM-11` | Requisitos > Remover item | Confirma produto, atualiza lista/resumo no sucesso e preserva item com mensagem no erro. |
| `FE-ITEM-12` | Requisitos > Cancelar | Inclusão/edição/remoção retornam ou fecham sem persistir alteração. |
| `FE-ITEM-13` | Contrato de API | Serviço cobre endpoints, headers, decimal string, schemas, `201/200` e tradução de erros. |
| `FE-ITEM-14` | Requisitos não funcionais > comunicação | Componentes usam somente serviços injetados, sem acesso direto ao servidor. |
| `FE-ITEM-15` | Requisitos não funcionais > acessibilidade | Campos/erros/diálogos/destaque têm semântica e foco acessíveis e funcionam por teclado. |
| `FE-ITEM-16` | Testes de validação da EF-05 | Playwright cobre inclusão, edição, duplicidade, soma, remoção, snapshots, papéis e conflito. |

