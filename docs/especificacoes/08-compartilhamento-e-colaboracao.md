# EF-08 — Compartilhamento e colaboração

## Resultado esperado

Permitir que o proprietário compartilhe uma lista ativa por e-mail e que participantes ativos colaborem nos itens com controle de acesso consistente e atualização em tempo hábil.

## Dados funcionais

### Participação

| Campo | Regra |
|---|---|
| `listId`, `userId` | Par único |
| `role` | `OWNER` ou `EDITOR` |
| `joinedAt` | Data de ativação |
| `createdAt`, `updatedAt` | Auditoria |

### Convite

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `listId` | Lista ativa |
| `email` | E-mail normalizado do destinatário |
| `invitedBy` | OWNER responsável |
| `status` | `PENDING`, `ACCEPTED`, `CANCELLED` ou `EXPIRED` |
| `deliveryStatus` | `QUEUED`, `SENT` ou `FAILED` |
| `tokenHash` | Token de uso único armazenado de modo não reversível |
| `expiresAt` | 7 dias após envio ou reenvio |
| `acceptedBy`, `acceptedAt` | Preenchidos no aceite |

Só pode haver um convite `PENDING` por par lista/e-mail.

## Matriz de permissões

| Ação | OWNER | EDITOR |
|---|---:|---:|
| Ver lista e participantes | Sim | Sim |
| Adicionar, editar, remover e marcar itens em lista ativa | Sim | Sim |
| Convidar, reenviar ou cancelar convite | Sim | Não |
| Remover participante | Sim | Não |
| Sair da lista | Não | Sim |
| Editar metadados, concluir, reabrir ou excluir lista | Sim, conforme EF-07 | Não |

## Fluxos

### 1. Consultar pessoas com acesso

1. OWNER e EDITOR veem proprietário, participantes ativos e convites pendentes.
2. Mostrar nome e e-mail de usuários ativos; para convite sem conta, mostrar o e-mail e “Convite pendente”.
3. OWNER vê ações permitidas; EDITOR vê a relação sem controles administrativos.
4. A lista de pessoas é ordenada: proprietário, participantes por nome e convites por e-mail.

### 2. Convidar

1. Somente OWNER de lista `ACTIVE` informa um e-mail válido.
2. Não permitir convidar o próprio proprietário, participante ativo ou e-mail com convite pendente; retornar erro específico.
3. Se o e-mail pertence a uma conta ativa, criar participação `EDITOR` imediatamente e enviar aviso por e-mail. A lista passa a aparecer no painel dessa pessoa.
4. Se não pertence a uma conta, criar convite `PENDING`, enviar link e exibi-lo na lista de pessoas.
5. O link contém token aleatório, de uso único, e não expõe identificadores sensíveis suficientes para acesso sem token.
6. Persistir o convite antes do envio. A entrega usa fila transacional ou mecanismo equivalente: começa como `QUEUED`, passa a `SENT` quando o provedor confirma ou `FAILED` quando as tentativas se esgotam.
7. Falha definitiva não concede falsa confirmação de envio: o convite permanece `PENDING`, aparece ao OWNER como “Falha no envio” e oferece “Reenviar”.

### 3. Aceitar convite pendente

1. Visitante abre o link. Se não possui conta com o e-mail convidado, é direcionado ao cadastro com e-mail bloqueado para alteração nesse fluxo.
2. Após cadastro/login, a conta autenticada deve possuir exatamente o e-mail convidado.
3. Token válido e lista `ACTIVE` criam participação `EDITOR` e marcam convite `ACCEPTED` atomicamente.
4. Token usado, cancelado ou expirado não concede acesso.
5. Criar conta pelo fluxo comum com o mesmo e-mail não aceita convites automaticamente; a pessoa precisa abrir o link válido enviado a ela.

### 4. Reenviar ou cancelar convite

- Reenviar é permitido somente ao OWNER para convite `PENDING`; invalida token anterior, gera novo token e reinicia validade de 7 dias.
- Cancelar marca `CANCELLED`, invalida token e remove o convite da visão padrão.
- Convite vencido passa a `EXPIRED` na primeira leitura ou tentativa de uso e pode ser reenviado, gerando novo estado `PENDING`.

### 5. Remover participante

1. OWNER seleciona um `EDITOR` ativo e confirma a remoção identificando nome/e-mail.
2. Remover o vínculo de acesso de forma atômica.
3. O participante perde acesso imediatamente; sessões abertas recebem o evento e saem da lista para o painel.
4. Não é permitido remover o proprietário.
5. Dados criados pelo participante permanecem e conservam autoria histórica.

### 6. Sair da lista

1. EDITOR pode sair após confirmação.
2. O vínculo é removido e a lista desaparece de seu painel.
3. OWNER não pode sair, pois transferência de propriedade está fora do escopo.
4. Sair não remove itens nem autoria histórica.

### 7. Colaboração em tempo hábil

1. Eventos de criação, edição, remoção e marcação de itens, mudança de estado e acesso são propagados a clientes conectados em até 5 segundos.
2. Cada evento inclui identificador da lista, tipo, versão e dados mínimos para atualizar ou sinalizar recarga; não inclui senha ou token.
3. O servidor continua sendo a fonte de verdade. Reconexão exige ressincronização.
4. Regras de conflito seguem as convenções globais e a EF-06.

## Segurança

- Tokens de convite nunca são registrados em texto puro nem retornados depois do envio inicial.
- Endereços de e-mail só são exibidos a pessoas com acesso atual à lista.
- Toda ação administrativa é revalidada no servidor.
- Remoção, saída, cancelamento e exclusão revogam acesso sem depender da atualização da interface.

## Critérios de aceite

1. Convidar conta existente concede acesso `EDITOR` uma única vez e a lista aparece em seu painel.
2. Convidar e-mail sem conta cria convite pendente e o aceite válido concede acesso à conta de mesmo e-mail.
3. E-mail do proprietário, participante ou convite pendente não pode ser convidado novamente.
4. Token expirado, usado ou cancelado nunca concede acesso.
5. Reenvio invalida o token anterior.
6. EDITOR administra itens, mas não administra lista, convites ou pessoas.
7. Removido ou usuário que saiu perde acesso imediatamente, inclusive em sessão já aberta.
8. Exclusão da lista invalida todos os acessos e convites.
9. Alterações colaborativas chegam a outros clientes conectados em até 5 segundos e convergem após reconexão.
10. Ações de um participante removido durante uma operação são recusadas no servidor.

## Fora do escopo específico

Convite por link público, contatos, QR Code, transferência de propriedade, acesso somente leitura, comentários, chat e notificações push.
