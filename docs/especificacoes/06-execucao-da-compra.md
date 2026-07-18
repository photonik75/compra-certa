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
5. Transporte em tempo real é decisão técnica; polling, SSE ou WebSocket são aceitáveis se cumprirem o comportamento.

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

## Fora do escopo específico

Modo offline completo, localização de mercado, rota pelos corredores, histórico visual de quem marcou e notificações push.
