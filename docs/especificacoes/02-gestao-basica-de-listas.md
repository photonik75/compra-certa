# EF-02 — Gestão básica de listas

## Resultado esperado

Permitir que o usuário consulte as listas às quais tem acesso, pesquise e filtre, crie uma lista vazia e altere seus dados básicos.

## Dados funcionais

### Lista de compras

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `ownerId` | Usuário criador; imutável na versão 1 |
| `name` | Obrigatório, 1–60 caracteres normalizados |
| `description` | Opcional, até 240 caracteres |
| `status` | `ACTIVE` ao criar; `COMPLETED` conforme EF-07 |
| `completedAt` | Nulo enquanto ativa |
| `version` | Incrementado a cada alteração |
| `createdAt`, `updatedAt`, `deletedAt` | Conforme convenções globais |

O nome deve ser único entre as listas não excluídas de propriedade do mesmo usuário. Listas das quais ele é apenas participante não entram nessa validação.

## Permissões

| Ação | OWNER | EDITOR |
|---|---:|---:|
| Listar e abrir | Sim | Sim |
| Criar lista própria | Sim | Sim, tornando-se OWNER da nova lista |
| Editar nome/descrição | Sim | Não |
| Consultar contagens/progresso | Sim | Sim |

As permissões de conclusão, exclusão e saída estão nas EF-07 e EF-08.

## Fluxos

### 1. Consultar “Minhas listas”

1. Exibir todas as listas não excluídas em que o usuário é `OWNER` ou `EDITOR`.
2. Filtro inicial: `ACTIVE`. Filtros disponíveis: ativas, concluídas e todas.
3. Ordenar por `updatedAt` decrescente dentro do filtro.
4. Pesquisa por nome contém o texto informado, ignorando caixa e acentos, combinada com o filtro atual.
5. Cada cartão mostra nome, estado, última alteração, papel do usuário, total de itens, pendentes e percentual concluído.
6. O cabeçalho mostra a soma dos itens pendentes e a quantidade de listas ativas acessíveis ao usuário.
7. Paginar no servidor quando houver mais de 30 resultados; pesquisa e filtro aplicam-se ao conjunto completo, não apenas à página carregada.

Estados vazios:

- nenhuma lista: chamada para criar a primeira lista;
- nenhuma ativa/concluída: mensagem específica do filtro;
- pesquisa sem resultado: mensagem e ação para limpar pesquisa.

### 2. Criar lista

1. Receber nome e descrição opcional.
2. Validar limites e unicidade.
3. Criar lista `ACTIVE`, sem itens, com o usuário atual como `OWNER`.
4. Em sucesso, abrir o detalhe da nova lista.
5. Cancelar volta à listagem sem persistir.

### 3. Editar lista

1. Disponível somente ao `OWNER` e para lista `ACTIVE`.
2. Abrir formulário preenchido com os valores atuais.
3. Validar nome e descrição como na criação; o próprio registro é ignorado na unicidade.
4. Salvar apenas se houver mudança. Em sucesso, atualizar `updatedAt` e `version` e voltar ao detalhe.
5. Conflito de versão não sobrescreve dados; oferecer recarregar os dados mais recentes.

### 4. Abrir lista

Lista ativa abre em modo de uso; lista concluída abre em modo de consulta conforme EF-07. Recurso inexistente, excluído ou sem acesso retorna “Lista não encontrada”.

## Critérios de aceite

1. Uma lista nova aparece no topo do filtro “Ativas”, vazia e com o criador como proprietário.
2. Nome vazio, acima de 60 caracteres ou duplicado para o mesmo proprietário impede criação e edição.
3. Um participante pode possuir lista própria com o mesmo nome de uma lista compartilhada consigo.
4. Pesquisa e filtro funcionam juntos e ignoram caixa e acentos.
5. Totais do cabeçalho e cartões correspondem aos itens persistidos, inclusive após recarregar.
6. `EDITOR` não vê ação de editar metadados e recebe `FORBIDDEN` se tentar a operação diretamente.
7. Cancelar criação ou edição não altera dados.
8. Duplo clique em salvar não cria duas listas.
9. Alteração concorrente retorna conflito e não perde a versão mais recente.

