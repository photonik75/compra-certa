# Lista de telas
1. Login
2. Cadastro de usuário
3. Minhas listas
4. Criar/editar lista
5. Detalhes da lista
6. Adicionar/editar item da lista
7. Cadastro de categorias
8. Cadastro de tipos de item
9. Compartilhamento da lista

# Detalhamento das telas
## 1. Login
Tela inicial para usuários já cadastrados.
### Conteúdo:
- Logotipo e nome do aplicativo.
- Campo E-mail.
- Campo Senha, com opção de mostrar ou ocultar o conteúdo.
- Botão principal Entrar.
- Link Criar uma conta.
- Link Esqueci minha senha.

### Interações:
- Validar se o e-mail foi preenchido em formato válido.
- Informar quando a senha estiver vazia.
- Exibir mensagem genérica quando as credenciais estiverem incorretas.
- Manter o botão Entrar desabilitado enquanto os campos obrigatórios não estiverem preenchidos.
- Redirecionar o usuário autenticado para a tela Minhas listas.
- O link Criar uma conta abre a tela de cadastro.
- O link Esqueci minha senha inicia o processo de recuperação, caso essa funcionalidade seja incluída na primeira versão.

### Aparência:
Layout simples e centralizado, com poucos elementos e foco no formulário. Em celular, os campos devem ocupar praticamente toda a largura disponível.

## 2. Cadastro de usuário
Tela destinada à criação de uma nova conta.
### Conteúdo:
- Campo Nome.
- Campo E-mail.
- Campo Senha.
- Campo Confirmar senha.
- Botão principal Criar conta.
- Link Já tenho uma conta.
### Interações:
- Validar o preenchimento de todos os campos.
- Validar o formato do e-mail.
- Verificar se o e-mail já está cadastrado.
- Validar os critérios mínimos da senha.
- Verificar se a confirmação corresponde à senha informada.
- Exibir mensagens de validação junto aos respectivos campos.
- Após o cadastro, autenticar o usuário automaticamente ou redirecioná-lo para o login.
- Criar automaticamente categorias iniciais, como “Hortifruti”, “Limpeza” e “Bebidas”, caso essa regra seja adotada.
### Aparência:
Deve seguir o mesmo padrão visual da tela de login, mantendo consistência entre os fluxos de autenticação.

## 3. Minhas listas
Tela principal após a autenticação. Apresenta todas as listas às quais o usuário tem acesso.
### Conteúdo:
- Barra superior com:
  - título Minhas listas;
  - identificação ou avatar do usuário;
  - acesso ao menu principal.
- Campo ou botão de pesquisa, caso haja muitas listas.
- Filtros ou abas:
  - Ativas;
  - Concluídas;
  - eventualmente Todas.
- Cartões ou linhas representando cada lista, contendo:
  - nome da lista;
  - quantidade de itens;
  - quantidade de itens pendentes;
  - indicação de lista compartilhada;
  - data da última alteração.
- Botão flutuante +, para criar uma lista.
### Interações:
- Tocar em uma lista abre seus detalhes.
- O botão + abre a tela de criação de lista.
- Menu de contexto em cada lista com ações como:
  - editar nome;
  - compartilhar;
  - concluir ou reabrir;
  - excluir.
- Permitir filtrar listas pelo status.
- Exibir uma mensagem amigável quando não houver listas cadastradas, acompanhada do botão Criar primeira lista.
- Listas compartilhadas devem indicar se o usuário é proprietário ou participante.
### Aparência:
Os cartões devem destacar as informações mais importantes, principalmente nome e quantidade de itens pendentes. Listas concluídas podem aparecer visualmente atenuadas.

## 4. Criar ou editar lista
Tela utilizada para cadastrar uma nova lista ou alterar os dados básicos de uma lista existente.
### Conteúdo:
- Título variável:
  - Nova lista;
  - Editar lista.
- Campo obrigatório Nome da lista.
- Campo opcional Descrição ou observação.
- Botões:
  - Salvar;
  - Cancelar.
