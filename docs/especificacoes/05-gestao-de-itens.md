# EF-05 — Gestão de itens da lista

## Resultado esperado

Permitir que pessoas com acesso a uma lista ativa adicionem, editem e removam os produtos que precisam comprar.

## Dados funcionais

### Item da lista

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `listId` | Lista à qual pertence |
| `productId` | Produto de origem; pode apontar para produto posteriormente desativado |
| `productName` | Snapshot obrigatório do nome, até 60 caracteres |
| `categoryId` | Categoria escolhida na inclusão |
| `categoryName` | Snapshot obrigatório do nome da categoria |
| `categoryIcon` | Snapshot obrigatório do ícone |
| `quantity` | Decimal obrigatório maior que 0 e menor ou igual a 999999,99 |
| `unit` | Unidade permitida pela EF-04 |
| `notes` | Opcional, até 240 caracteres |
| `checked` | `false` ao criar |
| `checkedAt`, `checkedBy` | Nulos ao criar; controlados pela EF-06 |
| `version` | Incrementada a cada alteração |
| `createdBy`, `updatedBy` | Usuários responsáveis |
| `createdAt`, `updatedAt`, `deletedAt` | Conforme convenções globais |

Quantidade é persistida como decimal exato, nunca ponto flutuante binário. A apresentação usa vírgula decimal no locale `pt-BR` e omite zeros desnecessários.

## Permissões e pré-condições

- `OWNER` e `EDITOR` podem adicionar, editar e remover itens de lista `ACTIVE`.
- Lista `COMPLETED` é somente leitura.
- O produto selecionado deve estar ativo no catálogo pessoal de quem está adicionando. Após a inclusão, qualquer participante edita o item sem precisar possuir o produto em seu catálogo.
- A categoria escolhida deve pertencer ao catálogo de quem adiciona e estar ativa.

## Fluxos

### 1. Localizar produto

1. Ao digitar pelo menos 1 caractere, buscar produtos ativos do usuário pelo nome, ignorando caixa e acentos.
2. Ordenar correspondência exata, prefixo e depois ocorrência; dentro do grupo, por nome.
3. Retornar no máximo 10 sugestões por consulta.
4. Ao selecionar, preencher unidade e categoria com os padrões do produto; ambos podem ser alterados para o item.
5. Texto sem seleção de um produto válido não pode ser salvo. A ação “Cadastrar novo produto” abre a EF-04 e, ao retornar com sucesso, mantém os demais dados e seleciona o novo produto.

### 2. Adicionar item sem duplicidade

1. Receber produto, quantidade, unidade, categoria e observação.
2. Copiar nome e dados da categoria para o snapshot.
3. Criar item desmarcado e atualizar a lista.
4. Em sucesso, voltar ao detalhe e destacar brevemente o novo item.

### 3. Resolver produto duplicado

Há duplicidade quando existe item não excluído na mesma lista cujo `productName` normalizado seja igual ao nome do produto selecionado, independentemente de quem cadastrou o produto, da categoria ou da unidade atual. Isso evita duplicatas quando participantes possuem catálogos pessoais diferentes.

Antes de criar, apresentar três opções:

- **Cancelar:** não altera dados e mantém o formulário.
- **Editar existente:** não cria item e abre o item existente para edição.
- **Somar quantidade:** soma a quantidade informada à existente, preserva unidade, categoria e observação do item existente e não cria outro registro.

“Somar quantidade” só é permitido se as unidades forem iguais. Se diferirem, desabilitar essa opção e orientar “Editar existente”. A soma deve respeitar o limite máximo.

### 4. Editar item

1. Abrir formulário com valores atuais.
2. Permitir alterar produto, quantidade, unidade, categoria e observação.
3. Se a troca de produto causar duplicidade, aplicar o mesmo fluxo anterior; “Somar” incorpora a quantidade ao outro item e exclui logicamente o item editado, atomicamente.
4. Alterar um item marcado preserva `checked`, `checkedAt` e `checkedBy`.
5. Salvar com versão antiga retorna `CONFLICT`.

### 5. Remover item

1. Solicitar confirmação identificando o produto.
2. Excluir logicamente e recalcular totais.
3. A remoção é definitiva para o usuário na versão 1.
4. Repetir a mesma remoção é idempotente.

## Ordenação e agrupamento

No detalhe, agrupar pelo `categoryName` normalizado e exibir o nome e o ícone do primeiro item do grupo na ordenação. Ordenar grupos por nome e itens por `productName`, ambos em `pt-BR`. Categorias de catálogos diferentes com o mesmo nome normalizado formam um único grupo. Categorias posteriormente renomeadas não alteram o item até que ele seja editado e salvo com outra categoria.

## Critérios de aceite

