# EF-08 — Testes TDD do backend

Cada linha deve motivar apenas a menor produção necessária. E-mail, relógio, tokens e eventos são dublês nos
testes unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

Critério de validação: testes de comportamento devem executar a operação e observar resultado e efeitos
colaterais. Inspeção de código, SQL, anotações ou arquitetura não substitui a simulação de sucesso, falha,
concorrência e rollback; essas inspeções são aceitas somente no teste arquitetural.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-SHARE-01` | Unitário | Contrato > `GET .../access` | Retorna acesso ordenado apenas a membro atual, com `Cache-Control: no-store` e sem token. |
| `BE-SHARE-02` | Unitário | Contrato > criar convite | Exige proprietário de ativa, CSRF/chave e e-mail válido/minúsculo; persiste antes de enviar. |
| `BE-SHARE-03` | Unitário | Requisitos > conta existente/inexistente | Conta ativa cria vínculo/aviso; inexistente cria convite único de sete dias/link de uso único. |
| `BE-SHARE-04` | Unitário | Requisitos > conflitos de convite | Rejeita proprietário, membro e pendente com códigos específicos, sem efeito adicional. |
| `BE-SHARE-05` | Unitário | Requisitos > falha de entrega/reenvio | Mantém pendente na falha; reenvio invalida token, cria outro e reinicia validade. |
| `BE-SHARE-06` | Unitário | Contrato > cancelar convite | Exige papel/versão, invalida token, remove da visão padrão e publica mudança após commit. |
| `BE-SHARE-07` | Unitário | Contrato > preview | Token válido retorna apenas preview; inválido/usado/cancelado e expirado retornam códigos normativos. |
| `BE-SHARE-08` | Unitário | Contrato > aceitar | Exige e-mail exato/lista ativa, cria um vínculo e só então consome token; repetição não duplica. |
| `BE-SHARE-09` | Unitário | Contrato > remover membro | Proprietário remove participante, preserva autoria e impede remover proprietário/alheio. |
| `BE-SHARE-10` | Unitário | Contrato > sair | Participante remove o próprio vínculo; proprietário não sai; itens/autoria permanecem. |
| `BE-SHARE-11` | Unitário | Requisitos > permissões | Service revalida papel em toda mutação e aplica matriz proprietário/participante/concluída. |
| `BE-SHARE-12` | Unitário | Requisitos não funcionais > tokens | Token é imprevisível, hash não reversível, ausente de logs/respostas/eventos e enviado no fragmento. |
| `BE-SHARE-13` | Unitário | Requisitos não funcionais > atomicidade/idempotência | Vínculo/token/evento confirmam juntos; repetição não duplica; falha reverte a transação. |
| `BE-SHARE-14` | Unitário | Requisitos > eventos | Publica dados mínimos pós-commit e revogação impede imediatamente novas consultas/escritas. |
| `BE-SHARE-15` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e adaptadores isolados. |
| `BE-SHARE-16` | Integração | Testes de validação `SHARE-001` a `SHARE-014` | Integração entre camadas HTTP, domínio, e-mail e eventos, com sistemas externos substituídos, cobre fluxos, privacidade, revogação e concorrência. |

