# EF-07 — Testes TDD do backend

Cada linha é um ciclo TDD independente; relógio, repositórios e eventos são substituídos nos unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

Critério de validação: testes de comportamento devem executar a operação e observar resultado e efeitos
colaterais. Inspeção de código, SQL, anotações ou arquitetura não substitui a simulação de sucesso, falha,
concorrência e rollback; essas inspeções são aceitas somente no teste arquitetural.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-LIFE-01` | Unitário | Contrato > `PUT .../status` | Exige sessão/CSRF/proprietário/versão/chave, valida único campo e retorna detalhe/`ETag`. |
| `BE-LIFE-02` | Unitário | Requisitos > concluir | Conclui ativa vazia/parcial/completa, define data do servidor e preserva relações e itens. |
| `BE-LIFE-03` | Unitário | Requisitos > reabrir | Reabre concluída, remove data e preserva integralmente conteúdo, marcações, pessoas e convites. |
| `BE-LIFE-04` | Unitário | Contrato > transições | Estado desejado já atual ou transição inválida retorna `INVALID_LIST_TRANSITION` sem evento. |
| `BE-LIFE-05` | Unitário | Requisitos > somente leitura | Toda mutação de lista concluída é recusada, inclusive aceite de convite, sem alterar dados. |
| `BE-LIFE-06` | Unitário | Contrato > `DELETE /lists/{id}` | Exclui logicamente, revoga membros/convites e invalida acesso/endereço. |
| `BE-LIFE-07` | Unitário | Requisitos > preservação | Exclusão mantém dados históricos internos, mas nenhuma consulta normal os revela. |
| `BE-LIFE-08` | Unitário | Requisitos não funcionais > atomicidade | Estado/exclusão, relações e eventos confirmam juntos; falha reverte tudo. |
| `BE-LIFE-09` | Unitário | Requisitos não funcionais > idempotência | Mesma chave repete resultado sem versão/evento; nova exclusão posterior retorna `NOT_FOUND`. |
| `BE-LIFE-10` | Unitário | Contrato > concorrência/autorização | Versão antiga conflita; participante recebe `FORBIDDEN`; alheio/inexistente recebe `NOT_FOUND`. |
| `BE-LIFE-11` | Unitário | Requisitos > clientes conectados | Evento pós-commit contém versão/estado mínimo para somente leitura, reabertura ou redirecionamento. |
| `BE-LIFE-12` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e publicador isolado. |
| `BE-LIFE-13` | Integração | Testes de validação `LIFE-001` a `LIFE-010` | Integração entre camadas HTTP, domínio e eventos, com banco e broker substituídos, cobre transições, histórico, revogação, idempotência e concorrência. |