### Interações:
- Validar se o nome foi informado.
- Impedir ou alertar sobre nomes duplicados, caso essa regra seja necessária.
- Ao salvar uma nova lista, retornar para a tela de detalhes da lista recém-criada.
- Ao editar, atualizar o título da lista e retornar à tela anterior.
- Ao cancelar com alterações não salvas, solicitar confirmação antes de descartar.
- O usuário deve poder criar uma lista vazia e adicionar os itens posteriormente.
### Aparência:
Formulário curto, com o campo de nome em destaque. Em telas maiores, pode ser implementado como diálogo; em dispositivos móveis, é preferível uma tela completa.

## 5. Detalhes da lista
Principal tela operacional do aplicativo. É utilizada tanto no planejamento quanto durante as compras.
### Conteúdo:
- Barra superior com:
  - nome da lista;
  - botão para voltar;
  - menu de ações.
- Resumo da lista:
  - quantidade total de itens;
  - quantidade comprada;
  - quantidade pendente.
- Barra de progresso opcional.
- Itens agrupados por categoria.
- Para cada item:
  - caixa de seleção;
  - nome do produto;
  - quantidade;
  - unidade, quando aplicável;
  - observação opcional;
  - indicação visual de item comprado.
- Botão flutuante + Adicionar item.
- Ações gerais:
  - compartilhar;
  - editar lista;
  - concluir lista;
  - excluir lista.
### Interações:
- Marcar e desmarcar um item como comprado.
- Ao marcar, o item pode:
  - ficar riscado;
  - aparecer atenuado;
  - ser movido para o final da categoria.
- Tocar no item abre sua edição.
- Deslizar ou usar um menu permite editar ou excluir o item.
- Ao excluir, solicitar confirmação.
- Permitir recolher e expandir grupos de categorias.
- Atualizar a lista quando outro participante realizar alterações.
- Ao concluir a lista, solicitar confirmação e alterar seu status.
- Uma lista concluída pode ficar somente para consulta ou permitir reabertura.
### Aparência:
Deve favorecer o uso com uma única mão e em movimento. Caixas de seleção e botões precisam ser grandes. O contraste entre itens pendentes e comprados deve ser evidente.

## 6. Adicionar ou editar item da lista
Tela responsável pela inclusão de um produto em uma lista específica.
### Conteúdo:
- Campo Produto, com pesquisa e autocomplete.
- Campo Quantidade.
- Campo Unidade, como:
  - unidade;
  - pacote;
  - caixa;
  - quilograma;
  - litro.
- Campo Categoria, preenchido automaticamente com base no produto, mas editável.
- Campo opcional Observação, por exemplo:
  - “sem lactose”;
  - “marca específica”;
  - “comprar somente se estiver em promoção”.
- Botões:
- Adicionar ou Salvar;
- Cancelar.
- Opção Cadastrar novo produto, caso a pesquisa não encontre resultado.
### Interações:
- Pesquisar produtos enquanto o usuário digita.
- Evitar que o usuário cadastre variações apenas por diferenças de maiúsculas, espaços ou acentuação.
- Ao selecionar um produto, preencher automaticamente sua categoria padrão.
- Caso o produto não exista, permitir cadastrá-lo sem abandonar o fluxo.
- Alertar quando o mesmo produto já estiver na lista.
- Nesse caso, permitir:
  - somar a quantidade;
  - editar o item existente;
  - adicionar mesmo assim, quando justificável.
- Validar valores numéricos e campos obrigatórios.
### Aparência:
O campo de produto deve ser o elemento principal da tela. As sugestões do autocomplete devem ser fáceis de selecionar em dispositivos móveis.

## 7. Cadastro de categorias
CRUD destinado à organização das categorias utilizadas para agrupar os produtos.
### Conteúdo:
- Título Categorias.
- Lista das categorias cadastradas.
- Para cada categoria:
  - nome;
  - quantidade de produtos associados;
  - ações de editar e excluir.
