# EF-03 — Testes TDD do frontend

Cada linha representa um ciclo vermelho/verde independente. Componentes recebem o serviço como stub/spy.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-CAT-01` | Unitário | Requisitos > Tela Categorias | Renderiza título, pesquisa, “Nova categoria”, lista ordenada e dados de cada categoria. |
| `FE-CAT-02` | Unitário | Requisitos > categorias iniciais | Apresenta as quatro categorias iniciais com ícones e contagem zero para conta recém-criada. |
| `FE-CAT-03` | Unitário | Requisitos > Pesquisa | Normaliza/limita a 40, pesquisa sem caixa/acento e exibe estado vazio com ação de limpar. |
| `FE-CAT-04` | Unitário | Requisitos > Categoria > ações | Abre edição preenchida e exclusão apenas com as informações e condições especificadas. |
| `FE-CAT-05` | Unitário | Requisitos > Novo/Editar > Nome | Valida vazio, 40 caracteres e duplicidade normalizada com mensagens normativas. |
| `FE-CAT-06` | Unitário | Requisitos > Novo/Editar > Ícone | Exibe somente ícones permitidos, exige seleção e informa valor inválido. |
| `FE-CAT-07` | Unitário | Requisitos > Novo > Salvar | Bloqueia reenvio, inclui na ordem no sucesso e mantém diálogo/campos no erro. |
| `FE-CAT-08` | Unitário | Requisitos > Editar > Salvar | Desabilita sem mudança, atualiza lista/produtos refletidos no sucesso e trata erro preservando campos. |
| `FE-CAT-09` | Unitário | Requisitos > Editar > conflito | Informa conflito e oferece recarregar sem sobrescrever dados recentes. |
| `FE-CAT-10` | Unitário | Requisitos > Excluir | Exibe confirmação; bloqueio informa quantidade de produtos; sucesso remove categoria das seleções. |
| `FE-CAT-11` | Unitário | Requisitos > Cancelar e `Esc` | Fecha sem mutação e devolve foco ao acionador em criação, edição e exclusão. |
| `FE-CAT-12` | Unitário | Contrato de API | Serviço implementa métodos, rotas, query, `Idempotency-Key`, `If-Match`, `ETag` e tradução de erros. |
| `FE-CAT-13` | Unitário | Requisitos não funcionais > comunicação | Componentes não conhecem HTTP e chamam somente métodos do serviço injetado. |
| `FE-CAT-14` | Unitário | Requisitos não funcionais > acessibilidade | Diálogos gerenciam foco; campos, erros, mensagens e ações têm semântica acessível e teclado. |
| `FE-CAT-15` | E2E | Testes de validação `CAT-001` a `CAT-010` | Playwright cobre iniciais, CRUD, busca, propagação, histórico, isolamento e conflito. |
| `FE-CAT-16` | Integração | Requisitos não funcionais > rota | `RouterTestingHarness` confirma que `/categorias` renderiza a tela para usuário autenticado e redireciona visitante ao login preservando o destino. |
