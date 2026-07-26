# EF-04 — Testes TDD do backend

Cada linha deve falhar antes da menor implementação necessária. Colaboradores são mocks nos testes unitários.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-PROD-01` | Contrato > `GET /products` | Controller valida filtros/status/limite e retorna coleção paginada do usuário. |
| `BE-PROD-02` | Requisitos > listagem/pesquisa | Repository filtra ativos por padrão, combina busca/categoria e aplica ordenação estável `pt-BR`. |
| `BE-PROD-03` | Requisitos > seleção | Busca limita dez ativos e prioriza correspondência exata, início, ocorrência, nome e `id`. |
| `BE-PROD-04` | Contrato > cursor | Cursor incorpora filtros/ordem e rejeita uso incompatível sem omitir/repetir produtos. |
| `BE-PROD-05` | Contrato > `POST /products` | Exige sessão/CSRF/chave, deriva usuário/ícone/ativo e retorna `201`, `Location`, `ETag`. |
| `BE-PROD-06` | Requisitos > validações | Valida nome/unidade/categoria, rejeita duplicado ativo normalizado e permite homônimo inativo. |
| `BE-PROD-07` | Requisitos não funcionais > idempotência | Mesma chave/carga não duplica e reutilização incompatível retorna código normativo. |
| `BE-PROD-08` | Contrato > `PATCH /products/{id}` | Aceita somente campos editáveis, exige versão/mudança e impede edição de inativo. |
| `BE-PROD-09` | Requisitos > edição e histórico | Atualiza catálogo para uso futuro e preserva integralmente snapshots de itens existentes. |
| `BE-PROD-10` | Contrato > categoria | Categoria alheia/inexistente retorna `NOT_FOUND`; excluída retorna `CATEGORY_UNAVAILABLE`. |
| `BE-PROD-11` | Contrato > `DELETE /products/{id}` | Define inativo/incrementa versão, preserva itens e repete desativação com `204` sem nova mudança. |
| `BE-PROD-12` | Requisitos não funcionais > atomicidade | Falha em criação/edição/desativação não deixa catálogo ou histórico parcialmente alterado. |
| `BE-PROD-13` | Contrato > concorrência/isolamento | Versão antiga conflita; ID alheio retorna `NOT_FOUND`; nenhum dado é sobrescrito. |
| `BE-PROD-14` | Contrato > schemas/erros | Serializa referência disponível/inativa, datas/versão e todos os erros normativos. |
| `BE-PROD-15` | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository em pacote de produtos. |
| `BE-PROD-16` | Testes de validação `PROD-001` a `PROD-010` | Integração HTTP/banco cobre CRUD, filtros, snapshots, idempotência, isolamento e conflito. |

