# EF-04 — Testes TDD do frontend

Executar cada linha como ciclo vermelho/verde. Componentes usam serviço substituído; o serviço é testado com
controlador HTTP de teste.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-PROD-01` | Unitário | Requisitos > Tela Produtos | Renderiza ativos ordenados, cabeçalho, pesquisa, categoria, dados e aviso histórico. |
| `FE-PROD-02` | Unitário | Requisitos > Pesquisa e filtro | Normaliza/limita busca, combina categoria, distingue vazio de catálogo/pesquisa/filtros e limpa critérios. |
| `FE-PROD-03` | Unitário | Requisitos > Produto > ações | Mostra editar/desativar só para ativo e abre diálogos com o produto correto. |
| `FE-PROD-04` | Unitário | Requisitos > Novo/Editar > Nome | Valida limites e duplicidade normalizada, aceitando reutilização de nome inativo. |
| `FE-PROD-05` | Unitário | Requisitos > Categoria padrão | Lista apenas categorias disponíveis, exige seleção e trata categoria tornada indisponível. |
| `FE-PROD-06` | Unitário | Requisitos > Unidade padrão | Exibe o enum traduzido, exige uma opção e rejeita valor não permitido. |
| `FE-PROD-07` | Unitário | Requisitos > Novo > Salvar | Bloqueia reenvio, fecha/inclui/avisa no sucesso e preserva diálogo no erro. |
| `FE-PROD-08` | Unitário | Requisitos > Editar > Salvar | Desabilita sem mudança, salva alterações futuras, atualiza lista e preserva snapshots existentes. |
| `FE-PROD-09` | Unitário | Requisitos > Editar > conflito | Informa conflito, mantém dados locais e oferece recarregar dados atuais. |
| `FE-PROD-10` | Unitário | Requisitos > Desativar | Confirma nome/efeito; sucesso remove ativos/seleções e erro preserva produto. |
| `FE-PROD-11` | Unitário | Requisitos > Seleção de produto | Sugere no máximo dez ativos, com dados completos e ordem exata/início/ocorrência. |
| `FE-PROD-12` | Unitário | Requisitos > Cancelar e `Esc` | Fecha diálogos sem mutação e devolve foco ao acionador correspondente. |
| `FE-PROD-13` | Unitário | Contrato de API | Serviço cobre rotas, query, corpos, cabeçalhos, schemas e tradução de todos os erros. |
| `FE-PROD-14` | Unitário | Requisitos não funcionais > comunicação | Componentes usam somente o serviço e não conhecem URLs, status ou cliente HTTP. |
| `FE-PROD-15` | Unitário | Requisitos não funcionais > acessibilidade | Formulários/diálogos têm foco, nomes, erros e anúncios acessíveis e operam por teclado. |
| `FE-PROD-16` | E2E | Testes de validação `PROD-001` a `PROD-010` | Playwright cobre CRUD, filtros, snapshots, desativação, isolamento, conflito e foco. |
| `FE-PROD-17` | Integração | Requisitos não funcionais > rota | `RouterTestingHarness` confirma que `/produtos` renderiza a tela para usuário autenticado e redireciona visitante ao login preservando o destino. |
