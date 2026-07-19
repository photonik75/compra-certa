# CompraCerta — especificações funcionais da versão 1

## Objetivo

Este diretório define o comportamento funcional completo da primeira versão do CompraCerta. Os documentos são independentes de linguagem, framework e banco de dados, mas normativos quanto a regras, permissões, estados, validações e resultados observáveis.

Em caso de divergência entre o protótipo e estes documentos, prevalecem estas especificações. O protótipo continua sendo a referência visual.

## Ordem de implementação

| Sprint | Especificação | Dependências |
|---|---|---|
| 1 | [EF-01 — Autenticação e conta](01-autenticacao-e-conta.md) | Nenhuma |
| 1 | [EF-02 — Gestão básica de listas](02-gestao-basica-de-listas.md) | EF-01 |
| 2 | [EF-03 — Gestão de categorias](03-gestao-de-categorias.md) | EF-01 |
| 2 | [EF-04 — Catálogo de produtos](04-catalogo-de-produtos.md) | EF-01 e EF-03 |
| 3 | [EF-05 — Gestão de itens da lista](05-gestao-de-itens.md) | EF-02, EF-03 e EF-04 |
| 3 | [EF-06 — Execução da compra](06-execucao-da-compra.md) | EF-05 |
| 4 | [EF-07 — Ciclo de vida da lista](07-ciclo-de-vida-da-lista.md) | EF-02, EF-05 e EF-06 |
| 5 | [EF-08 — Compartilhamento e colaboração](08-compartilhamento-e-colaboracao.md) | EF-01, EF-05, EF-06 e EF-07 |

## Convenções globais

### Identificadores e datas

- Entidades persistidas possuem identificador opaco, único e imutável. UUID é recomendado, mas não obrigatório.
- Datas são persistidas em UTC e apresentadas no fuso do usuário.
- Toda entidade mutável possui `createdAt` e `updatedAt`.
- Exclusões lógicas possuem `deletedAt`; registros excluídos não aparecem em consultas normais.

### Normalização de entrada

- Antes de validar textos descritivos, remover espaços nas extremidades e substituir sequências internas de espaços por um único espaço. Essa regra não se aplica a senhas, tokens ou ao conteúdo interno de e-mails.
- Campos opcionais vazios são persistidos como `null`, não como string vazia.
- Comparações de unicidade de nomes ignoram maiúsculas/minúsculas e acentos. E-mails são comparados pela sua forma normalizada em minúsculas. O valor digitado pelo usuário é preservado para exibição quando aplicável.
- A interface deve validar antes do envio, mas o servidor é a autoridade e repete todas as validações.

### Respostas e erros

Cada comando deve resultar em sucesso ou em um erro estável com, no mínimo:

- `code`: identificador tratável pela aplicação;
- `message`: mensagem segura para apresentação;
- `field`, quando o erro estiver associado a um campo.