- Campo de pesquisa.
- Botão flutuante + Nova categoria.
- Formulário de inclusão ou edição contendo:
  - nome da categoria;
  - eventualmente ícone ou ordem de exibição.
### Interações:
- Criar uma nova categoria.
- Editar o nome de uma categoria.
- Impedir categorias duplicadas, desconsiderando diferenças de maiúsculas e espaços.
- Excluir uma categoria sem produtos associados.
- Quando houver produtos associados, o sistema deve:
  - impedir a exclusão; ou
  - solicitar uma categoria de destino para os produtos.
- Exibir categorias padrão criadas durante o cadastro do usuário.
- Opcionalmente permitir reordenar as categorias.
### Aparência:
Lista simples, semelhante a uma tela de configurações. As categorias podem ser exibidas com ícones discretos, mas isso não é essencial na primeira versão.

## 8. Cadastro de tipos de item
CRUD dos produtos ou tipos de item que podem ser adicionados às listas.
### Conteúdo:
- Título Produtos ou Tipos de item.
- Campo de pesquisa.
- Filtro por categoria.
- Lista dos produtos cadastrados.
- Para cada produto:
  - nome;
  - categoria padrão;
  - unidade padrão, caso existente;
  - ações de editar e excluir.
- Botão flutuante + Novo produto.
- Formulário contendo:
  - nome do produto;
  - categoria padrão;
  - unidade padrão opcional.
### Interações:
- Criar um novo produto.
- Editar nome, categoria ou unidade padrão.
- Pesquisar produtos pelo nome.
- Filtrar produtos por categoria.
- Evitar cadastros duplicados, normalizando:
  - maiúsculas e minúsculas;
  - espaços extras;
  - eventualmente acentuação.
- Antes de excluir, verificar se o produto já foi utilizado.
- Produtos já utilizados em listas antigas não devem ser apagados fisicamente do histórico. O ideal é desativá-los para novos usos.
- Permitir criar um produto diretamente durante a inclusão de um item na lista.
- Alterar a categoria padrão não deve modificar automaticamente itens históricos já existentes.
### Aparência:
Pode utilizar uma lista compacta. O nome do produto deve ser o principal elemento, com a categoria apresentada como texto secundário.

## 9. Compartilhamento da lista
Tela destinada à administração das pessoas que podem visualizar e alterar determinada lista.
### Conteúdo:
- Nome da lista compartilhada.
- Identificação do proprietário.
- Relação de participantes.
- Para cada participante:
  - nome;
  - e-mail;
  - situação do convite;
  - nível de acesso, caso haja mais de um.
- Campo para informar o e-mail de um novo participante.
- Botão Convidar.
- Ações para remover participantes.
- Informações sobre permissões.
### Interações:
- Convidar um usuário pelo e-mail.
- Caso o usuário já esteja cadastrado, conceder acesso à lista.
- Caso ainda não esteja cadastrado, registrar um convite pendente.
- Impedir que o mesmo e-mail seja convidado mais de uma vez.
- Permitir que o proprietário remova participantes.
- Impedir que o proprietário remova a si mesmo sem antes transferir a propriedade ou excluir a lista.
- Participantes comuns podem sair da lista compartilhada.
- Na primeira versão, todos os participantes podem ter permissão de edição, simplificando o controle de acesso.
- Exibir confirmação antes de remover alguém.
- Informar claramente quando o convite for enviado ou quando ocorrer erro.
### Aparência:
A lista de participantes pode utilizar avatares com iniciais. O proprietário deve estar claramente identificado, e convites pendentes devem ter aparência diferente dos usuários ativos.

## Navegação principal
Após o login, o aplicativo pode usar um menu lateral ou menu associado ao avatar com os seguintes acessos:
- Minhas listas
- Categorias
- Produtos
- Sair
As telas de criação, edição, detalhes e compartilhamento são acessadas a partir desses módulos, não precisando aparecer diretamente no menu principal.