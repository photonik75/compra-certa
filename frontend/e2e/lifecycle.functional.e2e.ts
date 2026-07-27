import { Browser, expect, Page, test } from '@playwright/test';

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

async function criarLista(page: Page, nome: string): Promise<void> {
  await page.goto('/listas/nova');
  await page.getByLabel('Nome da lista').fill(nome);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page.getByRole('heading', { name: nome })).toBeVisible();
}

async function criarProdutoEItem(page: Page, produto = 'Arroz'): Promise<void> {
  const listaUrl = page.url();
  await page.goto('/produtos');
  await page.getByRole('button', { name: 'Novo produto' }).first().click();
  const dialogo = page.getByRole('dialog');
  await dialogo.getByLabel('Nome').fill(produto);
  const categoria = await dialogo.getByLabel('Categoria padrão').locator('option')
    .filter({ hasText: 'Mercearia' }).getAttribute('value');
  await dialogo.getByLabel('Categoria padrão').selectOption(categoria!);
  await dialogo.getByLabel('Unidade padrão').selectOption('UNIT');
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Produto criado com sucesso.');
  await page.goto(listaUrl);
  const adicionarUrl = await page.getByRole('link', { name: 'Adicionar item' }).getAttribute('href');
  await page.goto(adicionarUrl!);
  await page.getByLabel('Produto').fill(produto);
  await page.getByRole('listbox').getByRole('button', { name: new RegExp(produto) }).click();
  await page.getByLabel('Quantidade').fill('1');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await expect(page.getByRole('heading', { name: produto, exact: true })).toBeVisible();
}

async function concluir(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Concluir lista' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click();
  await expect(page.getByText('Lista concluída com sucesso.', { exact: true })).toBeVisible();
}

async function preparar(page: Page, id: string, nome: string, comItem = false): Promise<void> {
  await cadastrar(page, id);
  await criarLista(page, nome);
  if (comItem) await criarProdutoEItem(page);
}

async function compartilhar(page: Page, browser: Browser, id: string) {
  const contexto = await browser.newContext();
  const participante = await contexto.newPage();
  const email = await cadastrar(participante, id);
  await page.getByRole('link', { name: /Compartilhar/ }).click();
  await page.getByLabel('Convidar participante').fill(email);
  await page.getByRole('button', { name: 'Convidar' }).click();
  await expect(page.getByRole('status')).toContainText('Participante adicionado');
  return { contexto, participante };
}

test('LIFE-001 - Concluir informa pendências e ativa o modo somente leitura.', async ({ page }) => {
  await preparar(page, 'life001', 'Feira semanal', true);
  await page.getByRole('button', { name: 'Concluir lista' }).click();
  await expect(page.getByRole('dialog')).toContainText('1 itens pendentes');
  await page.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click();
  await expect(page.getByText(/Concluída em/)).toBeVisible();
  await expect(page.getByRole('link', { name: 'Adicionar item' })).toHaveCount(0);
  await expect(page.getByRole('checkbox')).toBeDisabled();
});

test('LIFE-002 - Listas vazia e totalmente comprada preservam seus resumos.', async ({ page }) => {
  await preparar(page, 'life002', 'Lista vazia');
  await concluir(page);
  await expect(page.getByText('Total 0')).toBeVisible();
  await criarLista(page, 'Lista totalmente comprada');
  await criarProdutoEItem(page, 'Leite');
  await page.getByRole('checkbox', { name: 'Marcar Leite' }).check();
  await expect(page.getByRole('alert')).toContainText('Item marcado');
  await concluir(page);
  await expect(page.getByText('Progresso da compra 100%')).toBeVisible();
});

test('LIFE-003 - Cancelar e Escape preservam estado e devolvem foco.', async ({ page }) => {
  await preparar(page, 'life003', 'Cancelar ciclo');
  const botao = page.getByRole('button', { name: 'Concluir lista' });
  await botao.click();
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
  await expect(botao).toBeFocused();
  await botao.click();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(botao).toBeFocused();
});

