# EF-06 — Testes TDD do backend

Implementar um ciclo vermelho/verde por linha, com relógio e publicador de eventos substituídos nos unitários.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-SHOP-01` | Contrato > endpoint de marcação | Controller exige sessão, CSRF, `If-Match`, chave e corpo válido e retorna item/resumo/`ETag`. |
| `BE-SHOP-02` | Requisitos > autorização | Permite proprietário/participante de ativa; lista concluída rejeita e recurso alheio retorna indisponível. |
| `BE-SHOP-03` | Requisitos > marcar | Define comprado, autor e horário do servidor e incrementa a versão exatamente uma vez. |
| `BE-SHOP-04` | Requisitos > desmarcar | Remove marcação, autor e horário conforme contrato e incrementa versão uma vez. |
| `BE-SHOP-05` | Requisitos > resumo | Recalcula total/comprados/pendentes/percentual somente com itens não excluídos. |
| `BE-SHOP-06` | Requisitos não funcionais > atomicidade | Item, resumo e evento confirmam juntos; falha em qualquer etapa reverte tudo. |
| `BE-SHOP-07` | Requisitos > concorrência | `If-Match` antigo retorna estado/`ETag` atual sem sobrescrever a primeira marcação. |
| `BE-SHOP-08` | Requisitos não funcionais > idempotência | Mesma chave/carga retorna resultado original sem nova versão/evento; carga diferente conflita. |
| `BE-SHOP-09` | Requisitos > eventos | Publica apenas após commit, com lista/tipo/versão/dados mínimos e sem dados privados. |
| `BE-SHOP-10` | Requisitos > reconexão | Consulta de ressincronização retorna estado persistido completo e versão atual antes de nova escrita. |
| `BE-SHOP-11` | Contrato > validação/erros | Rejeita estado/campos inválidos e mapeia concluída, conflito, indisponível e validação. |
| `BE-SHOP-12` | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e adaptador de eventos isolado. |
| `BE-SHOP-13` | Testes de validação `SHOP-001` a `SHOP-011` | Integração HTTP/banco/eventos prova atomicidade, concorrência, isolamento e convergência do resumo. |

