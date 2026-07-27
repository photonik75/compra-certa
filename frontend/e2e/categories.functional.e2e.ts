import { expect, Page, test } from '@playwright/test';

const SENHA = 'Senha segura 123';

async function cadastrar(page: Page, cenario: string): Promise<void> {
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(`${cenario}.${Date.now()}@example.com`);
  await page.getByLabel('Senha', { exact: true }).fill(SENHA);
  await page.getByLabel('Confirmar senha').fill(SENHA);
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
  await page.goto('/categorias');
  await expect(page.getByRole('heading', { name: 'Categorias' })).toBeVisible();
}

async function abrirNova(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Nova categoria' }).first().click();
  await expect(page.getByRole('dialog')).toBeVisible();
}

async function criar(page: Page, nome: string, icone = 4): Promise<void> {
  await abrirNova(page);
  await page.getByRole('dialog').getByLabel('Nome').fill(nome);
  await page.getByTestId('icon-option').nth(icone).getByRole('radio').check();
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Categoria criada com sucesso.');
}

test('CAT-001 - Conta nova recebe exatamente as quatro categorias iniciais.', async ({ page }) => {
  await cadastrar(page, 'cat001');
  const categorias = page.getByTestId('category');
  await expect(categorias).toHaveCount(4);
  for (const [nome, icone] of [
    ['Bebidas', '🧃'], ['Hortifruti', '🥬'], ['Limpeza', '🧴'], ['Mercearia', '🛍️'],
  ]) {
    const categoria = categorias.filter({ hasText: nome });
    await expect(categoria).toContainText(icone);
    await expect(categoria).toContainText('0 produtos ativos');
  }
});

test('CAT-002 - Criação repetida gera uma categoria ordenada e persistente.', async ({ page }) => {
  await cadastrar(page, 'cat002');
  await abrirNova(page);
  await page.getByRole('dialog').getByLabel('Nome').fill('Padaria');
  await page.getByTestId('icon-option').nth(4).getByRole('radio').check();
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).dblclick();
  await expect(page.getByRole('status')).toContainText('Categoria criada com sucesso.');
  await expect(page.getByTestId('category').filter({ hasText: 'Padaria' })).toHaveCount(1);
  await page.reload();
  await expect(page.getByTestId('category').filter({ hasText: 'Padaria' })).toHaveCount(1);
  const nomes = await page.getByTestId('category').getByRole('heading').allTextContents();
  expect(nomes).toEqual([...nomes].sort((a, b) => a.localeCompare(b, 'pt-BR')));
});

test('CAT-003 - Dados inválidos preservam o diálogo e não criam categoria.', async ({ page }) => {
  await cadastrar(page, 'cat003');
  await abrirNova(page);
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('Por favor, informe o nome da categoria.')).toBeVisible();
  await expect(dialogo.getByText('Por favor, escolha um ícone disponível.')).toBeVisible();
  await dialogo.getByLabel('Nome').fill('x'.repeat(41));
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('O nome da categoria deve ter no máximo 40 caracteres.')).toBeVisible();
  await dialogo.getByLabel('Nome').fill('Bebidas');
  await page.getByTestId('icon-option').first().getByRole('radio').check();
  await dialogo.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogo.getByText('Você já possui uma categoria com este nome.')).toBeVisible();
  await expect(dialogo.getByLabel('Nome')).toHaveValue('Bebidas');
  await dialogo.getByRole('button', { name: 'Cancelar' }).click();
  await expect(page.getByTestId('category')).toHaveCount(4);
});

test('CAT-004 - Pesquisa ignora acentos e caixa e oferece limpeza no vazio.', async ({ page }) => {
  await cadastrar(page, 'cat004');
  await criar(page, 'Higiene');
  await criar(page, 'Grãos');
  await page.getByLabel('Pesquisar categorias').fill('GRAOS');
  await expect(page.getByTestId('category')).toHaveCount(1);
  await expect(page.getByText('Grãos', { exact: true })).toBeVisible();
  await page.getByLabel('Pesquisar categorias').fill('inexistente');
  await expect(page.getByText('Nenhuma categoria encontrada para esta pesquisa.')).toBeVisible();
  await page.getByRole('button', { name: 'Limpar pesquisa' }).click();
  await expect(page.getByTestId('category')).toHaveCount(6);
});

test('CAT-007 - Categoria sem produtos ativos pode ser excluída e permanece ausente.', async ({ page }) => {
  await cadastrar(page, 'cat007');
  await criar(page, 'Temporária');
  const categoria = page.getByTestId('category').filter({ hasText: 'Temporária' });
  await categoria.getByRole('button', { name: 'Excluir' }).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo).toContainText('Excluir a categoria ‘Temporária’?');
  await dialogo.getByRole('button', { name: 'Excluir' }).click();
  await expect(page.getByRole('status')).toContainText('Categoria excluída com sucesso.');
  await expect(categoria).toHaveCount(0);
  await page.reload();
  await expect(page.getByText('Temporária', { exact: true })).toHaveCount(0);
});

test('CAT-008 - Cancelar e Escape descartam alterações e devolvem o foco.', async ({ page }) => {
  await cadastrar(page, 'cat008');
  const nova = page.getByRole('button', { name: 'Nova categoria' }).first();
  await nova.click();
  await page.getByRole('dialog').getByLabel('Nome').fill('Não criar');
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
  await expect(nova).toBeFocused();
  await expect(page.getByText('Não criar')).toHaveCount(0);
  const bebidas = page.getByTestId('category').filter({ hasText: 'Bebidas' });
  const editar = bebidas.getByRole('button', { name: 'Editar' });
  await editar.click();
  await expect(page.getByRole('dialog').getByLabel('Nome')).toHaveValue('Bebidas');
  await page.getByRole('dialog').getByLabel('Nome').fill('Não alterar');
  await page.keyboard.press('Escape');
  await expect(editar).toBeFocused();
  await expect(page.getByText('Bebidas', { exact: true })).toBeVisible();
  await expect(page.getByText('Não alterar')).toHaveCount(0);
});

test('CAT-010 - Edição concorrente preserva a primeira mudança e oferece recarga.', async ({
  page,
  context,
}) => {
  await cadastrar(page, 'cat010');
  await criar(page, 'Concorrente');
  const segunda = await context.newPage();
  await segunda.goto('/categorias');
  const abrirEdicao = async (pagina: Page): Promise<void> => {
    const categoria = pagina.getByTestId('category').filter({ hasText: 'Concorrente' });
    await categoria.getByRole('button', { name: 'Editar' }).click();
    await expect(pagina.getByRole('dialog').getByLabel('Nome')).toHaveValue('Concorrente');
  };
  await abrirEdicao(page);
  await abrirEdicao(segunda);
  await page.getByRole('dialog').getByLabel('Nome').fill('Primeira mudança');
  await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('status')).toContainText('Categoria atualizada com sucesso.');
  await segunda.getByRole('dialog').getByLabel('Nome').fill('Mudança obsoleta');
  await segunda.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
  await expect(segunda.getByRole('status')).toContainText(
    'Esta categoria foi alterada em outro lugar. Recarregue os dados para continuar.',
  );
  await segunda.getByRole('button', { name: 'Recarregar dados' }).click();
  await expect(segunda.getByRole('dialog').getByLabel('Nome')).toHaveValue('Primeira mudança');
  await segunda.close();
});
