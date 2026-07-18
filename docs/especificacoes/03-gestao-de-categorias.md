# EF-03 — Gestão de categorias

## Resultado esperado

Permitir que cada usuário organize seu catálogo pessoal em categorias reutilizáveis.

## Dados funcionais

### Categoria

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `userId` | Proprietário do catálogo |
| `name` | Obrigatório, 1–40 caracteres normalizados |
| `icon` | Obrigatório; um dos valores permitidos |
| `createdAt`, `updatedAt`, `deletedAt` | Conforme convenções globais |

Ícones permitidos na versão 1: `🥬`, `🛍️`, `🧃`, `🧴`, `🍞`, `❄️`, `🐾` e `🛒`. O conjunto pode ser configurado no código, mas a API rejeita valores fora dele.

Nome é único entre categorias não excluídas do mesmo usuário.

## Categorias iniciais

No cadastro, criar atomicamente:

| Nome | Ícone |
|---|---|
| Hortifruti | 🥬 |
| Mercearia | 🛍️ |
| Bebidas | 🧃 |
| Limpeza | 🧴 |

Falha ao criar esse conjunto deve desfazer o cadastro ou ser recuperada antes de confirmar seu sucesso.

## Fluxos

### 1. Listar e pesquisar

1. Mostrar somente categorias do usuário autenticado e não excluídas.
2. Ordenar por nome usando regras do locale `pt-BR`.
3. Mostrar ícone, nome e quantidade de produtos ativos associados.
4. Pesquisar por nome, ignorando caixa e acentos.
5. Estado vazio oferece a ação “Nova categoria”.

### 2. Criar

1. Receber nome e ícone.
2. Validar comprimento, ícone permitido e unicidade.
3. Persistir e incluir imediatamente na listagem.

### 3. Editar

1. Permitir alterar nome e ícone.
2. Alterar o ícone atualiza a apresentação de produtos ativos associados no catálogo.
3. Itens já inseridos em listas não são alterados, pois usam snapshot conforme EF-05.
4. Alterar o nome atualiza a referência categorial dos produtos ativos associados, sem alterar itens históricos.

### 4. Excluir

1. Categoria com qualquer produto ativo associado não pode ser excluída e retorna `CATEGORY_IN_USE`.
2. A interface informa quantos produtos precisam ser movidos ou desativados.
3. Sem produtos ativos, solicitar confirmação e realizar exclusão lógica.
4. Produtos inativos e itens históricos preservam os dados de exibição anteriores.

Todas as operações confirmam que a categoria pertence ao usuário atual.

## Critérios de aceite

1. Uma conta nova contém exatamente as quatro categorias iniciais.
2. Dois usuários podem ter categorias com o mesmo nome sem interferência.
3. Nome duplicado, inclusive com diferenças apenas de caixa, acento ou espaços, é recusado.
4. Pesquisa encontra “Mercearia” ao buscar “merce” e ignora acentos.
5. Editar nome/ícone reflete nos produtos ativos associados, mas não reescreve itens existentes.
6. Categoria com produto ativo não pode ser excluída por interface nem por chamada direta.
7. Categoria sem produtos ativos pode ser excluída após confirmação e deixa de aparecer nas seleções.
8. Contagem exibida considera somente produtos ativos.
9. Usuário não lê nem altera categoria de outro usuário.

## Definições de testes funcionais (Playwright)

### CAT-001 — Conta nova recebe categorias iniciais (`P0`)

- **Preparação:** conta recém-criada.
- **Ação:** abrir Categorias.
- **Resultado:** mostrar exatamente Hortifruti, Mercearia, Bebidas e Limpeza com os respectivos ícones e contagem zero quando não houver produtos.

### CAT-002 — Criar categoria válida (`P0`)

- **Preparação:** usuário autenticado.
- **Ação:** criar “Padaria” com ícone permitido.
- **Resultado:** diálogo fecha, confirmação aparece, categoria entra na posição alfabética e permanece após recarregar.

### CAT-003 — Recusar nome ou ícone inválido (`P0`)

- **Preparação:** categoria “Bebidas” existente.
- **Ação:** tentar nome vazio, acima de 40 caracteres, duplicata normalizada e ícone não permitido por requisição manipulada.
- **Resultado:** retornar erro correspondente, manter diálogo aberto e não criar/alterar categoria.

### CAT-004 — Pesquisa ignora caixa e acentos (`P1`)

- **Preparação:** categorias incluindo “Higiene” e “Grãos”.
- **Ação:** pesquisar variações sem acento e com caixa diferente.
- **Resultado:** mostrar somente correspondências corretas e estado de ausência quando aplicável.

### CAT-005 — Editar propaga ao catálogo, não ao histórico (`P0`)

- **Preparação:** categoria com produto ativo e item de lista criado anteriormente a partir dele.
- **Ação:** alterar nome e ícone da categoria.
- **Resultado:** categoria e produto ativo exibem novos dados; item existente mantém snapshots anteriores, inclusive após recarregar.

### CAT-006 — Bloquear exclusão quando há produto ativo (`P0`)

- **Preparação:** categoria com dois produtos ativos.
- **Ação:** tentar excluí-la pela interface e por requisição direta.
- **Resultado:** retornar `CATEGORY_IN_USE`, informar a quantidade dois e preservar categoria e produtos.

### CAT-007 — Excluir categoria sem produto ativo (`P0`)

- **Preparação:** categoria sem produtos ativos, podendo haver produto inativo e item histórico.
- **Ação:** confirmar exclusão.
- **Resultado:** categoria some do catálogo e seleções, produto/item histórico continua legível e recarregar não restaura a categoria.

### CAT-008 — Cancelar diálogos preserva dados (`P1`)

- **Preparação:** abrir criação e edição com valores modificados.
- **Ação:** cancelar e também fechar com `Esc`.
- **Resultado:** nenhuma alteração é persistida e o foco retorna ao acionador.

### CAT-009 — Isolamento entre usuários (`P0`)

- **Preparação:** `owner` e `outsider` possuem categorias de mesmo nome e IDs distintos.
- **Ação:** cada um lista e edita a própria; `outsider` tenta alterar o ID do outro por requisição direta.
- **Resultado:** cada catálogo permanece isolado e a tentativa cruzada retorna `NOT_FOUND` sem revelar o recurso.

## Fora do escopo específico

Ícones personalizados, ordenação manual, categorias compartilhadas e mesclagem de categorias.