## Contrato de API (futura OpenAPI)

### Endpoints

| Método e rota | Request | Sucesso |
|---|---|---|
| `GET /api/v1/lists` | Query `status`, `search`, `cursor`, `limit` | `200 ListCollection` |
| `POST /api/v1/lists` | `CreateListRequest` + `Idempotency-Key` | `201 ListDetail` + `Location` + `ETag` |
| `GET /api/v1/lists/{listId}` | Path UUID | `200 ListDetail` + `ETag` |
| `PATCH /api/v1/lists/{listId}` | `UpdateListRequest` + `If-Match` | `200 ListDetail` + novo `ETag` |

Todos exigem sessão; mutações exigem CSRF. `PATCH` aceita somente `name` e `description`; enviar `status`, `ownerId` ou contadores retorna `400 VALIDATION_ERROR`.

### Consulta da coleção

- `status`: `ACTIVE` (padrão), `COMPLETED` ou `ALL`.
- `search`: opcional, após normalização, máximo 60 caracteres.
- Ordenação: `updatedAt desc`, depois `id asc`.
- O cursor incorpora filtros e ordenação e não pode ser reutilizado com parâmetros diferentes.

#### `ListCollection`

```json
{
  "items": [
    {
      "id": "814466fa-1331-448c-a8dd-40a87771d330",
      "name": "Compras da semana",
      "status": "ACTIVE",
      "role": "OWNER",
      "owner": { "id": "...", "name": "Larissa Barros" },
      "shared": true,
      "summary": { "total": 12, "checked": 4, "pending": 8, "percentage": 33 },
      "updatedAt": "2026-07-18T12:42:00Z",
      "completedAt": null,
      "version": 3
    }
  ],
  "page": { "nextCursor": null, "hasMore": false },
  "summary": { "activeLists": 2, "pendingItems": 16 }
}
```

`summary` no nível da coleção considera todas as listas acessíveis, sem ser limitado por `search`, `status` ou paginação. `ListCard.summary` usa somente itens não excluídos da lista.

No OpenAPI, cada elemento de `items` é o schema `ListCard`; `owner` usa `UserReference { id, name }` e ambos `ListCard.summary` e `ListDetail.summary` usam `ListSummary { total, checked, pending, percentage }`. Os quatro inteiros de `ListSummary` são obrigatórios, não negativos, e `percentage` fica entre 0 e 100.

### Criação e edição

#### `CreateListRequest`

```json
{ "name": "Compras do mês", "description": "Priorizar promoções" }
```

`name` é obrigatório; `description` pode ser omitido ou `null`. `UpdateListRequest` possui os mesmos campos, ambos opcionais, mas deve conter ao menos uma mudança. `description: null` remove a descrição.

#### `ListDetail`

| Campo | Tipo | Regra |
|---|---|---|
| `id` | UUID | Imutável |
| `name` | string | 1–60 |
| `description` | string ou null | Até 240 |
| `status` | `ListStatus` | Estado atual |
| `owner` | `UserReference` | `{ id, name }` |
| `role` | `ListRole` | Papel do usuário solicitante |
| `shared` | boolean | Verdadeiro quando há outro membro ativo |
| `summary` | `ListSummary` | `{ total, checked, pending, percentage }` |
| `createdAt`, `updatedAt` | date-time | UTC |
| `completedAt` | date-time ou null | Conforme estado |
| `version` | integer | Usado no `ETag` |

Nome duplicado retorna `409 LIST_NAME_ALREADY_IN_USE`. Lista inexistente, excluída ou inacessível retorna `404 NOT_FOUND`. Edição por `EDITOR` retorna `403 FORBIDDEN`; edição de concluída retorna `409 LIST_COMPLETED`.

## Definições de testes funcionais (Playwright)

### LIST-001 — Criar lista vazia (`P0`)