test('LIFE-004 - Lista concluída mantém consulta e bloqueia mutações.', async ({ page }) => {
  await preparar(page, 'life004', 'Histórico bloqueado', true);
  await concluir(page);
  await expect(page.getByRole('heading', { name: 'Arroz' })).toBeVisible();
  await expect(page.getByRole('link', { name: /Editar lista|Adicionar item/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Remover' })).toHaveCount(0);
});

test('LIFE-005 - Reabrir preserva conteúdo e devolve ações.', async ({ page }) => {
  await preparar(page, 'life005', 'Lista reaberta', true);
  await concluir(page);
  await page.getByRole('button', { name: 'Reabrir' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Reabrir' }).click();
  await expect(page.getByRole('alert')).toContainText('Lista reaberta com sucesso.');
  await expect(page.getByRole('heading', { name: 'Arroz' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Adicionar item' })).toBeVisible();
});

test('LIFE-006 - Participante não recebe ações exclusivas de ciclo de vida.', async ({ page, browser }) => {
  await preparar(page, 'life006-owner', 'Lista participada');
  const { contexto, participante } = await compartilhar(page, browser, 'life006-member');
  await participante.goto('/listas');
  await participante.getByTestId('list-card').filter({ hasText: 'Lista participada' })
    .getByRole('button', { name: 'Abrir' }).click();
  await expect(participante.getByRole('button', { name: /Concluir lista|Reabrir|Excluir/ })).toHaveCount(0);
  await contexto.close();
});

test('LIFE-007 - Excluir revoga o acesso de todos os contextos.', async ({ page, browser }) => {
  await preparar(page, 'life007-owner', 'Lista descartada');
  const { contexto, participante } = await compartilhar(page, browser, 'life007-member');
  await participante.goto('/listas');
  await participante.getByTestId('list-card').filter({ hasText: 'Lista descartada' })
    .getByRole('button', { name: 'Abrir' }).click();
  await page.goBack();
  await concluir(page);
  await page.getByRole('button', { name: 'Excluir' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
  await expect(page).toHaveURL(/\/listas$/);
  await participante.reload();
  if (!/\/listas$/.test(participante.url())) {
    await expect(participante.getByRole('alert')).toContainText('Lista não encontrada');
  }
  await expect(participante.getByText('Lista descartada')).toHaveCount(0);
  await contexto.close();
});

test('LIFE-008 - Exclusão concorrente produz uma única ausência persistida.', async ({ page, context }) => {
  await preparar(page, 'life008', 'Exclusão única');
  await concluir(page);
  const segunda = await context.newPage();
  await segunda.goto(page.url());
  await page.getByRole('button', { name: 'Excluir' }).click();
  await segunda.getByRole('button', { name: 'Excluir' }).click();
  await Promise.all([
    page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click(),
    segunda.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click(),
  ]);
  await page.goto('/listas');
  await expect(page.getByText('Exclusão única')).toHaveCount(0);
  await segunda.close();
});

test('LIFE-009 - Transições concorrentes não criam estado intermediário.', async ({ page, context }) => {
  await preparar(page, 'life009', 'Concorrência de estado');
  const segunda = await context.newPage();
  await segunda.goto(page.url());
  await page.getByRole('button', { name: 'Concluir lista' }).click();
  await segunda.getByRole('button', { name: 'Concluir lista' }).click();
  await Promise.all([
    page.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click(),
    segunda.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click(),
  ]);
  await page.reload();
  await expect(page.getByRole('button', { name: 'Reabrir' })).toBeVisible();
  await segunda.close();
});

test('LIFE-010 - Histórico concluído permanece íntegro após recarga.', async ({ page }) => {
  await preparar(page, 'life010', 'Histórico conhecido', true);
  await page.getByRole('checkbox', { name: 'Marcar Arroz' }).check();
  await expect(page.getByRole('alert')).toContainText('Item marcado');
  await concluir(page);
  await page.reload();
  await expect(page.getByText(/Concluída em/)).toBeVisible();
  await expect(page.getByText('Comprados 1')).toBeVisible();
  await expect(page.getByText(/Marcado por Pessoa de Teste/)).toBeVisible();
  await expect(page.getByRole('checkbox')).toBeDisabled();
});
