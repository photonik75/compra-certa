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

async function criarProduto(page: Page, nome: string, unidade = 'UNIT'): Promise<void> {
  await page.goto('/produtos');
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByLabel('Nome').fill(nome);
  const categoria = await dialogo.getByLabel('Categoria padrão').locator('option')
    .filter({ hasText: 'Mercearia' }).getAttribute('value');
  if (!categoria) throw new Error('Categoria padrão indisponível.');
  await dialogo.getByLabel('Categoria padrão').selectOption(categoria);
  await dialogo.getByLabel('Unidade padrão').selectOption(unidade);
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto criado com sucesso.');
}

async function criarLista(page: Page, nome: string): Promise<void> {
  await page.goto('/listas');
  await page.getByRole('link', { name: 'Nova lista' }).click();
  await page.getByLabel('Nome da lista').fill(nome);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page.getByText('Nenhum item na lista.')).toBeVisible();
}

async function abrirAdicao(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('heading', { name: 'Adicionar item' })).toBeVisible();
}

async function selecionarProduto(page: Page, nome: string): Promise<void> {
  await page.getByLabel('Produto').fill(nome);
  await page.getByRole('listbox').getByRole('button', { name: new RegExp(nome, 'i') }).first().click();
}

async function adicionarItem(
  page: Page,
  produto: string,
  quantidade = '1',
  observacao = '',
): Promise<void> {
  await abrirAdicao(page);
  await selecionarProduto(page, produto);
  await page.getByLabel('Quantidade').fill(quantidade);
  await page.getByLabel('Observação').fill(observacao);
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('heading', { name: produto, exact: true })).toBeVisible();
}

test('ITEM-001 - Inclusão válida aplica padrões, snapshots e persiste após recarga.', async ({ page }) => {
  await cadastrar(page, 'item001');
  await criarProduto(page, 'Arroz');
  await criarLista(page, 'Lista principal');
  await adicionarItem(page, 'Arroz', '2,5', 'Pacote azul');
  const item = page.getByRole('heading', { name: 'Arroz', exact: true }).locator('..').locator('..');
  await expect(item).toContainText('2,5 unidade');
  await expect(item).toContainText('Pacote azul');
  await expect(page.getByRole('checkbox', { name: 'Marcar Arroz' })).not.toBeChecked();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Arroz', exact: true })).toBeVisible();
});

test('ITEM-002 - Texto digitado sem seleção não cria item e preserva os campos.', async ({ page }) => {
  await cadastrar(page, 'item002');
  await criarProduto(page, 'Feijão');
  await criarLista(page, 'Lista sem seleção');
  await abrirAdicao(page);
  await page.getByLabel('Produto').fill('Feijão');
  await page.getByLabel('Quantidade').fill('3');
  await page.getByLabel('Observação').fill('Preservar');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByText('Selecione um produto válido na lista de sugestões.')).toBeVisible();
  await expect(page.getByLabel('Quantidade')).toHaveValue('3');
  await expect(page.getByLabel('Observação')).toHaveValue('Preservar');
});

test('ITEM-003 - Quantidades inválidas são recusadas sem alteração parcial.', async ({ page }) => {
  await cadastrar(page, 'item003');
  await criarProduto(page, 'Macarrão');
  await criarLista(page, 'Lista inválida');
  await abrirAdicao(page);
  await selecionarProduto(page, 'Macarrão');
  for (const quantidade of ['', '0', '-1', 'abc']) {
    await page.getByLabel('Quantidade').fill(quantidade);
    await page.getByRole('button', { name: 'Adicionar item' }).click();
    await expect(page.getByText('Informe uma quantidade maior que zero.')).toBeVisible();
  }
  await page.getByLabel('Quantidade').fill('1000000');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByText('A quantidade deve ser menor ou igual a 999999,99.')).toBeVisible();
});

