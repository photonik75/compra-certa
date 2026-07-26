# EF-03 — Testes TDD do backend

Implementar na ordem, sempre iniciando pelo teste falho e isolando colaboradores nos testes unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-CAT-01` | Unitário | Requisitos > categorias iniciais | Criação de conta persiste exatamente as quatro categorias padrão, uma vez, com contagem zero. |
| `BE-CAT-02` | Unitário | Contrato > `GET /categories` | Controller valida query e retorna coleção paginada do usuário com `200`. |
| `BE-CAT-03` | Unitário | Requisitos > Pesquisa/ordenação | Repository ignora caixa/acento, limita pesquisa a 40 e ordena `pt-BR` com desempate por `id`. |
| `BE-CAT-04` | Unitário | Contrato > cursor | Cursor vincula pesquisa/ordenação e rejeita reutilização incompatível sem repetição entre páginas. |
| `BE-CAT-05` | Unitário | Contrato > `POST /categories` | Exige sessão/CSRF/chave, valida nome/ícone e retorna `201`, `Location` e `ETag`. |
| `BE-CAT-06` | Unitário | Requisitos > unicidade | Rejeita nome equivalente do mesmo usuário e permite o mesmo nome para usuários distintos. |
| `BE-CAT-07` | Unitário | Requisitos não funcionais > idempotência | Repete criação equivalente sem duplicar; chave reutilizada com outra carga retorna erro normativo. |
| `BE-CAT-08` | Unitário | Contrato > `PATCH /categories/{id}` | Aceita mudanças permitidas, exige versão e rejeita corpo vazio, nulo ou campo desconhecido. |
| `BE-CAT-09` | Unitário | Requisitos > propagação | Alteração incrementa categoria e produtos ativos associados; preserva inativos e snapshots de itens. |
| `BE-CAT-10` | Unitário | Requisitos não funcionais > atomicidade | Falha ao atualizar produto reverte toda a alteração de categoria e versões. |
| `BE-CAT-11` | Unitário | Contrato > `DELETE /categories/{id}` | Exclui sem produtos ativos; com ativos retorna `CATEGORY_IN_USE` e contagem, sem alterar dados. |
| `BE-CAT-12` | Unitário | Contrato > isolamento | Consultar/editar/excluir ID alheio ou excluído retorna `NOT_FOUND`. |
| `BE-CAT-13` | Unitário | Contrato > concorrência | `If-Match` antigo retorna `CONFLICT` e `ETag` atual sem sobrescrever. |
| `BE-CAT-14` | Unitário | Contrato > Erros | Validação e exceções são convertidas nos status, códigos, campos e mensagens especificados. |
| `BE-CAT-15` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository e pacote por funcionalidade. |
| `BE-CAT-16` | Integração | Testes de validação `CAT-001` a `CAT-010` | Integração entre Controller, Service e Repository, com banco substituído, cobre CRUD, propagação atômica, histórico, isolamento e concorrência. |

