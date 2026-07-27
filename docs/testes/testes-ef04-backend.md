# EF-04 — Testes TDD do backend

Cada linha deve falhar antes da menor implementação necessária. Colaboradores são mocks nos testes unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

Critério de validação: testes de comportamento devem executar a operação e observar resultado e efeitos
colaterais. Inspeção de código, SQL, anotações ou arquitetura não substitui a simulação de sucesso, falha,
concorrência e rollback; essas inspeções são aceitas somente no teste arquitetural.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-PROD-01` | Unitário | Contrato > `GET /products` | Controller valida filtros/status/limite e retorna coleção paginada do usuário. |
| `BE-PROD-02` | Unitário | Requisitos > listagem/pesquisa | Repository filtra ativos por padrão, combina busca/categoria e aplica ordenação estável `pt-BR`. |
| `BE-PROD-03` | Unitário | Requisitos > seleção | Busca limita dez ativos e prioriza correspondência exata, início, ocorrência, nome e `id`. |
| `BE-PROD-04` | Unitário | Contrato > cursor | Cursor incorpora filtros/ordem e rejeita uso incompatível sem omitir/repetir produtos. |
| `BE-PROD-05` | Unitário | Contrato > `POST /products` | Exige sessão/CSRF/chave, deriva usuário/ícone/ativo e retorna `201`, `Location`, `ETag`. |
| `BE-PROD-06` | Unitário | Requisitos > validações | Valida nome/unidade/categoria, rejeita duplicado ativo normalizado e permite homônimo inativo. |
| `BE-PROD-07` | Unitário | Requisitos não funcionais > idempotência | Mesma chave/carga não duplica e reutilização incompatível retorna código normativo. |
| `BE-PROD-08` | Unitário | Contrato > `PATCH /products/{id}` | Aceita somente campos editáveis, exige versão/mudança e impede edição de inativo. |
| `BE-PROD-09` | Unitário | Requisitos > edição e histórico | Atualiza catálogo para uso futuro e preserva integralmente snapshots de itens existentes. |
| `BE-PROD-10` | Unitário | Contrato > categoria | Categoria alheia/inexistente retorna `NOT_FOUND`; excluída retorna `CATEGORY_UNAVAILABLE`. |
| `BE-PROD-11` | Unitário | Contrato > `DELETE /products/{id}` | Define inativo/incrementa versão, preserva itens e repete desativação com `204` sem nova mudança. |
| `BE-PROD-12` | Unitário | Requisitos não funcionais > atomicidade | Falha em criação/edição/desativação não deixa catálogo ou histórico parcialmente alterado. |
| `BE-PROD-13` | Unitário | Contrato > concorrência/isolamento | Versão antiga conflita; ID alheio retorna `NOT_FOUND`; nenhum dado é sobrescrito. |
| `BE-PROD-14` | Unitário | Contrato > schemas/erros | Serializa referência disponível/inativa, datas/versão e todos os erros normativos. |
| `BE-PROD-15` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository em pacote de produtos. |
| `BE-PROD-16` | Integração | Testes de validação `PROD-001` a `PROD-010` | Integração entre Controller, Service e Repository, com banco substituído, cobre CRUD, filtros, snapshots, idempotência, isolamento e conflito. |

