# EF-07 — Ciclo de vida da lista

## Resultado esperado

Controlar a passagem de uma lista entre ativa e concluída e permitir sua exclusão, preservando consulta histórica enquanto ela não for excluída.

## Máquina de estados

| Estado atual | Ação | Novo estado | Quem pode |
|---|---|---|---|
| `ACTIVE` | Concluir | `COMPLETED` | OWNER |
| `COMPLETED` | Reabrir | `ACTIVE` | OWNER |
| `ACTIVE` ou `COMPLETED` | Excluir | Excluída logicamente | OWNER |

Não existem outros estados na versão 1.

## Permissões por estado

| Operação | ACTIVE | COMPLETED |
|---|---:|---:|
| Consultar lista e itens | OWNER/EDITOR | OWNER/EDITOR |
| Editar nome/descrição | OWNER | Não |
| Adicionar, editar, remover ou marcar item | OWNER/EDITOR | Não |
| Gerenciar convites/participantes | OWNER | Não |
| Concluir/reabrir/excluir | Conforme tabela acima | Conforme tabela acima |

## Fluxos

### 1. Concluir

1. Disponível ao `OWNER` de lista ativa.
2. Solicitar confirmação informando que a lista ficará somente para consulta e poderá ser reaberta.
3. Não é obrigatório que todos os itens estejam marcados; a confirmação mostra quantos permanecem pendentes.
4. Em sucesso, definir `status=COMPLETED`, `completedAt=agora`, incrementar versão e atualizar `updatedAt`.
5. A operação é atômica e faz clientes conectados entrarem imediatamente em modo de consulta.
6. Convites pendentes permanecem pendentes, mas não podem ser aceitos enquanto a lista estiver concluída; o link explica o estado.

### 2. Consultar concluída

1. Mostrar nome, descrição, data de conclusão, participantes, itens, marcações e resumo final.
2. Ocultar ações de mutação e mostrar indicação “Concluída”.
3. Tentativas diretas de mutação retornam `LIST_COMPLETED`.

### 3. Reabrir

1. Disponível ao `OWNER`.
2. Solicitar confirmação.
3. Definir `status=ACTIVE`, limpar `completedAt`, incrementar versão e atualizar `updatedAt`.
4. Preservar todos os itens e seus estados.
5. Após sucesso, operações de lista ativa voltam a ser permitidas, inclusive aceite de convites pendentes válidos.

### 4. Excluir

1. Disponível ao `OWNER` em qualquer estado.
2. Confirmação deve informar que todos os participantes perderão acesso e que não há restauração na versão 1.
3. Definir `deletedAt` na lista e invalidar acesso de participantes e convites, atomicamente.
4. Itens e vínculos podem permanecer para integridade interna, mas ficam inacessíveis por fluxos normais.
5. Remover a lista das consultas de todos os participantes e redirecionar ao painel.
6. Repetir a exclusão produz resultado idempotente para o proprietário que realizou a ação; demais clientes passam a receber `NOT_FOUND`.

### 5. Concorrência de transição

Cada transição exige a versão atual da lista. Se o estado mudou desde a leitura, retornar `CONFLICT` com o novo estado e não aplicar a ação solicitada.

## Critérios de aceite

1. Proprietário pode concluir lista com zero, alguns ou todos os itens marcados.
2. Após conclusão, nenhuma mutação de metadados, itens, marcações ou participantes é aceita.
3. Participante nunca conclui, reabre ou exclui uma lista.
4. Reabrir preserva itens, quantidades, observações e marcações.
5. Excluir remove a lista de todos os painéis e invalida convites e acessos imediatamente.
6. Lista concluída continua acessível em consulta e aparece no filtro correspondente.
7. Transições concorrentes não produzem estado intermediário nem sobrescrita silenciosa.
8. Ações destrutivas exigem confirmação explícita e não são disparadas por mero fechamento de diálogo.

## Contrato de API (futura OpenAPI)

### Endpoints

| Método e rota | Request | Sucesso |
|---|---|---|
| `PUT /api/v1/lists/{listId}/status` | `ChangeListStatusRequest` + `If-Match` + `Idempotency-Key` | `200 ListDetail` + novo `ETag` |
| `DELETE /api/v1/lists/{listId}` | `If-Match` + `Idempotency-Key` | `204` |

Ambos exigem sessão, CSRF e papel `OWNER`. O backend deriva datas e não aceita `completedAt`, `deletedAt` ou versão no corpo.

### Alterar estado

#### `ChangeListStatusRequest`

```json
{ "status": "COMPLETED" }
```

`status` aceita `COMPLETED` para concluir lista `ACTIVE` e `ACTIVE` para reabrir lista `COMPLETED`. Não há `PATCH` genérico de status. O response referencia `ListDetail` da EF-02 e contém resumo inalterado dos itens.

Regras contratuais:

