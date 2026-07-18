# EF-06 — Execução da compra

## Resultado esperado

Permitir que participantes usem uma lista ativa durante a compra, marcando e desmarcando itens e acompanhando o progresso correto.

## Resumo derivado

Para itens não excluídos:

- `total = quantidade de itens`;
- `comprados = quantidade com checked=true`;
- `pendentes = total - comprados`;
- `percentual = 0` quando `total=0`; nos demais casos, `round(comprados / total * 100)`.

Esses valores são calculados a partir dos itens na mesma leitura consistente. Quantidade numérica de um item não altera a contagem: uma linha é um item.

## Fluxos

### 1. Consultar lista ativa

1. Mostrar nome, indicação de lista compartilhada quando aplicável, horário da última sincronização, resumo e barra de progresso.
2. Agrupar e ordenar itens conforme EF-05.
3. Cada grupo mostra ícone, nome da categoria, quantidade de itens e pode ser expandido/recolhido localmente.
4. Cada item mostra nome, quantidade, unidade, observação quando houver, estado comprado e ações permitidas.
5. Recolher grupo é preferência apenas da interface e não altera o servidor.

### 2. Marcar como comprado

1. `OWNER` ou `EDITOR` aciona a caixa do item em lista `ACTIVE`.
2. Persistir atomicamente `checked=true`, `checkedAt=agora`, `checkedBy=usuário atual`, incrementar versão do item e atualizar a lista.
3. Atualizar linha, resumo e barra somente após confirmação do servidor; pode haver atualização otimista se houver reversão visual em caso de erro.
4. Repetir comando para o mesmo estado é idempotente e não troca `checkedAt/checkedBy`.

### 3. Desmarcar

Persistir `checked=false`, limpar `checkedAt` e `checkedBy`, incrementar versão e atualizar o resumo. Repetir quando já desmarcado é idempotente.

### 4. Atualizações concorrentes

1. Para o mesmo item, o servidor serializa mudanças e transmite o estado confirmado mais recente.
2. Um comando baseado em versão antiga retorna `CONFLICT` com o estado atual; a interface adota esse estado e informa que o item foi atualizado por outra pessoa.
3. Mudanças confirmadas em lista compartilhada devem aparecer para clientes conectados em até 5 segundos, sem recarregar a página.
4. Ao reconectar ou voltar do segundo plano, buscar uma versão atual da lista antes de aceitar novas alterações.
5. A versão 1 usa Server-Sent Events (SSE) conforme o contrato desta EF. O cliente sempre confirma o estado pela API HTTP antes de considerar uma escrita sincronizada.

### 5. Falha de conexão

- A versão 1 não garante edição offline.
- Sem conexão confirmada, informar o estado e impedir novas marcações ou colocá-las como “aguardando”, sem mostrá-las como sincronizadas.
- Ao falhar uma escrita, restaurar o último estado confirmado e permitir tentar novamente.

## Estados de interface

- Lista vazia: resumo zerado e ação para adicionar o primeiro item.
- Sincronizando, sincronizado com horário, sem conexão e falha de sincronização.
- Item em atualização deve impedir cliques repetidos até confirmação.
- Observação é exibida como texto, sem interpretar HTML.

## Critérios de aceite

1. Marcar e desmarcar persiste e recalcula os quatro valores do resumo corretamente.
2. Lista sem itens apresenta percentual zero, sem divisão por zero.
3. Repetir o mesmo estado não altera autor ou horário da marcação original.
4. Usuário sem acesso, lista concluída ou item removido não pode ser marcado.
5. Dois clientes visualizando a mesma lista recebem alterações confirmadas em até 5 segundos.
6. Conflito não deixa clientes indefinidamente divergentes nem sobrescreve silenciosamente outro usuário.
7. Falha de rede não apresenta mudança não confirmada como sincronizada.
8. Grupos podem ser recolhidos sem afetar dados ou outros usuários.
9. Totais permanecem corretos após inclusão, remoção, marcação e atualização simultâneas.

