# EF-09 — Testes TDD do frontend

Implementar cada linha em ciclo vermelho/verde. Componentes usam stubs ou spies do serviço de sessão.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou camadas em
conjunto, substituindo APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-NAV-01` | Unitário | Requisitos > Layout da área autenticada | Exibe marca, destinos, identificação e saída em todas as páginas autenticadas e não os exibe nas públicas. |
| `FE-NAV-02` | Unitário | Requisitos > Menu de navegação | Marca e itens abrem as rotas corretas, preservam ordem e destacam somente o destino atual. |
| `FE-NAV-03` | Unitário | Requisitos > rotas filhas | Rotas filhas mantêm destacado o destino pai de listas, categorias ou produtos. |
| `FE-NAV-04` | Unitário | Requisitos > Identificação do usuário | Apresenta nome e iniciais da sessão, sem promover papel de lista a papel global. |
| `FE-NAV-05` | Unitário | Requisitos > Ação “Sair” | Bloqueia duplo envio, navega no sucesso e preserva sessão com mensagem polida na falha. |
| `FE-NAV-06` | Unitário | Requisitos > Menu mobile | Abre e fecha por botão, destino, camada e `Esc`, com nomes acessíveis correspondentes. |
| `FE-NAV-07` | Unitário | Requisitos > foco e fundo mobile | Contém/restaura foco, bloqueia rolagem e impede interação com o conteúdo ao fundo. |
| `FE-NAV-08` | Unitário | Requisitos > Menu desktop/horizontal | Mantém painel inteiro, fixo e sem rolagem própria ou horizontal em alturas reduzidas. |
| `FE-NAV-09` | Unitário | Contrato de API | Serviço consulta sessão e encerra acesso com CSRF, sem expor transporte aos componentes. |
| `FE-NAV-10` | Integração | Requisitos não funcionais > rotas | RouterTestingHarness valida o layout e o destaque em todas as rotas públicas, autenticadas e filhas. |
| `FE-NAV-11` | Unitário | Requisitos não funcionais > acessibilidade | Valida nomes, foco, ordem, contraste não exclusivo e semântica do menu e da camada. |
| `FE-NAV-12` | E2E | Testes de validação `NAV-001` a `NAV-010` | Playwright cobre desktop, mobile, rotas, sessão, logout, foco, rolagem e acessibilidade. |
