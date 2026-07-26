# EF-02 — Testes TDD do frontend

Implementar um teste por ciclo vermelho/verde, na ordem da tabela. Componentes usam serviço substituído por
stub/spy; testes HTTP pertencem ao serviço.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `FE-LIS-01` | Requisitos > Minhas listas | Renderiza título, totais, pesquisa, filtros, cartões e “Nova lista”. |
| `FE-LIS-02` | Requisitos > Minhas listas > ordenação e paginação | Exibe páginas recebidas sem duplicar cartões e preserva a ordem do serviço. |
| `FE-LIS-03` | Requisitos > Cabeçalho | Mostra totais de listas ativas e itens pendentes retornados no resumo, independentemente do filtro. |
| `FE-LIS-04` | Requisitos > Pesquisar listas | Normaliza espaços, limita a 60 caracteres, combina pesquisa e filtro e permite limpar pesquisa vazia. |
| `FE-LIS-05` | Requisitos > Filtros | Inicia em “Ativas”, alterna para concluídas/todas e apresenta o estado vazio específico de cada filtro. |
| `FE-LIS-06` | Requisitos > Conjunto de cartões | Exibe dados, papel, totais e percentual e apresenta o estado sem listas com “Criar lista”. |
| `FE-LIS-07` | Requisitos > Cartão > Abrir/Editar | Abre lista no modo correto, restringe “Editar” ao proprietário de ativa e trata indisponibilidade. |
| `FE-LIS-08` | Requisitos > Nova lista > campos | Valida nome nos limites, unicidade retornada pelo serviço e descrição até 240, com mensagens normativas. |
| `FE-LIS-09` | Requisitos > Nova lista > Salvar/Cancelar | Bloqueia reenvio, envia valores normalizados, navega ao detalhe no sucesso e preserva formulário no erro. |
| `FE-LIS-10` | Requisitos > Editar lista | Carrega dados, permite remover descrição, desabilita salvamento sem mudança e restringe acesso/papel/estado. |
| `FE-LIS-11` | Requisitos > Editar > Salvar | Envia somente mudanças, bloqueia reenvio, navega no sucesso e preserva formulário no erro. |
| `FE-LIS-12` | Requisitos > Editar > conflito | Exibe mensagem de conflito e “Recarregar dados”, sem substituir a versão mais recente. |
| `FE-LIS-13` | Contrato de API > Endpoints e erros | Serviço usa rotas, métodos, query, corpos e cabeçalhos definidos e traduz os códigos de erro para a UI. |
| `FE-LIS-14` | Requisitos não funcionais > comunicação HTTP | Componentes interagem apenas com o serviço injetado, sem criar cliente ou conhecer detalhes HTTP. |
| `FE-LIS-15` | Requisitos não funcionais > acessibilidade | Campos têm nomes/erros associados; estados e mensagens são anunciados e ações funcionam por teclado. |
| `FE-LIS-16` | Testes de validação `LIST-001` a `LIST-012` | Playwright cobre criação, edição, busca/filtros, paginação, papéis, conflitos, cancelamento e indisponibilidade. |

