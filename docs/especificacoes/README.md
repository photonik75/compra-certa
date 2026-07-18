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

Códigos comuns: `VALIDATION_ERROR`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT` e `RATE_LIMITED`. A interface preserva os valores preenchidos quando ocorre erro recuperável e impede envio duplicado enquanto uma operação está em andamento.

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
