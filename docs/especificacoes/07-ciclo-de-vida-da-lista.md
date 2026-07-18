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

## Fora do escopo específico

Arquivamento separado, lixeira, restauração, conclusão automática e retenção configurável.