1. Selecionar produto preenche categoria e unidade padrão, que podem ser alteradas antes de salvar.
2. Quantidade zero, negativa, não numérica ou acima do limite é recusada.
3. Item guarda snapshots; mudanças ou desativação do produto não modificam a lista existente.
4. Texto digitado sem produto selecionado não cria item.
5. Produto repetido nunca cria silenciosamente uma segunda linha.
6. Somar quantidades iguais atualiza um único item de forma atômica; unidades diferentes exigem edição.
7. Remover item atualiza imediatamente total, pendentes e percentual.
8. `EDITOR` pode administrar itens; usuário sem acesso e lista concluída não podem ser alterados.
9. Conflito de versão não sobrescreve edição concorrente.
10. Criar, editar ou remover persiste após recarregar e identifica autor e horário.

## Contrato de API (futura OpenAPI)

### Endpoints

| Método e rota | Request | Sucesso |
|---|---|---|
| `GET /api/v1/lists/{listId}/items` | Query `cursor`, `limit` | `200 ListItemCollection` |
| `POST /api/v1/lists/{listId}/items` | `CreateItemRequest` + `Idempotency-Key` | `201` criado ou `200` mesclado, ambos `ItemMutationResult` |
| `GET /api/v1/lists/{listId}/items/{itemId}` | Path UUIDs | `200 ListItem` + `ETag` |
| `PATCH /api/v1/lists/{listId}/items/{itemId}` | `UpdateItemRequest` + `If-Match` + `Idempotency-Key` | `200 ItemMutationResult` + `ETag` |
| `DELETE /api/v1/lists/{listId}/items/{itemId}` | `If-Match` + `Idempotency-Key` | `200 ItemDeletionResult` |

Todos exigem sessão e acesso `OWNER` ou `EDITOR`; mutações exigem CSRF e lista `ACTIVE`.

### Escrita de item

#### `CreateItemRequest`

```json
{
  "productId": "67ec605f-711d-420a-8f75-73999b4e609f",
  "quantity": "1.50",
  "unit": "KILOGRAM",
  "categoryId": "226506e1-871c-428f-a8fb-6fae32a7dd42",
  "notes": "Escolher bem fresca",
  "duplicateResolution": null,
  "duplicateItemVersion": null
}
```

`productId`, `quantity`, `unit` e `categoryId` são obrigatórios. `notes` pode ser omitido/null. `duplicateResolution` é omitido na primeira tentativa ou vale `MERGE`. Para `MERGE`, `duplicateItemVersion` é obrigatório.

`UpdateItemRequest` aceita os mesmos campos funcionais, todos opcionais, exige ao menos uma mudança e pode incluir `duplicateResolution`/`duplicateItemVersion`. O `If-Match` refere-se ao item editado; `duplicateItemVersion` refere-se ao item de destino da mesclagem.

O backend obtém nome/ícone/categoria pelos IDs e cria os snapshots. O frontend nunca envia `productName`, `categoryName`, `categoryIcon`, `checked`, autoria ou datas por esses endpoints.

### Leitura de item

#### `ListItem`

```json
{
  "id": "49935830-0dc6-4925-a399-661b14476187",
  "listId": "814466fa-1331-448c-a8dd-40a87771d330",
  "product": { "id": "67ec605f-711d-420a-8f75-73999b4e609f", "name": "Banana prata" },
  "category": { "id": "226506e1-871c-428f-a8fb-6fae32a7dd42", "name": "Hortifruti", "icon": "🥬" },
  "quantity": "1.50",
  "unit": "KILOGRAM",
  "notes": "Escolher bem fresca",
  "checked": false,
  "checkedAt": null,
  "checkedBy": null,
  "createdBy": { "id": "...", "name": "Larissa Barros" },
  "updatedBy": { "id": "...", "name": "Larissa Barros" },
  "createdAt": "2026-07-18T14:30:00Z",
  "updatedAt": "2026-07-18T14:30:00Z",
  "version": 1
}
```

`product.name` e todos os campos de `category` são snapshots do item. `product.id` pode referenciar produto depois desativado. `ListItemCollection` usa envelope comum, ordenado por categoria e produto conforme esta EF, e acrescenta `listSummary` e `listVersion` ao nível raiz.

### Resultados de mutação e duplicidade

`ItemMutationResult`:

```json
{
  "outcome": "CREATED",
  "item": { "id": "...", "version": 1 },
  "removedItemId": null,
  "listSummary": { "total": 8, "checked": 3, "pending": 5, "percentage": 38 },
  "listVersion": 11
}
```

`outcome` vale `CREATED`, `UPDATED` ou `MERGED`. Em mesclagem durante edição, `removedItemId` contém o item logicamente removido; em outros casos é `null`. O schema real de `item` é `ListItem` completo.

Sem resolução, duplicidade retorna `409 DUPLICATE_ITEM`:

```json
{
  "code": "DUPLICATE_ITEM",
  "meta": {
    "existingItem": { "id": "...", "productName": "Arroz", "quantity": "2", "unit": "PACKAGE", "version": 3 },
    "canMerge": true
  }
}
```

Unidades diferentes produzem `canMerge=false`; tentar `MERGE` retorna `409 INCOMPATIBLE_UNITS`. Soma acima do limite retorna `400 QUANTITY_LIMIT_EXCEEDED`.

