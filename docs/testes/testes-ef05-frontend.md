# EF-05 — Testes TDD do frontend

Implementar um teste por ciclo vermelho/verde. Componentes recebem serviços falsos; detalhes HTTP são
verificados somente nos testes dos serviços.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-ITEM-01` | Unitário | Requisitos > Detalhe da lista | Agrupa por categoria normalizada, ordena grupos/itens e exibe contagem e snapshots. |
| `FE-ITEM-02` | Unitário | Requisitos > permissões/estado | Habilita gestão para proprietário/participante de ativa e mantém concluída somente para consulta. |
| `FE-ITEM-03` | Unitário | Requisitos > Produto | Sugere até dez ativos desde o primeiro caractere e exige seleção válida. |
| `FE-ITEM-04` | Unitário | Requisitos > Cadastrar produto | Ao clicar em “Cadastrar novo produto”, abre o cadastro transportando o rascunho do item; no retorno bem-sucedido, restaura quantidade e observação, seleciona o produto criado e aplica sua unidade e categoria padrão. |
| `FE-ITEM-05` | Unitário | Requisitos > Quantidade | Valida vazio, zero, negativo e máximo; formata vírgula e remove zeros desnecessários. |
| `FE-ITEM-06` | Unitário | Requisitos > Unidade/Categoria/Observação | Exibe unidades e categorias disponíveis com rótulos legíveis, mantendo códigos e identificadores apenas como valores internos; aplica disponibilidade, limite de 240 e mensagens normativas. |
| `FE-ITEM-07` | Unitário | Requisitos > Adicionar item | Bloqueia reenvio, envia snapshots, volta/destaca no sucesso e preserva formulário no erro. |
| `FE-ITEM-08` | Unitário | Requisitos > Produto já está na lista | Exibe dados existentes; cancelar preserva; editar abre destino; soma depende de unidade/limite. |
| `FE-ITEM-09` | Unitário | Requisitos > Editar item | Carrega campos, permite todas as alterações, preserva marcação e desabilita salvar sem mudança. |
| `FE-ITEM-10` | Unitário | Requisitos > Editar > duplicidade/conflito | Trata mesclagem sem item extra e conflito sem sobrescrever, oferecendo recarga. |
| `FE-ITEM-11` | Unitário | Requisitos > Remover item | Confirma produto, atualiza lista/resumo no sucesso e preserva item com mensagem no erro. |
| `FE-ITEM-12` | Unitário | Requisitos > Cancelar | Inclusão/edição/remoção retornam ou fecham sem persistir alteração. |
| `FE-ITEM-13` | Unitário | Contrato de API | Serviço cobre endpoints, headers, decimal string, schemas, `201/200` e tradução de erros. |
| `FE-ITEM-14` | Unitário | Requisitos não funcionais > comunicação | Componentes usam somente serviços injetados, sem acesso direto ao servidor. |
| `FE-ITEM-15` | Unitário | Requisitos não funcionais > acessibilidade | Campos/erros/diálogos/destaque têm semântica e foco acessíveis e funcionam por teclado. |
| `FE-ITEM-16` | E2E | Testes de validação da EF-05 | Playwright cobre inclusão, edição, duplicidade, soma, remoção, snapshots, papéis e conflito. |
| `FE-ITEM-17` | Integração | Requisitos não funcionais > rotas | `RouterTestingHarness` valida `/listas/:listId`, `/listas/:listId/itens/novo` e `/listas/:listId/itens/:itemId/editar`, com sessão, acesso e parâmetros inválidos. |
