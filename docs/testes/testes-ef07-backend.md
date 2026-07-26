# EF-07 — Testes TDD do backend

Cada linha é um ciclo TDD independente; relógio, repositórios e eventos são substituídos nos unitários.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-LIFE-01` | Contrato > `PUT .../status` | Exige sessão/CSRF/proprietário/versão/chave, valida único campo e retorna detalhe/`ETag`. |
| `BE-LIFE-02` | Requisitos > concluir | Conclui ativa vazia/parcial/completa, define data do servidor e preserva relações e itens. |
| `BE-LIFE-03` | Requisitos > reabrir | Reabre concluída, remove data e preserva integralmente conteúdo, marcações, pessoas e convites. |
| `BE-LIFE-04` | Contrato > transições | Estado desejado já atual ou transição inválida retorna `INVALID_LIST_TRANSITION` sem evento. |
| `BE-LIFE-05` | Requisitos > somente leitura | Toda mutação de lista concluída é recusada, inclusive aceite de convite, sem alterar dados. |
| `BE-LIFE-06` | Contrato > `DELETE /lists/{id}` | Exclui logicamente, revoga membros/convites e invalida acesso/endereço. |
| `BE-LIFE-07` | Requisitos > preservação | Exclusão mantém dados históricos internos, mas nenhuma consulta normal os revela. |
| `BE-LIFE-08` | Requisitos não funcionais > atomicidade | Estado/exclusão, relações e eventos confirmam juntos; falha reverte tudo. |
| `BE-LIFE-09` | Requisitos não funcionais > idempotência | Mesma chave repete resultado sem versão/evento; nova exclusão posterior retorna `NOT_FOUND`. |
| `BE-LIFE-10` | Contrato > concorrência/autorização | Versão antiga conflita; participante recebe `FORBIDDEN`; alheio/inexistente recebe `NOT_FOUND`. |
| `BE-LIFE-11` | Requisitos > clientes conectados | Evento pós-commit contém versão/estado mínimo para somente leitura, reabertura ou redirecionamento. |
| `BE-LIFE-12` | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e publicador isolado. |
| `BE-LIFE-13` | Testes de validação `LIFE-001` a `LIFE-010` | Integração HTTP/banco/eventos cobre transições, histórico, revogação, idempotência e concorrência. |