Códigos comuns: `VALIDATION_ERROR`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND` e `CONFLICT`. A interface preserva os valores preenchidos quando ocorre erro recuperável e impede envio duplicado enquanto uma operação está em andamento.

### Autorização

- Toda leitura ou mutação exige sessão válida, exceto cadastro, login e recuperação de senha.
- Autorização é verificada a cada operação no servidor.
- A inexistência de um recurso e a falta de acesso a ele devem produzir a mesma resposta externa (`NOT_FOUND`) quando revelar sua existência representar risco.
- Papéis em listas: `OWNER` e `EDITOR`. O criador é sempre `OWNER`; convidados ativos são `EDITOR`.

### Acessibilidade e responsividade

- Todas as funções devem ser utilizáveis por teclado e em telas a partir de 320 px de largura.
- Campos têm rótulo programaticamente associado; erros são anunciados por tecnologia assistiva.
- Diálogos prendem o foco enquanto abertos, fecham com `Esc` quando a ação não está em andamento e devolvem o foco ao elemento acionador.
- Não usar somente cor para comunicar estado.
- Operações assíncronas exibem estado de processamento, sucesso, erro e vazio quando aplicável.

### Consistência e concorrência

- Operações de escrita são atômicas.
- Totais e percentuais são derivados dos itens, nunca mantidos como fonte de verdade independente.
- Repetir acidentalmente a mesma requisição não pode criar duplicatas. Comandos de criação aceitam chave de idempotência ou proteção equivalente.
- Recursos mutáveis expõem uma versão. Uma edição baseada em versão antiga retorna `CONFLICT`, solicita atualização dos dados e não sobrescreve silenciosamente a alteração mais recente.

## Convenções da API para futura OpenAPI

Estas convenções são normativas para todas as seções “Contrato de API” e deverão ser consolidadas posteriormente em um documento OpenAPI 3.1.x.

### Protocolo e representação

- Base path: `/api/v1`.
- HTTPS obrigatório fora do desenvolvimento local.
- Requests e responses JSON usam `application/json; charset=utf-8`; `PATCH` usa `application/merge-patch+json`.
- Campos seguem `camelCase`; enums usam os valores maiúsculos documentados e não são traduzidos no protocolo.
- Objetos de request usam `additionalProperties: false`. Campos mostrados nos schemas de response são obrigatórios, salvo quando explicitamente opcionais; “ou null” significa união com `null`, não ausência do campo.
- Identificadores são strings opacas com `format: uuid`; clientes não interpretam seu conteúdo.
- Datas usam string RFC 3339 UTC com `format: date-time`, por exemplo `2026-07-18T14:30:00Z`.
- Campos não reconhecidos em requests são rejeitados com `VALIDATION_ERROR`; campos novos em responses devem ser ignorados por clientes antigos.
- Valores monetários não existem na versão 1. Quantidades decimais trafegam como strings, por exemplo `"1.50"`, para não perder precisão.

### Autenticação e proteção de escrita

- O security scheme será `sessionCookie`, cookie `cc_session`, `HttpOnly`, `Secure` e `SameSite=Lax`.
- O frontend envia `credentials: include`; sessão nunca é armazenada em `localStorage`.
- Responses de criação/consulta de sessão fornecem `csrfToken`. Toda operação mutável autenticada exige `X-CSRF-Token`.
- Ambientes com frontend e API em origens distintas permitem apenas origens explicitamente configuradas, com credenciais; curingas são proibidos.

### Versionamento, ETag e idempotência

- Recursos mutáveis retornam `version: integer >= 1` e header `ETag: "<version>"`.
- `PATCH`, `PUT`, `DELETE` e transições de estado exigem `If-Match: "<version>"`. Ausência retorna `428 PRECONDITION_REQUIRED`; versão antiga retorna `409 CONFLICT` com `meta.currentVersion`.
- `POST` que cria recurso ou dispara e-mail exige `Idempotency-Key`, UUID único por intenção do usuário. Repetição com mesmo corpo retorna o resultado original; mesma chave com corpo diferente retorna `409 IDEMPOTENCY_KEY_REUSED`.
- Sucesso de criação retorna `201`, corpo do recurso e header `Location`. Exclusão sem corpo retorna `204`. Comandos aceitos para processamento assíncrono retornam `202`.

### Paginação e filtros

- Coleções potencialmente grandes usam `cursor` opaco e `limit`, padrão 30, mínimo 1 e máximo 100.
- Envelope comum: `{ "items": [...], "page": { "nextCursor": "..." | null, "hasMore": boolean } }`.
- Parâmetros inválidos retornam `400 VALIDATION_ERROR`; cursor expirado ou incompatível com os filtros retorna `400 INVALID_CURSOR`.
- Ordenação é definida por endpoint e estável, usando `id` como último desempate.

### Erros

Erros usam `application/problem+json` com o schema comum:

```json
{
  "type": "/problems/conflict",
  "title": "Conflito de versão",
  "status": 409,
  "code": "CONFLICT",
  "detail": "O recurso foi alterado por outra pessoa.",
  "fieldErrors": [
    { "field": "name", "code": "DUPLICATE", "message": "Esse nome já está em uso." }
  ],
  "meta": { "currentVersion": 4 },
  "traceId": "01J..."
}
```

`fieldErrors` e `meta` são opcionais. `traceId` serve apenas para suporte. Mapeamento mínimo:

| HTTP | Códigos principais |
|---:|---|
| 400 | `VALIDATION_ERROR`, `INVALID_CURSOR`, `INVALID_TOKEN` |
| 401 | `UNAUTHENTICATED`, `INVALID_CREDENTIALS` |
| 403 | `FORBIDDEN`, `CSRF_INVALID` |
| 404 | `NOT_FOUND` |
| 409 | `CONFLICT`, `LIST_COMPLETED`, `DUPLICATE_ITEM`, `IDEMPOTENCY_KEY_REUSED` |
| 410 | `INVITATION_EXPIRED` |
| 428 | `PRECONDITION_REQUIRED` |

### Enums compartilhados

- `ListStatus`: `ACTIVE`, `COMPLETED`.
- `ListRole`: `OWNER`, `EDITOR`.
- `Unit`: `UNIT`, `PACKAGE`, `BOX`, `BOTTLE`, `FLASK`, `CAN`, `BAG`, `TRAY`, `DOZEN`, `KILOGRAM`, `GRAM`, `LITER`, `MILLILITER`.
- `InvitationStatus`: `PENDING`, `ACCEPTED`, `CANCELLED`, `EXPIRED`.
- `DeliveryStatus`: `QUEUED`, `SENT`, `FAILED`.

### Compatibilidade

- Mudanças aditivas opcionais podem ocorrer dentro de `/v1`; remoção, renomeação, mudança de tipo ou de semântica exige nova versão principal.
- O OpenAPI será a fonte dos tipos gerados para frontend e backend. Exemplos deste diretório devem passar na validação do schema quando o arquivo OpenAPI for criado.
- Nomes de schemas registrados nestas EFs (`ListDetail`, `ListItem`, `Membership` etc.) tornam-se nomes de `components/schemas`; renomeá-los será mudança contratual.
- Cada operação receberá `operationId` único e estável ao materializar o OpenAPI. Alterar somente `operationId` também exige coordenação, pois afeta clientes gerados.
- Nenhum endpoint depende de HTML, rota de tela ou framework específico.

### Fluxo contract-first entre frontend e backend

1. Antes de implementar os endpoints, materializar estas seções em `openapi/compra-certa-v1.yaml`, com 37 operações, `operationId`, exemplos válidos e todos os schemas referenciados.
2. Frontend gera tipos/cliente e trabalha contra um mock server derivado desse arquivo; não cria DTOs paralelos manualmente.
3. Backend gera stubs ou valida handlers/responses contra o mesmo arquivo e mantém testes de contrato para status, headers e schemas.
4. A integração contínua valida sintaxe, referências, exemplos, unicidade de `operationId` e mudanças incompatíveis antes do merge.
5. Qualquer alteração contratual começa pelo OpenAPI e pela EF correspondente; frontend e backend adotam a mesma revisão do arquivo.

Snippets com `"..."` neste diretório são abreviações de leitura. Ao migrá-los para `examples` do OpenAPI, devem ser expandidos com valores válidos para o schema.

## Fora do escopo da versão 1

- Login social, autenticação multifator e exclusão da conta;
- múltiplas moedas, preços, orçamento e comparação de mercados;
- anexos, imagens próprias e leitura de código de barras;
- funcionamento offline completo;
- papéis personalizados ou acesso somente leitura;
- transferência de propriedade da lista;
- recorrência, modelos de lista e notificações push;
- auditoria visível ao usuário e restauração de recursos excluídos.

## Definição de pronto comum

Uma especificação só está pronta quando:

1. todos os critérios de aceite do documento passam em testes automatizados;
2. regras de autorização possuem testes negativos;
3. estados de carregamento, vazio, erro e sucesso estão implementados;
4. fluxos principais funcionam em desktop e celular e somente por teclado;
5. erros não expõem senha, token, dados internos ou existência de recurso sem autorização;
6. alterações relevantes atualizam `updatedAt` e são observáveis após recarregar a página.

## Convenções para os testes funcionais em Playwright

As seções “Definições de testes funcionais” de cada EF descrevem cenários a serem automatizados posteriormente. Cada cenário identificado deve gerar um teste independente e poder ser executado isoladamente ou em qualquer ordem.

### Prioridades

- `P0`: fluxo essencial ou regra de segurança; bloqueia entrega e deve integrar a suíte de smoke test.
- `P1`: regra importante, validação ou estado alternativo; integra a regressão completa.
- `P2`: compatibilidade, acessibilidade ou comportamento complementar.

### Ambiente e fixtures

- Cada teste recebe banco isolado ou namespace exclusivo e remove seus dados ao terminar.
- O ambiente fornece uma API ou fixture de seed para criar usuários, sessões, catálogos, listas, itens, convites e versões concorrentes sem depender de outros testes.
- Fixtures padrão: `owner`, `editor`, `outsider` e `visitor`, sempre com e-mails únicos por execução.
- E-mails de recuperação e convite são capturados por uma caixa de correio de teste consultável pela suíte; nunca usar serviço ou destinatário real.
- Tempo e fuso devem ser controláveis para testar expiração, ordenação e textos relativos sem esperas reais.
- Cenários colaborativos usam dois `BrowserContext` independentes, um por usuário.
- Falhas de rede e respostas concorrentes são produzidas por ambiente de teste ou interceptação explícita, sem alterar o comportamento que se deseja validar.

### Seletores e asserções

- Priorizar `getByRole`, `getByLabel`, `getByText` e nomes acessíveis. Adicionar `data-testid` somente quando não houver referência semântica estável.
- Não selecionar por classes de estilo, posição no DOM ou texto temporal instável.
- Validar resultado pela interface e, quando necessário, por API pública de consulta; não acessar diretamente tabelas do banco no corpo do teste.
- Esperar estados e eventos observáveis, nunca usar pausas fixas.
- Para cada operação mutável, validar persistência recarregando a página ou abrindo uma nova sessão.
- Mensagens podem mudar editorialmente; quando a regra depender de tratamento programático, validar também o código de erro exposto pelo contrato.

### Projetos mínimos

- Todos os cenários `P0` e `P1`: Chromium desktop.
- Todos os `P0`: pelo menos Chromium mobile com viewport de 390 × 844.
- Cenários marcados como acessibilidade ou compatibilidade: Chromium, Firefox e WebKit, conforme disponibilidade do pipeline.

### Inventário inicial de cenários

| Especificação | Prefixo | P0 | P1 | P2 | Total |
|---|---|---:|---:|---:|---:|
| EF-01 | `AUTH` | 8 | 2 | 1 | 11 |
| EF-02 | `LIST` | 6 | 5 | 0 | 11 |
| EF-03 | `CAT` | 7 | 2 | 0 | 9 |
| EF-04 | `PROD` | 6 | 4 | 0 | 10 |
| EF-05 | `ITEM` | 11 | 1 | 0 | 12 |
| EF-06 | `SHOP` | 8 | 3 | 0 | 11 |
| EF-07 | `LIFE` | 6 | 4 | 0 | 10 |
| EF-08 | `SHARE` | 12 | 2 | 0 | 14 |
| **Total** | — | **64** | **23** | **1** | **88** |
