# EF-02 — Testes TDD do backend

Implementar um teste por ciclo vermelho/verde. Controllers usam Service mockado; Services usam Repositories
mockados; persistência e HTTP real ficam restritos aos testes de integração.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-LIS-01` | Contrato > `GET /lists` | Controller valida sessão/query e devolve coleção, resumo e status `200`. |
| `BE-LIS-02` | Requisitos > visibilidade | Service lista apenas recursos não excluídos em que o usuário é proprietário ou participante. |
| `BE-LIS-03` | Contrato > Consulta da coleção | Repository combina estado/pesquisa normalizada, limita a 30 e ordena por `updatedAt desc, id asc`. |
| `BE-LIS-04` | Contrato > cursor e resumo | Cursor rejeita filtros incompatíveis, páginas não repetem itens e resumo ignora filtro/paginação. |
| `BE-LIS-05` | Contrato > `POST /lists` | Controller exige CSRF/chave, valida schema e responde `201`, `Location` e `ETag`. |
| `BE-LIS-06` | Requisitos > Nova lista | Service cria lista ativa, vazia e do usuário, normaliza/valida nome e valida descrição nos limites. |
| `BE-LIS-07` | Requisitos > unicidade | Aceita nome de lista alheia compartilhada e rejeita equivalente entre listas próprias não excluídas. |
| `BE-LIS-08` | Requisitos não funcionais > idempotência | Mesma chave/carga retorna a criação original; reutilização incompatível retorna código normativo. |
| `BE-LIS-09` | Contrato > `GET /lists/{id}` | Retorna detalhe acessível com `ETag`; ausente, excluído, alheio ou inacessível retorna `NOT_FOUND`. |
| `BE-LIS-10` | Contrato > `PATCH /lists/{id}` | Aceita só nome/descrição, exige `If-Match`, rejeita corpo vazio/desconhecido e gera novo `ETag`. |
| `BE-LIS-11` | Requisitos > Editar lista | Service permite somente proprietário de ativa, ignora o próprio nome e aceita `description=null`. |
| `BE-LIS-12` | Requisitos > Editar > sucesso | Mudança atualiza somente campos enviados, `updatedAt` e versão; ausência de mudança é rejeitada. |
| `BE-LIS-13` | Requisitos > Editar > concorrência | Versão antiga não sobrescreve dados e retorna `CONFLICT` com `ETag` atual. |
| `BE-LIS-14` | Contrato > Erros | Mapeia validação, duplicidade, proibição, concluída, conflito e não encontrado com payload normativo. |
| `BE-LIS-15` | Requisitos não funcionais > arquitetura | Teste de arquitetura garante pacotes e dependências Controller → Service → Repository. |
| `BE-LIS-16` | Testes de validação `LIST-001` a `LIST-012` | Integração HTTP/banco comprova persistência, isolamento, paginação, concorrência e atomicidade. |