- **Preparação:** `owner` autenticado sem lista com o nome escolhido.
- **Ação:** informar nome e descrição válidos e salvar, incluindo um duplo clique no botão.
- **Resultado:** abrir o detalhe de uma única lista `ACTIVE`, vazia, com os dados informados e `owner` como proprietário; recarregar preserva o registro.

### LIST-002 — Validar campos e unicidade (`P0`)

- **Preparação:** lista própria não excluída chamada “Compras do mês”.
- **Ação:** tentar criar ou renomear usando vazio, mais de 60 caracteres, descrição acima de 240 e variações normalizadas do nome existente.
- **Resultado:** cada caso apresenta erro no campo, não persiste alteração e preserva o formulário.

### LIST-003 — Nome igual em contextos permitidos (`P1`)

- **Preparação:** `editor` participa de “Viagem” criada por `owner`.
- **Ação:** `editor` cria uma lista própria chamada “Viagem”.
- **Resultado:** criação é aceita, ambas aparecem no painel e indicam corretamente proprietário versus participante.

### LIST-004 — Painel calcula e ordena cartões (`P0`)

- **Preparação:** listas ativas e concluídas com datas, papéis e combinações conhecidas de itens marcados.
- **Ação:** abrir “Minhas listas”.
- **Resultado:** filtro inicial contém somente ativas em `updatedAt` decrescente; cartões e cabeçalho mostram contagens e percentuais derivados corretos.

### LIST-005 — Pesquisa e filtros são cumulativos (`P1`)

- **Preparação:** listas “Farmácia”, “FARMACIA antiga” e “Mercado” distribuídas entre estados.
- **Ação:** pesquisar “farmacia” e alternar Ativas, Concluídas e Todas.
- **Resultado:** resultados ignoram caixa/acentos e sempre respeitam simultaneamente texto e filtro.

### LIST-006 — Estados vazios orientam a próxima ação (`P1`)

- **Preparação:** executar com usuário sem listas, filtro sem resultados e pesquisa sem correspondência.
- **Ação:** abrir cada estado.
- **Resultado:** mostrar, respectivamente, criação da primeira lista, mensagem do filtro e ação para limpar pesquisa, sem cartão fictício.

### LIST-007 — Editar metadados como proprietário (`P0`)

- **Preparação:** lista ativa própria.
- **Ação:** alterar nome e descrição, salvar e recarregar o detalhe e o painel.
- **Resultado:** ambos exibem os novos valores, a lista sobe na ordenação e somente uma nova versão é registrada.

### LIST-008 — Participante não edita metadados (`P0`)

- **Preparação:** lista compartilhada ativa com `editor`.
- **Ação:** abrir o detalhe como `editor` e tentar a mutação também por requisição direta autenticada.
- **Resultado:** ação não aparece na interface e o servidor recusa com `FORBIDDEN`, preservando os dados.

### LIST-009 — Cancelar não persiste (`P1`)

- **Preparação:** formulário novo e formulário de edição com alterações locais.
- **Ação:** cancelar cada formulário.
- **Resultado:** voltar ao local esperado e manter os dados anteriores sem criar lista.

### LIST-010 — Conflito de edição preserva versão mais recente (`P0`)

- **Preparação:** a mesma lista aberta em dois contextos do proprietário.
- **Ação:** salvar no primeiro e depois salvar dados diferentes no segundo, ainda baseado na versão antiga.
- **Resultado:** segundo contexto recebe `CONFLICT`, não sobrescreve o primeiro e oferece recarregar os valores atuais.

### LIST-011 — Paginação aplica consulta ao conjunto inteiro (`P1`)

- **Preparação:** mais de 30 listas com uma correspondência situada após a primeira página.
- **Ação:** pesquisar e filtrar essa correspondência.
- **Resultado:** o item é encontrado e a ordenação global continua correta, sem duplicatas entre páginas.

## Fora do escopo específico

Duplicar listas, modelos, ordenação manual, imagem de capa e transferência de propriedade.
