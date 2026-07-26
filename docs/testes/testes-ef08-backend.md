# EF-08 — Testes TDD do backend

Cada linha deve motivar apenas a menor produção necessária. E-mail, relógio, tokens e eventos são dublês nos
testes unitários.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-SHARE-01` | Contrato > `GET .../access` | Retorna acesso ordenado apenas a membro atual, com `Cache-Control: no-store` e sem token. |
| `BE-SHARE-02` | Contrato > criar convite | Exige proprietário de ativa, CSRF/chave e e-mail válido/minúsculo; persiste antes de enviar. |
| `BE-SHARE-03` | Requisitos > conta existente/inexistente | Conta ativa cria vínculo/aviso; inexistente cria convite único de sete dias/link de uso único. |
| `BE-SHARE-04` | Requisitos > conflitos de convite | Rejeita proprietário, membro e pendente com códigos específicos, sem efeito adicional. |
| `BE-SHARE-05` | Requisitos > falha de entrega/reenvio | Mantém pendente na falha; reenvio invalida token, cria outro e reinicia validade. |
| `BE-SHARE-06` | Contrato > cancelar convite | Exige papel/versão, invalida token, remove da visão padrão e publica mudança após commit. |
| `BE-SHARE-07` | Contrato > preview | Token válido retorna apenas preview; inválido/usado/cancelado e expirado retornam códigos normativos. |
| `BE-SHARE-08` | Contrato > aceitar | Exige e-mail exato/lista ativa, cria um vínculo e só então consome token; repetição não duplica. |
| `BE-SHARE-09` | Contrato > remover membro | Proprietário remove participante, preserva autoria e impede remover proprietário/alheio. |
| `BE-SHARE-10` | Contrato > sair | Participante remove o próprio vínculo; proprietário não sai; itens/autoria permanecem. |
| `BE-SHARE-11` | Requisitos > permissões | Service revalida papel em toda mutação e aplica matriz proprietário/participante/concluída. |
| `BE-SHARE-12` | Requisitos não funcionais > tokens | Token é imprevisível, hash não reversível, ausente de logs/respostas/eventos e enviado no fragmento. |
| `BE-SHARE-13` | Requisitos não funcionais > atomicidade/idempotência | Vínculo/token/evento confirmam juntos; repetição não duplica; falha reverte a transação. |
| `BE-SHARE-14` | Requisitos > eventos | Publica dados mínimos pós-commit e revogação impede imediatamente novas consultas/escritas. |
| `BE-SHARE-15` | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e adaptadores isolados. |
| `BE-SHARE-16` | Testes de validação `SHARE-001` a `SHARE-014` | Integração HTTP/banco/e-mail/eventos cobre fluxos, privacidade, revogação e concorrência. |

