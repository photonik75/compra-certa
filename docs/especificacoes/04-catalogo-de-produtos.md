# EF-04 — Catálogo de produtos

## Resultado esperado

Permitir que cada usuário mantenha um catálogo pessoal de produtos, com categoria e unidade padrão, para inclusão rápida nas listas.

## Dados funcionais

### Produto

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `userId` | Proprietário do catálogo |
| `name` | Obrigatório, 1–60 caracteres normalizados |
| `categoryId` | Obrigatório; categoria ativa do mesmo usuário |
| `defaultUnit` | Obrigatória; valor permitido |
| `active` | `true` ao criar; `false` ao desativar |
| `createdAt`, `updatedAt` | Conforme convenções globais |

Unidades permitidas: `unidade`, `pacote`, `caixa`, `garrafa`, `frasco`, `lata`, `saco`, `bandeja`, `dúzia`, `quilograma`, `grama`, `litro` e `mililitro`.

O nome é único entre produtos ativos do mesmo usuário. Produtos inativos não bloqueiam a criação de um novo registro com o mesmo nome; os identificadores e históricos permanecem distintos.

## Fluxos

### 1. Listar, pesquisar e filtrar

1. Por padrão, mostrar apenas produtos ativos do usuário.
2. Ordenar por nome no locale `pt-BR`.
3. Mostrar ícone herdado da categoria, nome, categoria e unidade padrão.
4. Pesquisa por nome ignora caixa e acentos.
5. Filtro de categoria aceita uma categoria do usuário ou “Todas”.
6. Pesquisa e categoria são cumulativas.
7. Estado vazio distingue catálogo vazio de filtro sem resultados.

### 2. Criar

1. Receber nome, categoria e unidade padrão.
2. Categoria deve existir, não estar excluída e pertencer ao usuário.
3. Validar nome único entre ativos e unidade permitida.
4. Criar produto ativo e apresentá-lo na listagem.

### 3. Editar

1. Permitir alterar nome, categoria e unidade padrão de produto ativo.
2. Mudanças afetam somente inclusões futuras.
3. Itens existentes mantêm nome, categoria, ícone e unidade copiados no momento de sua inclusão.
4. Se outro processo tiver alterado o produto, retornar `CONFLICT`.

### 4. Desativar

1. Solicitar confirmação: o produto deixará de estar disponível para novos itens, mas listas existentes não serão alteradas.
2. Marcar `active=false`; não apagar fisicamente.
3. Produto desativado desaparece da listagem padrão e das sugestões da EF-05.
4. Produto já desativado tratado novamente produz sucesso idempotente.

### 5. Seleção em outros fluxos

Consultas de seleção retornam somente produtos ativos e incluem `id`, nome, categoria, ícone e unidade padrão. Nunca aceitam `categoryId` ou `productId` pertencente a outro usuário.

## Critérios de aceite

1. Produto válido aparece com o ícone da categoria escolhida.
2. Nome ativo duplicado para o mesmo usuário é recusado; outro usuário pode usar o mesmo nome.
3. Pesquisa e filtro por categoria funcionam simultaneamente.
4. Editar padrões não altera itens já adicionados a listas.
5. Produto desativado some de listagens e sugestões, mas continua legível em listas históricas.
6. É possível criar novo produto com nome de produto inativo sem reativar ou sobrescrever o registro anterior.
7. Categoria excluída, inexistente ou de outro usuário é recusada.
8. Produto de outro usuário não pode ser lido nem alterado.
9. Repetir desativação não gera erro nem altera histórico.

## Definições de testes funcionais (Playwright)

### PROD-001 — Criar produto válido (`P0`)

- **Preparação:** categoria ativa “Padaria”.
- **Ação:** criar “Pão francês” com unidade `unidade`.
- **Resultado:** produto aparece alfabeticamente com categoria, unidade e ícone herdado corretos e permanece após recarregar.

### PROD-002 — Validar nome, categoria e unidade (`P0`)

- **Preparação:** produto ativo “Arroz” e categorias de dois usuários.
- **Ação:** tentar nome vazio, acima de 60 caracteres, duplicata normalizada, categoria inexistente/excluída/de outro usuário e unidade fora da enumeração.
- **Resultado:** cada tentativa é recusada sem persistência e informa campo ou código adequado.

### PROD-003 — Pesquisa e filtro são cumulativos (`P1`)

- **Preparação:** produtos com nomes acentuados distribuídos em categorias.
- **Ação:** pesquisar sem acento e alternar o filtro de categoria.
- **Resultado:** lista ignora caixa/acentos no nome, respeita a categoria simultaneamente e apresenta estados vazios corretos.

### PROD-004 — Editar padrões só afeta inclusões futuras (`P0`)

- **Preparação:** produto ativo já utilizado em item de lista.
- **Ação:** mudar nome, categoria e unidade padrão; depois adicionar novo item com o produto.
- **Resultado:** item antigo mantém snapshots; catálogo e novo item usam os valores atualizados.

### PROD-005 — Desativar preserva histórico (`P0`)

- **Preparação:** produto ativo utilizado em lista.
- **Ação:** confirmar desativação e abrir catálogo, formulário de novo item e lista histórica.
- **Resultado:** produto some das consultas de ativos e sugestões, mas o item histórico continua íntegro; repetir desativação é idempotente.

### PROD-006 — Nome de produto inativo pode ser reutilizado (`P1`)

- **Preparação:** produto “Café” desativado.
- **Ação:** criar novo produto ativo “cafe” com outra categoria/unidade.
- **Resultado:** novo registro é criado com ID diferente, o antigo continua inativo e listas antigas não mudam.

### PROD-007 — Ícone acompanha categoria atual (`P1`)

- **Preparação:** produto ativo associado a uma categoria.
- **Ação:** alterar apenas o ícone da categoria e reabrir Produtos.
- **Resultado:** produto mostra o novo ícone sem reescrever itens existentes.

### PROD-008 — Conflito de edição não sobrescreve (`P0`)

- **Preparação:** produto aberto para edição em dois contextos do mesmo usuário.
- **Ação:** salvar mudanças no primeiro e depois no segundo.
- **Resultado:** segundo recebe `CONFLICT`, mostra possibilidade de recarregar e preserva a primeira alteração.

### PROD-009 — Catálogos permanecem privados (`P0`)

- **Preparação:** produto pertencente a `owner` e sessão de `outsider`.
- **Ação:** `outsider` tenta consultar, editar e desativar o ID do produto.
- **Resultado:** todas as operações retornam `NOT_FOUND` e nada é alterado.

### PROD-010 — Cancelar formulário não salva (`P1`)

- **Preparação:** criação e edição abertas com alterações locais.
- **Ação:** cancelar ou pressionar `Esc`.
- **Resultado:** catálogo permanece igual e o foco volta ao botão que abriu o diálogo.

## Fora do escopo específico

Reativação, marcas comerciais estruturadas, códigos de barras, fotos, preços e catálogo global compartilhado.