- concluir não recebe nem valida contagem informada pelo cliente; o servidor calcula pendentes;
- `completedAt` é definido pelo servidor ao concluir e volta a `null` ao reabrir;
- mesma `Idempotency-Key` repete o resultado original sem nova versão/evento;
- nova solicitação cujo estado desejado já é o atual retorna `409 INVALID_LIST_TRANSITION`;
- versão antiga retorna `409 CONFLICT`; `EDITOR` retorna `403 FORBIDDEN`;
- sucesso publica `list.status.changed` no stream da EF-06.

### Excluir

- A operação marca a lista excluída e revoga membros/convites na mesma transação.
- Sucesso retorna `204` sem corpo e publica `list.deleted`/`list.access.changed` quando houver streams conectados.
- Repetição com a mesma `Idempotency-Key` retorna `204`; outra solicitação após a exclusão retorna `404 NOT_FOUND`.
- Lista inexistente, excluída ou inacessível retorna `404 NOT_FOUND`.
- O frontend remove a lista localmente após `204`; não existe endpoint de restauração.

### Headers de resposta

`PUT /status` retorna `ETag` da nova versão e `Cache-Control: no-store`. `DELETE` não retorna `ETag`. Operações não concluídas por conflito não publicam evento.

## Definições de testes funcionais (Playwright)

### LIFE-001 — Concluir lista com itens pendentes (`P0`)

- **Preparação:** lista ativa própria com itens comprados e pendentes.
- **Ação:** abrir confirmação e concluir.
- **Resultado:** diálogo informa a quantidade pendente; lista passa a `COMPLETED`, registra data, aparece no filtro Concluídas e entra em modo somente leitura.

### LIFE-002 — Concluir lista vazia ou totalmente comprada (`P1`)

- **Preparação:** uma lista vazia e outra com todos os itens marcados.
- **Ação:** concluir cada uma.
- **Resultado:** ambas são concluídas com sucesso e mantêm resumos corretos.

### LIFE-003 — Cancelar transições não muda estado (`P1`)

- **Preparação:** diálogos de concluir, reabrir e excluir disponíveis.
- **Ação:** cancelar ou pressionar `Esc` em cada diálogo.
- **Resultado:** estado e dados permanecem intactos e o foco retorna ao acionador.

### LIFE-004 — Lista concluída bloqueia todas as mutações (`P0`)

- **Preparação:** lista concluída com `owner`, `editor`, itens e convite pendente.
- **Ação:** tentar editar metadados, administrar/marcar itens, convidar, remover participante e aceitar convite, pela UI e por chamadas diretas.
- **Resultado:** consulta continua disponível, mutações retornam `LIST_COMPLETED`, convite permanece pendente e nada muda.

### LIFE-005 — Reabrir preserva conteúdo (`P0`)

- **Preparação:** lista concluída com dados e marcações conhecidas.
- **Ação:** confirmar reabertura.
- **Resultado:** status volta a `ACTIVE`, `completedAt` é limpo, conteúdo permanece idêntico e mutações voltam a funcionar.

### LIFE-006 — Participante não controla ciclo de vida (`P0`)

- **Preparação:** `editor` de lista ativa e concluída.
- **Ação:** procurar e invocar concluir, reabrir e excluir, inclusive diretamente.
- **Resultado:** ações não aparecem e chamadas são recusadas com `FORBIDDEN`, sem revelar controles exclusivos.

### LIFE-007 — Excluir revoga todos os acessos (`P0`)

- **Preparação:** lista com `owner`, `editor`, convite pendente e dois contextos abertos.
- **Ação:** `owner` confirma exclusão.
- **Resultado:** ambos saem para o painel, lista desaparece das consultas, convite deixa de funcionar e URLs antigas retornam `NOT_FOUND`.

### LIFE-008 — Exclusão é idempotente (`P1`)

- **Preparação:** capturar duas requisições equivalentes do proprietário para a mesma exclusão.
- **Ação:** enviá-las em sequência controlada.
- **Resultado:** existe uma única exclusão lógica, sem erro interno ou restauração; outros usuários recebem `NOT_FOUND`.

### LIFE-009 — Transições concorrentes não se sobrescrevem (`P0`)

- **Preparação:** mesma lista e versão abertas em dois contextos do proprietário.
- **Ação:** concluir no primeiro e tentar excluir/editar com a versão antiga no segundo.
- **Resultado:** a segunda transição recebe `CONFLICT`, adota o estado atual e não produz estado intermediário.

### LIFE-010 — Consulta histórica exibe resumo final (`P1`)

- **Preparação:** lista concluída com descrição, participantes e marcações conhecidas.
- **Ação:** abrir pelo painel e recarregar.
- **Resultado:** mostrar data de conclusão, participantes, itens e resumo final, sem qualquer controle mutável.

## Fora do escopo específico

Arquivamento separado, lixeira, restauração, conclusão automática e retenção configurável.