test('ITEM-004 - Sugestões mostram no máximo dez produtos próprios e ativos.', async ({ page }) => {
  test.setTimeout(90_000);
  await cadastrar(page, 'item004');
  for (let indice = 1; indice <= 11; indice++) await criarProduto(page, `Produto teste ${indice}`);
  await criarProduto(page, 'Produto teste inativo');
  await page.getByTestId('product').filter({ hasText: 'Produto teste inativo' })
    .getByRole('button', { name: 'Desativar' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Desativar' }).click();
  await criarLista(page, 'Lista sugestões');
  await abrirAdicao(page);
  await page.getByLabel('Produto').fill('produto teste');
  await expect(page.getByRole('listbox').getByRole('button')).toHaveCount(10);
  await expect(page.getByRole('listbox')).not.toContainText('Produto teste inativo');
});

test('ITEM-005 - Duplicata permite cancelar, editar existente ou somar.', async ({ page }) => {
  await cadastrar(page, 'item005');
  await criarProduto(page, 'Açúcar');
  await criarLista(page, 'Lista duplicata');
  await adicionarItem(page, 'Açúcar', '2');
  await adicionarDuplicata(page, 'Açúcar', '3');
  const dialogo = page.getByRole('dialog', { name: 'Produto já está na lista' });
  await dialogo.getByRole('button', { name: 'Cancelar' }).click();
  await expect(dialogo).toHaveCount(0);
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(dialogo).toBeVisible();
  await dialogo.getByRole('button', { name: 'Editar existente' }).click();
  await expect(page.getByRole('heading', { name: 'Editar item' })).toBeVisible();
  await page.getByRole('button', { name: 'Cancelar' }).click();
  await abrirAdicao(page);
  await selecionarProduto(page, 'Açúcar');
  await page.getByLabel('Quantidade').fill('3');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await dialogo.getByRole('button', { name: 'Somar quantidade' }).click();
  await expect(page.getByRole('heading', { name: 'Açúcar', exact: true })).toBeVisible();
  await expect(page.getByText('5 unidade')).toBeVisible();
});

async function adicionarDuplicata(page: Page, produto: string, quantidade: string): Promise<void> {
  await abrirAdicao(page);
  await selecionarProduto(page, produto);
  await page.getByLabel('Quantidade').fill(quantidade);
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('dialog', { name: 'Produto já está na lista' })).toBeVisible();
}

test('ITEM-006 - Unidades diferentes impedem soma com orientação polida.', async ({ page }) => {
  await cadastrar(page, 'item006');
  await criarProduto(page, 'Farinha');
  await criarLista(page, 'Lista unidades');
  await adicionarItem(page, 'Farinha', '1');
  await abrirAdicao(page);
  await selecionarProduto(page, 'Farinha');
  await page.getByLabel('Quantidade').fill('1');
  await page.getByLabel('Unidade').fill('KILOGRAM');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Produto já está na lista' });
  await expect(dialogo.getByRole('button', { name: 'Somar quantidade' })).toBeDisabled();
  await expect(dialogo).toContainText('Para somar, selecione a mesma unidade do item existente.');
});

test('ITEM-009 - Remoção pode ser cancelada e depois confirmada uma única vez.', async ({ page }) => {
  await cadastrar(page, 'item009');
  await criarProduto(page, 'Oleo');
  await criarLista(page, 'Lista remoção');
  await adicionarItem(page, 'Oleo');
  await page.getByRole('button', { name: 'Remover' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
  await expect(page.getByRole('heading', { name: 'Oleo', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Remover' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Remover' }).click();
  await expect(page.getByRole('heading', { name: 'Oleo', exact: true })).toHaveCount(0);
  await expect(page.getByText('Total 0')).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Oleo', exact: true })).toHaveCount(0);
});

test('ITEM-012 - Edição concorrente recusa a segunda e oferece recarga.', async ({ page, context }) => {
  await cadastrar(page, 'item012');
  await criarProduto(page, 'Concorrente');
  await criarLista(page, 'Lista concorrente');
  await adicionarItem(page, 'Concorrente');
  const editarUrl = await page.getByRole('link', { name: 'Editar', exact: true }).getAttribute('href');
  if (!editarUrl) throw new Error('Destino de edição indisponível.');
  const segunda = await context.newPage();
  await page.goto(editarUrl);
  await segunda.goto(editarUrl);
  await expect(page.getByLabel('Quantidade')).toHaveValue('1');
  await expect(segunda.getByLabel('Quantidade')).toHaveValue('1');
  await page.getByLabel('Quantidade').fill('2');
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('heading', { name: 'Concorrente', exact: true })).toBeVisible();
  await segunda.getByLabel('Quantidade').fill('3');
  await segunda.getByRole('button', { name: 'Salvar' }).click();
  await expect(segunda.getByRole('alert')).toContainText(
    'Este item foi alterado em outro lugar. Recarregue os dados para continuar.',
  );
  await expect(segunda.getByRole('button', { name: 'Recarregar dados' })).toBeVisible();
  await segunda.close();
});

test('ITEM-007 - Editar item marcado preserva integralmente sua marcação.', async ({ page }) => {
  await cadastrar(page, 'item007');
  await criarProduto(page, 'Biscoito');
  await criarLista(page, 'Lista marcada');
  await adicionarItem(page, 'Biscoito', '1');
  await page.getByRole('checkbox', { name: 'Marcar Biscoito' }).check();
  await expect(page.getByRole('checkbox', { name: 'Marcar Biscoito' })).toBeChecked();
  await page.getByRole('link', { name: 'Editar', exact: true }).click();
  await expect(page.getByLabel('Quantidade')).toHaveValue('1');
  await page.getByLabel('Quantidade').fill('4');
  await page.getByLabel('Observação').fill('Editado');
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('checkbox', { name: 'Marcar Biscoito' })).toBeChecked();
  await expect(page.getByText('4 unidade · Editado')).toBeVisible();
  await page.reload();
  await expect(page.getByRole('checkbox', { name: 'Marcar Biscoito' })).toBeChecked();
});