## Contrato de API (futura OpenAPI)

### Endpoints

| Método e rota | Request | Sucesso |
|---|---|---|
| `PUT /api/v1/lists/{listId}/items/{itemId}/checked` | `CheckItemRequest` + `If-Match` | `200 CheckItemResult` + novo `ETag` |
| `GET /api/v1/lists/{listId}/events` | `Accept: text/event-stream`, `Last-Event-ID` opcional | Stream SSE |

A carga inicial usa `GET /lists/{listId}` e todas as páginas de `GET /lists/{listId}/items`. O endpoint SSE transporta mudanças posteriores e não substitui a leitura autoritativa.

### Marcar e desmarcar

#### `CheckItemRequest`

```json
{ "checked": true }
```

`checked` é o único campo aceito. O endpoint exige sessão, CSRF, papel `OWNER` ou `EDITOR`, lista `ACTIVE` e `If-Match` do item.

#### `CheckItemResult`

```json
{
  "item": {
    "id": "49935830-0dc6-4925-a399-661b14476187",
    "checked": true,
    "checkedAt": "2026-07-18T15:02:00Z",
    "checkedBy": { "id": "...", "name": "Larissa Barros" },
    "version": 4
  },
  "listSummary": { "total": 8, "checked": 4, "pending": 4, "percentage": 50 },
  "listVersion": 12
}
```

No OpenAPI, `item` referencia `ListItem` completo da EF-05. Se o estado atual já for igual ao solicitado, retornar `200` com o estado atual sem alterar versão, autor ou horário, mesmo que o `If-Match` represente a versão imediatamente anterior. Se o estado for diferente e a versão estiver obsoleta, retornar `409 CONFLICT`.

### Stream de eventos

- Autenticação pelo `sessionCookie`; não exige CSRF por ser somente leitura.
- Response: `200`, `Content-Type: text/event-stream`, `Cache-Control: no-cache`, conexão persistente.
- `Last-Event-ID` permite retomar a partir do último evento confirmado.
- Heartbeat como comentário SSE a cada no máximo 20 segundos.
- Eventos autorizados devem chegar em até cinco segundos após commit.

Cada mensagem usa `id` SSE opaco, campo `event` com o tipo e `data` com `ListEvent` JSON:

```text
id: 01J4EVENT
event: list.item.checked
data: {"listId":"...","listVersion":12,"resourceId":"...","actor":{"id":"...","name":"Larissa Barros"},"occurredAt":"2026-07-18T15:02:00Z","payload":{"checked":true,"itemVersion":4,"listSummary":{"total":8,"checked":4,"pending":4,"percentage":50}}}
```

Tipos da versão 1:

- `list.updated`;
- `list.status.changed`;
- `list.deleted`;
- `list.item.created`, `list.item.updated`, `list.item.deleted`, `list.item.checked`;
- `list.access.changed`;
- `resync.required`.

`payload` tem schema discriminado pelo tipo no OpenAPI. Eventos de item incluem item completo para criação/edição, `resourceId` para exclusão e estado/resumo para marcação. Eventos nunca incluem tokens, e-mails desnecessários ou dados de catálogo privado.

Se `Last-Event-ID` não estiver mais retido, enviar `resync.required` e fechar; o frontend refaz as leituras iniciais. Remoção de acesso ou exclusão envia o evento quando possível e encerra o stream. Nova conexão sem acesso retorna `404 NOT_FOUND`.

### Erros específicos

- Item removido ou sem acesso: `404 NOT_FOUND`.
- Lista concluída: `409 LIST_COMPLETED`.
- Versão divergente: `409 CONFLICT`, com `meta.currentVersion` e `meta.currentChecked`.
- Falha transitória: `503 SERVICE_UNAVAILABLE`; cliente restaura estado confirmado e pode tentar novamente com nova leitura.

## Definições de testes funcionais (Playwright)

### SHOP-001 — Resumo inicial é derivado dos itens (`P0`)

