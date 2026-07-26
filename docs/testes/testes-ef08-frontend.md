# EF-08 — Testes TDD do frontend

Implementar cada linha em ciclo vermelho/verde. Componentes usam stubs/spies dos serviços de acesso, convite
e sincronização.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-SHARE-01` | Unitário | Requisitos > Compartilhar lista | Exibe proprietário, membros e convites na ordem, com dados e ações adequados ao papel/estado. |
| `FE-SHARE-02` | Unitário | Requisitos > Convidar > e-mail | Normaliza minúsculas, valida formato e apresenta mensagens específicas de proprietário/membro/pendente. |
| `FE-SHARE-03` | Unitário | Requisitos > Convidar | Bloqueia reenvio e distingue membro adicionado, convite criado, falha de entrega e erro inesperado. |
| `FE-SHARE-04` | Unitário | Requisitos > convite pendente | Exibe validade/entrega; reenviar/cancelar atualiza visão e token anterior não permanece utilizável. |
| `FE-SHARE-05` | Unitário | Requisitos > Aceitar > preview | Exibe dados permitidos sem revelar existência de conta e trata usado/cancelado/expirado/concluída. |
| `FE-SHARE-06` | Unitário | Requisitos > Visitante | Direciona a login/cadastro, bloqueia edição do e-mail convidado e retorna preservando token. |
| `FE-SHARE-07` | Unitário | Requisitos > Usuário autenticado | E-mail divergente mostra mensagem e não consome convite; correspondente permite aceite único. |
| `FE-SHARE-08` | Unitário | Requisitos > Remover participante | Confirma identidade/efeito, trata sucesso/conflito/falha e não altera ao cancelar. |
| `FE-SHARE-09` | Unitário | Requisitos > Sair da lista | Restringe a participante de ativa, cancela sem efeito e confirma redirecionamento/mensagem. |
| `FE-SHARE-10` | Unitário | Requisitos > colaboração | Aplica matriz de permissões para itens, metadados, pessoas e ciclo de vida. |
| `FE-SHARE-11` | Unitário | Requisitos > eventos/reconexão | Atualiza em até cinco segundos, ressincroniza na reconexão e redireciona quem perdeu acesso. |
| `FE-SHARE-12` | Unitário | Contrato de API | Serviços cobrem oito endpoints, headers, schemas, `no-store` e tradução de códigos. |
| `FE-SHARE-13` | Unitário | Requisitos não funcionais > comunicação/tokens | Componentes não acessam transporte direto nem persistem token fora do fluxo necessário. |
| `FE-SHARE-14` | Unitário | Requisitos não funcionais > acessibilidade | Formulários/diálogos/listas/mensagens e foco são acessíveis e operáveis por teclado. |
| `FE-SHARE-15` | E2E | Testes de validação `SHARE-001` a `SHARE-014` | Playwright cobre convite, aceite, tokens, papéis, remoção/saída, eventos, privacidade e concluída. |
| `FE-SHARE-16` | Integração | Requisitos não funcionais > rotas | `RouterTestingHarness` valida `/listas/:listId/compartilhar` com sessão/acesso e `/convites/aceitar` com token somente no fragmento, inclusive retorno após autenticação. |
