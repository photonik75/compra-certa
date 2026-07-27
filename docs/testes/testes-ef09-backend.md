# EF-09 — Testes TDD do backend

Cada linha deve motivar apenas a menor produção necessária. Sessão, relógio e armazenamento são dublês nos testes
unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou camadas em
conjunto, substituindo banco e sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-NAV-01` | Unitário | Contrato > `GET /auth/session` | Sessão válida retorna `SessionResponse` com usuário ativo, validade e CSRF, sem segredos. |
| `BE-NAV-02` | Unitário | Contrato > sessão inválida | Sessão ausente, expirada ou revogada retorna `401 UNAUTHENTICATED` sem dados do usuário. |
| `BE-NAV-03` | Unitário | Contrato > `DELETE .../current` | Logout com sessão e CSRF válidos revoga somente a sessão atual e expira o cookie. |
| `BE-NAV-04` | Unitário | Requisitos > saída segura | Repetição não produz erro indevido; sessão revogada não volta a acessar rotas protegidas. |
| `BE-NAV-05` | Unitário | Contrato > CSRF | Logout sem token ou com token inválido retorna `403 CSRF_INVALID` e mantém a sessão. |
| `BE-NAV-06` | Unitário | Requisitos não funcionais > segurança | Respostas usam `no-store`; logs e schemas não contêm cookie, CSRF, senha ou outros segredos. |
| `BE-NAV-07` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository no pacote de autenticação. |
| `BE-NAV-08` | Integração | Testes de validação `NAV-003`, `NAV-004` e `NAV-009` | Integra camadas HTTP e sessão com armazenamento substituído, cobrindo consulta, proteção e logout. |