- **Preparação:** lista com oito linhas, três marcadas e quantidades numéricas variadas.
- **Ação:** abrir o detalhe.
- **Resultado:** mostrar total 8, comprados 3, pendentes 5 e percentual 38; quantidade de unidades não altera a contagem.

### SHOP-002 — Marcar e desmarcar persiste (`P0`)

- **Preparação:** item pendente em lista ativa.
- **Ação:** marcar, recarregar, desmarcar e recarregar novamente.
- **Resultado:** linha, resumo e progresso refletem cada estado confirmado; marcação registra autor/horário e desmarcação os limpa.

### SHOP-003 — Comando idempotente preserva autoria (`P1`)

- **Preparação:** item já marcado com autor e horário conhecidos.
- **Ação:** repetir o comando `checked=true` pela interface/API de teste.
- **Resultado:** operação tem sucesso sem mudar autor, horário ou contagens.

### SHOP-004 — Lista vazia tem progresso válido (`P0`)

- **Preparação:** lista ativa sem itens.
- **Ação:** abrir o detalhe.
- **Resultado:** mostrar zeros, percentual zero e ação para adicionar o primeiro item, sem `NaN`, infinito ou grupo vazio.

### SHOP-005 — Agrupamento e recolhimento são locais (`P1`)

- **Preparação:** itens de categorias distintas, incluindo nomes normalizados iguais vindos de catálogos diferentes.
- **Ação:** abrir, verificar ordenação e recolher um grupo em um contexto.
- **Resultado:** categorias equivalentes formam um grupo, contagens e ordem estão corretas e outro contexto não é afetado pelo recolhimento.

### SHOP-006 — Atualização chega a outro participante (`P0`)

- **Preparação:** `owner` e `editor` abrem a mesma lista em contextos independentes.
- **Ação:** `owner` marca um item e `editor` adiciona outro.
- **Resultado:** cada alteração confirmada aparece no outro contexto, com resumo correto, em até cinco segundos e sem recarregar.

### SHOP-007 — Conflito no mesmo item converge (`P0`)

- **Preparação:** dois contextos com a mesma versão de um item.
- **Ação:** ambos enviam estados diferentes de forma controlada.
- **Resultado:** uma alteração é confirmada, a obsoleta recebe `CONFLICT`, ambos convergem para o estado do servidor e o usuário afetado é informado.

### SHOP-008 — Falha de escrita reverte estado otimista (`P0`)

- **Preparação:** interceptar a mutação para falhar após o clique.
- **Ação:** marcar item.
- **Resultado:** interface mostra processamento, não permite clique duplicado, restaura o último estado confirmado, informa erro e oferece nova tentativa.

### SHOP-009 — Reconexão ressincroniza antes de editar (`P0`)

- **Preparação:** um contexto perde conexão; outro altera itens.
- **Ação:** restaurar conexão e tentar nova marcação no primeiro.
- **Resultado:** exibir estado sem conexão enquanto aplicável, buscar versão atual antes da escrita e terminar com resumo convergente.

### SHOP-010 — Usuários não autorizados e lista concluída são somente leitura (`P0`)

- **Preparação:** lista concluída acessível a `owner/editor` e lista ativa inacessível a `outsider`.
- **Ação:** tentar marcações pela interface e por requisição direta.
- **Resultado:** controles não editáveis/ausentes e respostas `LIST_COMPLETED` ou `NOT_FOUND`, sem alteração visual persistente.

### SHOP-011 — Operações simultâneas mantêm totais consistentes (`P1`)

- **Preparação:** dois contextos em lista com estado conhecido.
- **Ação:** simultaneamente adicionar, remover e marcar itens diferentes.
- **Resultado:** após convergência e recarga, total, comprados, pendentes e percentual correspondem exatamente às linhas persistidas.

## Fora do escopo específico

Modo offline completo, localização de mercado, rota pelos corredores, histórico visual de quem marcou e notificações push.