`ItemDeletionResult` contém `{ deletedItemId, listSummary, listVersion }`. Toda mutação de item incrementa a versão da lista uma vez; mesclagem atômica também incrementa somente uma vez. Repetição com a mesma `Idempotency-Key` devolve o resultado original. Recurso inacessível retorna `404 NOT_FOUND`; lista concluída retorna `409 LIST_COMPLETED`.

## Definições de testes funcionais (Playwright)

### ITEM-001 — Selecionar produto e adicionar item (`P0`)

- **Preparação:** lista ativa e produto ativo com categoria/unidade padrão.
- **Ação:** pesquisar, selecionar a sugestão, alterar quantidade, preencher observação e salvar.
- **Resultado:** categoria e unidade são preenchidas automaticamente; detalhe abre com o item desmarcado, valores formatados e destaque temporário; recarregar preserva tudo.

### ITEM-002 — Exigir produto selecionado (`P0`)

- **Preparação:** formulário de novo item.
- **Ação:** digitar nome livre que não corresponde a seleção válida e enviar.
- **Resultado:** mostrar erro no produto, não criar item e manter os demais campos.

### ITEM-003 — Validar quantidade e enumerações (`P0`)

- **Preparação:** produto válido selecionado.
- **Ação:** testar quantidade vazia, zero, negativa, não numérica, acima de 999999,99 e, por requisição manipulada, unidade/categoria inválidas.
- **Resultado:** cada entrada é recusada, sem item parcial ou alteração nas contagens.

### ITEM-004 — Sugestões respeitam busca e catálogo ativo (`P1`)

- **Preparação:** produtos ativos, inativos e de outro usuário com nomes semelhantes.
- **Ação:** digitar termos com diferença de caixa/acento.
- **Resultado:** mostrar no máximo dez sugestões do usuário atual, ordenadas por exata/prefixo/ocorrência, sem inativos ou produtos alheios.

### ITEM-005 — Resolver duplicata nas três opções (`P0`)

- **Preparação:** item “Arroz” existente; formulário com produto de nome normalizado equivalente e mesma unidade.
- **Ação:** executar em testes independentes Cancelar, Editar existente e Somar quantidade.
- **Resultado:** Cancelar mantém formulário sem mutação; Editar abre a linha existente; Somar atualiza uma única linha, preserva metadados existentes e recalcula totais sem criar duplicata.

### ITEM-006 — Não somar unidades incompatíveis (`P0`)

- **Preparação:** item existente em `pacote` e tentativa duplicada em `quilograma`.
- **Ação:** abrir resolução de duplicidade.
- **Resultado:** “Somar quantidade” fica indisponível, a interface orienta editar o existente e nenhuma soma ocorre por requisição direta.

### ITEM-007 — Editar item e preservar marcação (`P0`)

- **Preparação:** item marcado com `checkedAt` e `checkedBy` conhecidos.
- **Ação:** alterar quantidade, unidade, categoria e observação.
- **Resultado:** novos valores persistem, snapshots são atualizados conforme a seleção e os três campos de marcação permanecem inalterados.

### ITEM-008 — Troca de produto que gera duplicidade é atômica (`P0`)

- **Preparação:** dois itens diferentes na mesma lista.
- **Ação:** editar o primeiro para o produto do segundo e escolher Somar.
- **Resultado:** sobra uma única linha com quantidade somada, item editado fica excluído logicamente e não há estado intermediário após recarregar.

### ITEM-009 — Remover item atualiza resumo (`P0`)

- **Preparação:** lista com itens marcados e pendentes.
- **Ação:** cancelar uma exclusão e depois confirmar outra; repetir a chamada confirmada.
- **Resultado:** cancelamento preserva dados; confirmação remove uma vez e atualiza total, pendentes e percentual; repetição é idempotente.

### ITEM-010 — Permissões e estado bloqueiam mutações (`P0`)

- **Preparação:** lista ativa com `editor`, lista concluída e sessão `outsider`.
- **Ação:** `editor` administra item ativo; `outsider` e ambos os papéis tentam mutar lista concluída por UI e chamada direta.
- **Resultado:** `editor` tem sucesso na ativa; demais tentativas são ocultadas/recusadas com `NOT_FOUND` ou `LIST_COMPLETED` e não alteram dados.

### ITEM-011 — Catálogos pessoais ainda detectam duplicata (`P0`)

- **Preparação:** `owner` e `editor` possuem produtos distintos chamados “Café”; um deles já está na lista compartilhada.
- **Ação:** o outro participante tenta incluir o produto do próprio catálogo.
- **Resultado:** abrir resolução de duplicidade pelo nome normalizado e nunca criar silenciosamente duas linhas.

### ITEM-012 — Conflito de versão preserva edição confirmada (`P0`)

- **Preparação:** mesmo item aberto em dois contextos autorizados.
- **Ação:** salvar alterações diferentes em sequência, mantendo a versão antiga no segundo.
- **Resultado:** segundo recebe `CONFLICT`, recarrega estado atual quando solicitado e não sobrescreve o primeiro.

## Fora do escopo específico

Itens livres sem produto cadastrado, anexos, preço, marca estruturada, ordenação manual e restauração de item removido.
