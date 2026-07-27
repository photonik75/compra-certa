# EF-06 — Testes TDD do backend

Implementar um ciclo vermelho/verde por linha, com relógio e publicador de eventos substituídos nos unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

Critério de validação: testes de comportamento devem executar a operação e observar resultado e efeitos
colaterais. Inspeção de código, SQL, anotações ou arquitetura não substitui a simulação de sucesso, falha,
concorrência e rollback; essas inspeções são aceitas somente no teste arquitetural.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-SHOP-01` | Unitário | Contrato > endpoint de marcação | Controller exige sessão, CSRF, `If-Match`, chave e corpo válido e retorna item/resumo/`ETag`. |
| `BE-SHOP-02` | Unitário | Requisitos > autorização | Permite proprietário/participante de ativa; lista concluída rejeita e recurso alheio retorna indisponível. |
| `BE-SHOP-03` | Unitário | Requisitos > marcar | Define comprado, autor e horário do servidor e incrementa a versão exatamente uma vez. |
| `BE-SHOP-04` | Unitário | Requisitos > desmarcar | Remove marcação, autor e horário conforme contrato e incrementa versão uma vez. |
| `BE-SHOP-05` | Unitário | Requisitos > resumo | Recalcula total/comprados/pendentes/percentual somente com itens não excluídos. |
| `BE-SHOP-06` | Unitário | Requisitos não funcionais > atomicidade | Item, resumo e evento confirmam juntos; falha em qualquer etapa reverte tudo. |
| `BE-SHOP-07` | Unitário | Requisitos > concorrência | `If-Match` antigo retorna estado/`ETag` atual sem sobrescrever a primeira marcação. |
| `BE-SHOP-08` | Unitário | Requisitos não funcionais > idempotência | Mesma chave/carga retorna resultado original sem nova versão/evento; carga diferente conflita. |
| `BE-SHOP-09` | Unitário | Requisitos > eventos | Publica apenas após commit, com lista/tipo/versão/dados mínimos e sem dados privados. |
| `BE-SHOP-10` | Unitário | Requisitos > reconexão | Consulta de ressincronização retorna estado persistido completo e versão atual antes de nova escrita. |
| `BE-SHOP-11` | Unitário | Contrato > validação/erros | Rejeita estado/campos inválidos e mapeia concluída, conflito, indisponível e validação. |
| `BE-SHOP-12` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e adaptador de eventos isolado. |
| `BE-SHOP-13` | Integração | Testes de validação `SHOP-001` a `SHOP-011` | Integração entre camadas HTTP, domínio e eventos, com banco e broker substituídos, prova atomicidade, concorrência, isolamento e convergência do resumo. |

