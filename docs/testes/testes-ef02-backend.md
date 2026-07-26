# EF-02 — Testes TDD do backend

Implementar um teste por ciclo vermelho/verde. Controllers usam Service mockado; Services usam Repositories
mockados; persistência e HTTP real ficam restritos aos testes de integração.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-LIS-01` | Unitário | Contrato > `GET /lists` | Controller valida sessão/query e devolve coleção, resumo e status `200`. |
| `BE-LIS-02` | Unitário | Requisitos > visibilidade | Service lista apenas recursos não excluídos em que o usuário é proprietário ou participante. |
| `BE-LIS-03` | Unitário | Contrato > Consulta da coleção | Repository combina estado/pesquisa normalizada, limita a 30 e ordena por `updatedAt desc, id asc`. |
| `BE-LIS-04` | Unitário | Contrato > cursor e resumo | Cursor rejeita filtros incompatíveis, páginas não repetem itens e resumo ignora filtro/paginação. |
| `BE-LIS-05` | Unitário | Contrato > `POST /lists` | Controller exige CSRF/chave, valida schema e responde `201`, `Location` e `ETag`. |
| `BE-LIS-06` | Unitário | Requisitos > Nova lista | Service cria lista ativa, vazia e do usuário, normaliza/valida nome e valida descrição nos limites. |
| `BE-LIS-07` | Unitário | Requisitos > unicidade | Aceita nome de lista alheia compartilhada e rejeita equivalente entre listas próprias não excluídas. |
| `BE-LIS-08` | Unitário | Requisitos não funcionais > idempotência | Mesma chave/carga retorna a criação original; reutilização incompatível retorna código normativo. |
| `BE-LIS-09` | Unitário | Contrato > `GET /lists/{id}` | Retorna detalhe acessível com `ETag`; ausente, excluído, alheio ou inacessível retorna `NOT_FOUND`. |
| `BE-LIS-10` | Unitário | Contrato > `PATCH /lists/{id}` | Aceita só nome/descrição, exige `If-Match`, rejeita corpo vazio/desconhecido e gera novo `ETag`. |
| `BE-LIS-11` | Unitário | Requisitos > Editar lista | Service permite somente proprietário de ativa, ignora o próprio nome e aceita `description=null`. |
| `BE-LIS-12` | Unitário | Requisitos > Editar > sucesso | Mudança atualiza somente campos enviados, `updatedAt` e versão; ausência de mudança é rejeitada. |
| `BE-LIS-13` | Unitário | Requisitos > Editar > concorrência | Versão antiga não sobrescreve dados e retorna `CONFLICT` com `ETag` atual. |
| `BE-LIS-14` | Unitário | Contrato > Erros | Mapeia validação, duplicidade, proibição, concluída, conflito e não encontrado com payload normativo. |
| `BE-LIS-15` | Unitário | Requisitos não funcionais > arquitetura | Teste de arquitetura garante pacotes e dependências Controller → Service → Repository. |
| `BE-LIS-16` | Integração | Testes de validação `LIST-001` a `LIST-012` | Integração entre Controller, Service e Repository, com banco substituído, comprova persistência, isolamento, paginação, concorrência e atomicidade. |

