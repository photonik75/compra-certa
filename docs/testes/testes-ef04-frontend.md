# EF-04 — Testes TDD do frontend

Executar cada linha como ciclo vermelho/verde. Componentes usam serviço substituído; o serviço é testado com
controlador HTTP de teste.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `FE-PROD-01` | Requisitos > Tela Produtos | Renderiza ativos ordenados, cabeçalho, pesquisa, categoria, dados e aviso histórico. |
| `FE-PROD-02` | Requisitos > Pesquisa e filtro | Normaliza/limita busca, combina categoria, distingue vazio de catálogo/pesquisa/filtros e limpa critérios. |
| `FE-PROD-03` | Requisitos > Produto > ações | Mostra editar/desativar só para ativo e abre diálogos com o produto correto. |
| `FE-PROD-04` | Requisitos > Novo/Editar > Nome | Valida limites e duplicidade normalizada, aceitando reutilização de nome inativo. |
| `FE-PROD-05` | Requisitos > Categoria padrão | Lista apenas categorias disponíveis, exige seleção e trata categoria tornada indisponível. |
| `FE-PROD-06` | Requisitos > Unidade padrão | Exibe o enum traduzido, exige uma opção e rejeita valor não permitido. |
| `FE-PROD-07` | Requisitos > Novo > Salvar | Bloqueia reenvio, fecha/inclui/avisa no sucesso e preserva diálogo no erro. |
| `FE-PROD-08` | Requisitos > Editar > Salvar | Desabilita sem mudança, salva alterações futuras, atualiza lista e preserva snapshots existentes. |
| `FE-PROD-09` | Requisitos > Editar > conflito | Informa conflito, mantém dados locais e oferece recarregar dados atuais. |
| `FE-PROD-10` | Requisitos > Desativar | Confirma nome/efeito; sucesso remove ativos/seleções e erro preserva produto. |
| `FE-PROD-11` | Requisitos > Seleção de produto | Sugere no máximo dez ativos, com dados completos e ordem exata/início/ocorrência. |
| `FE-PROD-12` | Requisitos > Cancelar e `Esc` | Fecha diálogos sem mutação e devolve foco ao acionador correspondente. |
| `FE-PROD-13` | Contrato de API | Serviço cobre rotas, query, corpos, cabeçalhos, schemas e tradução de todos os erros. |
| `FE-PROD-14` | Requisitos não funcionais > comunicação | Componentes usam somente o serviço e não conhecem URLs, status ou cliente HTTP. |
| `FE-PROD-15` | Requisitos não funcionais > acessibilidade | Formulários/diálogos têm foco, nomes, erros e anúncios acessíveis e operam por teclado. |
| `FE-PROD-16` | Testes de validação `PROD-001` a `PROD-010` | Playwright cobre CRUD, filtros, snapshots, desativação, isolamento, conflito e foco. |

