import { expect, Page, test } from '@playwright/test';

const SENHA = 'Senha segura 123';

async function cadastrar(page: Page, id: string): Promise<string> {
  const email = `${id}.${Date.now()}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(SENHA);
  await page.getByLabel('Confirmar senha').fill(SENHA);
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
  return email;
}

async function criarProduto(page: Page, nome: string): Promise<void> {
  await page.goto('/produtos');
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByLabel('Nome').fill(nome);
  const categoria = await dialogo.getByLabel('Categoria padrão').locator('option')
    .filter({ hasText: 'Mercearia' }).getAttribute('value');
  if (!categoria) throw new Error('Categoria indisponível.');
  await dialogo.getByLabel('Categoria padrão').selectOption(categoria);
  await dialogo.getByLabel('Unidade padrão').selectOption('UNIT');
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto criado com sucesso.');
}

async function criarLista(page: Page, nome: string): Promise<void> {
  await page.goto('/listas');
  await page.getByRole('link', { name: 'Nova lista' }).click();
  await page.getByLabel('Nome da lista').fill(nome);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page.getByText('Sua lista ainda está vazia.')).toBeVisible();
}

async function adicionar(page: Page, produto: string, quantidade = '1'): Promise<void> {
  await page.getByRole('link', { name: 'Adicionar item' }).click();
  await page.getByLabel('Produto').fill(produto);
  await page.getByRole('listbox').getByRole('button', { name: new RegExp(produto) }).click();
  await page.getByLabel('Quantidade').fill(quantidade);
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('heading', { name: produto, exact: true })).toBeVisible();
}

async function prepararItens(page: Page, quantidade: number): Promise<void> {
  for (let indice = 1; indice <= quantidade; indice++) await criarProduto(page, `Produto ${indice}`);
  await criarLista(page, 'Compra');
  for (let indice = 1; indice <= quantidade; indice++) await adicionar(page, `Produto ${indice}`, `${indice}`);
}

test('SHOP-001 - Resumo representa oito itens, três comprados e percentual arredondado.', async ({ page }) => {
  test.setTimeout(90_000);
  await cadastrar(page, 'shop001');
  await prepararItens(page, 8);
  for (let indice = 1; indice <= 3; indice++) {
    await page.getByRole('checkbox', { name: `Marcar Produto ${indice}` }).check();
  }
  await expect(page.getByText('Total 8')).toBeVisible();
  await expect(page.getByText('Comprados 3')).toBeVisible();
  await expect(page.getByText('Pendentes 5')).toBeVisible();
  await expect(page.getByText('Progresso da compra 38%')).toBeVisible();
});

test('SHOP-002 - Marcar e desmarcar persistem estado, resumo e autoria.', async ({ page }) => {
  await cadastrar(page, 'shop002');
  await criarProduto(page, 'Leite');
  await criarLista(page, 'Compra persistente');
  await adicionar(page, 'Leite');
  const caixa = page.getByRole('checkbox', { name: 'Marcar Leite' });
  await caixa.check();
  await expect(caixa).toBeChecked();
  await expect(page.getByText(/Marcado por Pessoa de Teste/)).toBeVisible();
  await page.reload();
  await expect(caixa).toBeChecked();
  await caixa.uncheck();
  await expect(page.getByRole('alert')).toContainText('Item desmarcado com sucesso.');
  await page.reload();
  await expect(caixa).not.toBeChecked();
  await expect(page.getByText(/Marcado por/)).toHaveCount(0);
});

test('SHOP-003 - Reabrir item já marcado não altera autoria nem resumo.', async ({ page }) => {
  await cadastrar(page, 'shop003');
  await criarProduto(page, 'Café');
  await criarLista(page, 'Compra idempotente');
  await adicionar(page, 'Café');
  await page.getByRole('checkbox', { name: 'Marcar Café' }).check();
  const autoria = await page.getByText(/Marcado por Pessoa de Teste/).textContent();
  await page.reload();
  await expect(page.getByRole('checkbox', { name: 'Marcar Café' })).toBeChecked();
  await expect(page.getByText(/Marcado por Pessoa de Teste/)).toHaveText(autoria ?? '');
  await expect(page.getByText('Comprados 1')).toBeVisible();
});

test('SHOP-004 - Lista vazia apresenta resumo zerado e ação de inclusão.', async ({ page }) => {
  await cadastrar(page, 'shop004');
  await criarLista(page, 'Compra vazia');
  await expect(page.getByText('Total 0')).toBeVisible();
  await expect(page.getByText('Comprados 0')).toBeVisible();
  await expect(page.getByText('Pendentes 0')).toBeVisible();
  await expect(page.getByText('Progresso da compra 0%')).toBeVisible();
  await expect(page.getByText('Sua lista ainda está vazia.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Adicionar item' })).toBeVisible();
});

test('SHOP-005 - Recolher grupo é uma preferência local do contexto.', async ({ page, context }) => {
  await cadastrar(page, 'shop005');
  await criarProduto(page, 'Arroz');
  await criarLista(page, 'Compra agrupada');
  await adicionar(page, 'Arroz');
  const grupo = page.locator('details').filter({ hasText: 'Mercearia' });
  await expect(grupo).toHaveAttribute('open', '');
  await grupo.locator('summary').click();
  await expect(grupo).not.toHaveAttribute('open', '');
  const segunda = await context.newPage();
  await segunda.goto(page.url());
  await expect(segunda.locator('details').filter({ hasText: 'Mercearia' })).toHaveAttribute('open', '');
  await segunda.close();
});

test('SHOP-006 - Proprietário e participante recebem alterações colaborativas.', async ({
  page,
  browser,
}) => {
  test.setTimeout(60_000);
  const contextoParticipante = await browser.newContext();
  const participante = await contextoParticipante.newPage();
  const emailParticipante = await cadastrar(participante, 'shop006-participante');
  await criarProduto(participante, 'Produto do participante');
  await cadastrar(page, 'shop006-proprietario');
  await criarProduto(page, 'Produto do proprietário');
  await criarLista(page, 'Compra compartilhada');
  await adicionar(page, 'Produto do proprietário');
  await page.getByRole('link', { name: /Compartilhar/ }).click();
  await page.getByLabel('Convidar participante').fill(emailParticipante);
  await page.getByRole('button', { name: 'Convidar' }).click();
  await expect(page.getByRole('status')).toContainText(/convite|participante/i);
  await participante.goto('/listas');
  const cartao = participante.getByTestId('list-card').filter({ hasText: 'Compra compartilhada' });
  await cartao.getByRole('button', { name: 'Abrir' }).click();
  await page.goBack();
  await expect(page.getByRole('checkbox', { name: 'Marcar Produto do proprietário' })).toBeVisible();
  await page.getByRole('checkbox', { name: 'Marcar Produto do proprietário' }).check();
  await expect(participante.getByRole('checkbox', {
    name: 'Marcar Produto do proprietário',
  })).toBeChecked({ timeout: 5_000 });
  await adicionar(participante, 'Produto do participante');
  await expect(page.getByRole('heading', {
    name: 'Produto do participante',
    exact: true,
  })).toBeVisible({ timeout: 5_000 });
  await contextoParticipante.close();
});

test('SHOP-007 - Contextos concorrentes convergem ao estado confirmado pelo servidor.', async ({
  page,
  context,
}) => {
  await cadastrar(page, 'shop007');
  await criarProduto(page, 'Sabão');
  await criarLista(page, 'Compra concorrente');
  await adicionar(page, 'Sabão');
  const segunda = await context.newPage();
  await segunda.goto(page.url());
  await expect(segunda.getByRole('checkbox', { name: 'Marcar Sabão' })).not.toBeChecked();
  await Promise.all([
    page.getByRole('checkbox', { name: 'Marcar Sabão' }).check(),
    segunda.getByRole('checkbox', { name: 'Marcar Sabão' }).check(),
  ]);
  await page.reload();
  await segunda.reload();
  await expect(page.getByRole('checkbox', { name: 'Marcar Sabão' })).toBeChecked();
  await expect(segunda.getByRole('checkbox', { name: 'Marcar Sabão' })).toBeChecked();
  await segunda.close();
});

test('SHOP-008 - Sem conexão impede marcação falsa e informa o estado.', async ({ page, context }) => {
  await cadastrar(page, 'shop008');
  await criarProduto(page, 'Farinha');
  await criarLista(page, 'Compra offline');
  await adicionar(page, 'Farinha');
  await context.setOffline(true);
  await expect(page.getByRole('status')).toContainText('Sem conexão');
  const caixa = page.getByRole('checkbox', { name: 'Marcar Farinha' });
  await expect(caixa).toBeDisabled();
  await expect(caixa).not.toBeChecked();
  await context.setOffline(false);
});

test('SHOP-009 - Reconexão ressincroniza antes de permitir nova marcação.', async ({ page, context }) => {
  await cadastrar(page, 'shop009');
  await criarProduto(page, 'Açúcar');
  await criarLista(page, 'Compra reconectada');
  await adicionar(page, 'Açúcar');
  await context.setOffline(true);
  await expect(page.getByRole('checkbox', { name: 'Marcar Açúcar' })).toBeDisabled();
  await context.setOffline(false);
  await expect(page.getByRole('status')).toContainText('Sincronizada');
  await page.getByRole('checkbox', { name: 'Marcar Açúcar' }).check();
  await expect(page.getByRole('alert')).toContainText('Item marcado com sucesso.');
  await page.reload();
  await expect(page.getByRole('checkbox', { name: 'Marcar Açúcar' })).toBeChecked();
});

test('SHOP-010 - Usuário alheio não acessa controles de compra.', async ({ page }) => {
  await cadastrar(page, 'shop010');
  await page.goto('/listas/00000000-0000-0000-0000-000000000000');
  await expect(page.getByRole('alert')).toContainText(
    'Lista não encontrada ou indisponível para sua conta.',
  );
  await expect(page.getByRole('checkbox')).toHaveCount(0);
});

test('SHOP-011 - Resumo final corresponde às mutações persistidas.', async ({ page }) => {
  await cadastrar(page, 'shop011');
  await criarProduto(page, 'Item A');
  await criarProduto(page, 'Item B');
  await criarLista(page, 'Compra final');
  await adicionar(page, 'Item A');
  await adicionar(page, 'Item B');
  await page.getByRole('checkbox', { name: 'Marcar Item A' }).check();
  const itemB = page.getByRole('heading', { name: 'Item B', exact: true }).locator('..').locator('..');
  await itemB.getByRole('button', { name: 'Remover' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Remover' }).click();
  await expect(page.getByRole('alert')).toContainText('Item removido com sucesso.');
  await page.reload();
  await expect(page.getByText('Total 1')).toBeVisible();
  await expect(page.getByText('Comprados 1')).toBeVisible();
  await expect(page.getByText('Pendentes 0')).toBeVisible();
  await expect(page.getByText('Progresso da compra 100%')).toBeVisible();
});
