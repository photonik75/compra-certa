# EF-02 — Testes TDD do frontend

Implementar um teste por ciclo vermelho/verde, na ordem da tabela. Componentes usam serviço substituído por
stub/spy; testes HTTP pertencem ao serviço.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-LIS-01` | Unitário | Requisitos > Minhas listas | Renderiza título, totais, pesquisa, filtros, cartões e “Nova lista”. |
| `FE-LIS-02` | Unitário | Requisitos > Minhas listas > ordenação e paginação | Exibe páginas recebidas sem duplicar cartões e preserva a ordem do serviço. |
| `FE-LIS-03` | Unitário | Requisitos > Cabeçalho | Mostra totais de listas ativas e itens pendentes retornados no resumo, independentemente do filtro. |
| `FE-LIS-04` | Unitário | Requisitos > Pesquisar listas | Normaliza espaços, limita a 60 caracteres, combina pesquisa e filtro e permite limpar pesquisa vazia. |
| `FE-LIS-05` | Unitário | Requisitos > Filtros | Inicia em “Ativas”, alterna para concluídas/todas e apresenta o estado vazio específico de cada filtro. |
| `FE-LIS-06` | Unitário | Requisitos > Conjunto de cartões | Exibe dados, papel, totais e percentual e apresenta o estado sem listas com “Criar lista”. |
| `FE-LIS-07` | Unitário | Requisitos > Cartão > Abrir/Editar | Abre lista no modo correto, restringe “Editar” ao proprietário de ativa e trata indisponibilidade. |
| `FE-LIS-08` | Unitário | Requisitos > Nova lista > campos | Valida nome nos limites, unicidade retornada pelo serviço e descrição até 240, com mensagens normativas. |
| `FE-LIS-09` | Unitário | Requisitos > Nova lista > Salvar/Cancelar | Bloqueia reenvio, envia valores normalizados, navega ao detalhe no sucesso e preserva formulário no erro. |
| `FE-LIS-10` | Unitário | Requisitos > Editar lista | Carrega dados, permite remover descrição, desabilita salvamento sem mudança e restringe acesso/papel/estado. |
| `FE-LIS-11` | Unitário | Requisitos > Editar > Salvar | Envia somente mudanças, bloqueia reenvio, navega no sucesso e preserva formulário no erro. |
| `FE-LIS-12` | Unitário | Requisitos > Editar > conflito | Exibe mensagem de conflito e “Recarregar dados”, sem substituir a versão mais recente. |
| `FE-LIS-13` | Unitário | Contrato de API > Endpoints e erros | Serviço usa rotas, métodos, query, corpos e cabeçalhos definidos e traduz os códigos de erro para a UI. |
| `FE-LIS-14` | Unitário | Requisitos não funcionais > comunicação HTTP | Componentes interagem apenas com o serviço injetado, sem criar cliente ou conhecer detalhes HTTP. |
| `FE-LIS-15` | Unitário | Requisitos não funcionais > acessibilidade | Campos têm nomes/erros associados; estados e mensagens são anunciados e ações funcionam por teclado. |
| `FE-LIS-16` | E2E | Testes de validação `LIST-001` a `LIST-012` | Playwright cobre criação, edição, busca/filtros, paginação, papéis, conflitos, cancelamento e indisponibilidade. |
| `FE-LIS-17` | Integração | Requisitos não funcionais > rotas | `RouterTestingHarness` valida `/listas`, `/listas/nova`, `/listas/:listId` e `/listas/:listId/editar`, seus guards e parâmetros inválidos/inacessíveis. |
