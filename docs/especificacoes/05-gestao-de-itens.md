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

## Fora do escopo específico

Itens livres sem produto cadastrado, anexos, preço, marca estruturada, ordenação manual e restauração de item removido.
