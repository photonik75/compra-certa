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

## Fora do escopo específico

Duplicar listas, modelos, ordenação manual, imagem de capa e transferência de propriedade.
