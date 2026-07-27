import { expect, Page, test } from '@playwright/test';

const SENHA = 'Senha segura 123';

async function cadastrar(page: Page, cenario: string): Promise<string> {
  const email = `${cenario}.${Date.now()}.${Math.random().toString(36).slice(2)}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(SENHA);
  await page.getByLabel('Confirmar senha').fill(SENHA);
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
  return email;
}

async function abrirProdutos(page: Page): Promise<void> {
  await page.goto('/produtos');
  await expect(page.getByRole('heading', { name: 'Produtos' })).toBeVisible();
}

async function criarCategoria(page: Page, nome: string, icone = 4): Promise<void> {
  await page.goto('/categorias');
  await page.getByRole('button', { name: 'Nova categoria' }).first().click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByLabel('Nome').fill(nome);
  await page.getByTestId('icon-option').nth(icone).getByRole('radio').check();
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Categoria criada com sucesso.');
}

async function preencherProduto(
  page: Page,
  nome: string,
  categoria: string,
  unidade = 'UNIT',
): Promise<void> {
  const dialogo = page.getByRole('dialog');
  await dialogo.getByLabel('Nome').fill(nome);
  const categoriaId = await dialogo.getByLabel('Categoria padrão').locator('option')
    .filter({ hasText: categoria }).getAttribute('value');
  if (!categoriaId) throw new Error(`Categoria ${categoria} indisponível.`);
  await dialogo.getByLabel('Categoria padrão').selectOption(categoriaId);
  await dialogo.getByLabel('Unidade padrão').selectOption(unidade);
}

async function criarProduto(
  page: Page,
  nome: string,
  categoria: string,
  unidade = 'UNIT',
): Promise<void> {
  await abrirProdutos(page);
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  await preencherProduto(page, nome, categoria, unidade);
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto criado com sucesso.');
}

async function desativarProduto(page: Page, nome: string): Promise<void> {
  await abrirProdutos(page);
  const produto = page.getByTestId('product').filter({ hasText: nome });
  await produto.getByRole('button', { name: 'Desativar' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Desativar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto desativado com sucesso.');
}

async function criarListaComItem(page: Page, produto: string): Promise<void> {
  await page.goto('/listas');
  await page.getByRole('link', { name: 'Nova lista' }).click();
  await page.getByLabel('Nome da lista').fill(`Lista de ${produto}`);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page.getByText('Nenhum item na lista.')).toBeVisible();
  await page.getByRole('link', { name: 'Adicionar item' }).click();
  await page.getByLabel('Produto').fill(produto);
  await page.getByRole('listbox').getByRole('button', { name: new RegExp(produto) }).click();
  await page.getByLabel('Quantidade').fill('1');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('heading', { name: produto, exact: true })).toBeVisible();
}

test('PROD-001 - Criação repetida gera um produto correto, ordenado e único.', async ({ page }) => {
  await cadastrar(page, 'prod001');
  await criarCategoria(page, 'Padaria');
  await abrirProdutos(page);
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  await preencherProduto(page, 'Pão francês', 'Padaria');
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).dblclick();
  await expect(page.getByRole('status')).toContainText('Produto criado com sucesso.');
  const produto = page.getByTestId('product').filter({ hasText: 'Pão francês' });
  await expect(produto).toHaveCount(1);
  await expect(produto).toContainText('🍞');
  await expect(produto).toContainText('Padaria · unidade padrão: unidade');
});

test('PROD-002 - Entradas inválidas preservam o diálogo e não alteram o catálogo.', async ({ page }) => {
  await cadastrar(page, 'prod002');
  await criarProduto(page, 'Arroz', 'Mercearia');
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('Por favor, informe o nome do produto.')).toBeVisible();
  await expect(dialogo.getByText('Por favor, escolha uma categoria.')).toBeVisible();
  await expect(dialogo.getByText('Por favor, escolha uma unidade disponível.')).toBeVisible();
  await dialogo.getByLabel('Nome').fill('x'.repeat(61));
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('O nome do produto deve ter no máximo 60 caracteres.')).toBeVisible();
  await preencherProduto(page, 'Arroz', 'Mercearia');
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('Você já possui um produto ativo com este nome.')).toBeVisible();
  await dialogo.getByRole('button', { name: 'Cancelar' }).click();
  await expect(page.getByTestId('product')).toHaveCount(1);
});

test('PROD-003 - Pesquisa sem acento combina corretamente com categoria.', async ({ page }) => {
  await cadastrar(page, 'prod003');
  await criarProduto(page, 'Café torrado', 'Mercearia');
  await criarProduto(page, 'CAFE gelado', 'Bebidas', 'BOTTLE');
  await criarProduto(page, 'Arroz', 'Mercearia');
  await page.getByLabel('Pesquisar produtos').fill('cafe');
  await expect(page.getByTestId('product')).toHaveCount(2);
  await page.getByLabel('Categoria').selectOption({ label: 'Mercearia' });
  await expect(page.getByTestId('product')).toHaveCount(1);
  await expect(page.getByText('Café torrado', { exact: true })).toBeVisible();
});

test('PROD-004 - Edição afeta catálogo e novos usos sem alterar item anterior.', async ({ page }) => {
  await cadastrar(page, 'prod004');
  await criarProduto(page, 'Leite antigo', 'Bebidas', 'BOTTLE');
  await criarListaComItem(page, 'Leite antigo');
  await abrirProdutos(page);
  const produto = page.getByTestId('product').filter({ hasText: 'Leite antigo' });
  await produto.getByRole('button', { name: 'Editar' }).click();
  await expect(page.getByRole('dialog').getByLabel('Nome')).toHaveValue('Leite antigo');
  await page.getByRole('dialog').getByLabel('Nome').fill('Leite novo');
  const merceariaId = await page.getByRole('dialog').getByLabel('Categoria padrão').locator('option')
    .filter({ hasText: 'Mercearia' }).getAttribute('value');
  if (!merceariaId) throw new Error('Categoria Mercearia indisponível.');
  await page.getByRole('dialog').getByLabel('Categoria padrão').selectOption(merceariaId);
  await page.getByRole('dialog').getByLabel('Unidade padrão').selectOption('BOX');
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto atualizado com sucesso.');
  await expect(page.getByTestId('product').filter({ hasText: 'Leite novo' })).toContainText('caixa');
  await page.goto('/listas');
  await page.getByTestId('list-card').getByRole('button', { name: 'Abrir' }).click();
  await expect(page.getByRole('heading', { name: 'Leite antigo', exact: true })).toBeVisible();
});

test('PROD-005 - Desativação remove produto dos ativos e preserva item histórico.', async ({ page }) => {
  await cadastrar(page, 'prod005');
  await criarProduto(page, 'Produto histórico', 'Mercearia');
  await criarListaComItem(page, 'Produto histórico');
  await desativarProduto(page, 'Produto histórico');
  await expect(page.getByText('Produto histórico', { exact: true })).toHaveCount(0);
  await page.goto('/listas');
  await page.getByTestId('list-card').getByRole('button', { name: 'Abrir' }).click();
  await expect(page.getByRole('heading', { name: 'Produto histórico', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Adicionar item' }).click();
  await page.getByLabel('Produto').fill('Produto histórico');
  await expect(page.getByRole('listbox').getByRole('button')).toHaveCount(0);
});

test('PROD-006 - Nome de produto inativo pode ser reutilizado em novo produto ativo.', async ({ page }) => {
  await cadastrar(page, 'prod006');
  await criarProduto(page, 'Café', 'Mercearia');
  await desativarProduto(page, 'Café');
  await criarProduto(page, 'cafe', 'Bebidas', 'BOTTLE');
  await expect(page.getByTestId('product').filter({ hasText: 'cafe' })).toHaveCount(1);
});

test('PROD-007 - Mudança de ícone da categoria aparece no produto ativo.', async ({ page }) => {
  await cadastrar(page, 'prod007');
  await criarProduto(page, 'Suco', 'Bebidas', 'BOTTLE');
  await page.goto('/categorias');
  const bebidas = page.getByTestId('category').filter({ hasText: 'Bebidas' });
  await bebidas.getByRole('button', { name: 'Editar' }).click();
  await expect(page.getByRole('dialog').getByLabel('Nome')).toHaveValue('Bebidas');
  await page.getByTestId('icon-option').nth(7).getByRole('radio').check();
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Categoria atualizada com sucesso.');
  await abrirProdutos(page);
  await expect(page.getByTestId('product').filter({ hasText: 'Suco' })).toContainText('🛒');
});

test('PROD-008 - Edição concorrente preserva a primeira mudança e oferece recarga.', async ({
  page,
  context,
}) => {
  await cadastrar(page, 'prod008');
  await criarProduto(page, 'Concorrente', 'Mercearia');
  const segunda = await context.newPage();
  await segunda.goto('/produtos');
  for (const pagina of [page, segunda]) {
    await pagina.getByTestId('product').filter({ hasText: 'Concorrente' })
      .getByRole('button', { name: 'Editar' }).click();
    await expect(pagina.getByRole('dialog').getByLabel('Nome')).toHaveValue('Concorrente');
  }
  await page.getByRole('dialog').getByLabel('Nome').fill('Primeiro produto');
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await segunda.getByRole('dialog').getByLabel('Nome').fill('Produto obsoleto');
  await segunda.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(segunda.getByRole('status')).toContainText(
    'Este produto foi alterado em outro lugar. Recarregue os dados para continuar.',
  );
  await segunda.getByRole('button', { name: 'Recarregar dados' }).click();
  await expect(segunda.getByRole('dialog').getByLabel('Nome')).toHaveValue('Primeiro produto');
  await segunda.close();
});

test('PROD-009 - Catálogos de produtos permanecem isolados por usuário.', async ({ page }) => {
  await cadastrar(page, 'prod009-a');
  await criarProduto(page, 'Produto privado', 'Mercearia');
  await page.goto('/listas');
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await cadastrar(page, 'prod009-b');
  await abrirProdutos(page);
  await expect(page.getByText('Produto privado', { exact: true })).toHaveCount(0);
});

test('PROD-010 - Cancelar e Escape descartam diálogos e devolvem o foco.', async ({ page }) => {
  await cadastrar(page, 'prod010');
  await criarProduto(page, 'Produto estável', 'Mercearia');
  const novo = page.getByRole('button', { name: 'Novo produto' }).first();
  await novo.click();
  await page.getByRole('dialog').getByLabel('Nome').fill('Não criar');
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
  await expect(novo).toBeFocused();
  const produto = page.getByTestId('product').filter({ hasText: 'Produto estável' });
  const editar = produto.getByRole('button', { name: 'Editar' });
  await editar.click();
  await expect(page.getByRole('dialog').getByLabel('Nome')).toHaveValue('Produto estável');
  await page.getByRole('dialog').getByLabel('Nome').fill('Não editar');
  await page.keyboard.press('Escape');
  await expect(editar).toBeFocused();
  const desativar = produto.getByRole('button', { name: 'Desativar' });
  await desativar.click();
  await page.keyboard.press('Escape');
  await expect(desativar).toBeFocused();
  await expect(produto).toBeVisible();
});
