import { expect, Page, test } from '@playwright/test';

const SENHA = 'Senha segura 123';
const NOME = 'Nome da lista';
const DESCRICAO = 'Descrição ou observação';

function emailUnico(cenario: string): string {
  return `${cenario}.${Date.now()}.${Math.random().toString(36).slice(2)}@example.com`;
}

async function cadastrar(page: Page, cenario: string, email = emailUnico(cenario)): Promise<string> {
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(SENHA);
  await page.getByLabel('Confirmar senha').fill(SENHA);
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
  return email;
}

async function sair(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
}

async function entrar(page: Page, email: string): Promise<void> {
  await page.goto('/entrar');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(SENHA);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
}

async function abrirNovaLista(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Nova lista' }).click();
  await expect(page.getByRole('heading', { name: 'Nova lista' })).toBeVisible();
}

async function preencherLista(page: Page, nome: string, descricao = ''): Promise<void> {
  await page.getByLabel(NOME).fill(nome);
  await page.getByLabel(DESCRICAO).fill(descricao);
}

async function criarLista(page: Page, nome: string, descricao = ''): Promise<void> {
  await abrirNovaLista(page);
  await preencherLista(page, nome, descricao);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page).toHaveURL(/\/listas\/[^/]+$/);
  await expect(page.getByRole('heading', { name: 'Detalhe da lista' })).toBeVisible();
}

async function voltarParaListas(page: Page): Promise<void> {
  await page.goto('/listas');
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
}

test('LIST-001 - Duplo clique cria uma única lista ativa, vazia e persistente.', async ({ page }) => {
  await cadastrar(page, 'list001');
  await abrirNovaLista(page);
  await preencherLista(page, 'Compras da semana', 'Itens essenciais');
  await page.getByRole('button', { name: 'Salvar lista' }).dblclick();
  await expect(page).toHaveURL(/\/listas\/[^/]+$/);
  await expect(page.getByText('Nenhum item na lista.')).toBeVisible();
  await voltarParaListas(page);
  await expect(page.getByTestId('list-card').filter({ hasText: 'Compras da semana' })).toHaveCount(1);
  await page.reload();
  await expect(page.getByTestId('list-card').filter({ hasText: 'Compras da semana' })).toHaveCount(1);
});

test('LIST-002 - Campos inválidos preservam o formulário e não persistem alterações.', async ({ page }) => {
  await cadastrar(page, 'list002');
  await criarLista(page, 'Compras do mês');
  await voltarParaListas(page);
  await abrirNovaLista(page);
  const salvar = page.getByRole('button', { name: 'Salvar lista' });
  await salvar.click();
  await expect(page.getByText('Por favor, informe o nome da lista.')).toBeVisible();
  await preencherLista(page, 'x'.repeat(61), 'Descrição preservada');
  await salvar.click();
  await expect(page.getByText('O nome da lista deve ter no máximo 60 caracteres.')).toBeVisible();
  await expect(page.getByLabel(DESCRICAO)).toHaveValue('Descrição preservada');
  await preencherLista(page, 'Compras do mês', 'Descrição preservada');
  await salvar.click();
  await expect(page.getByText('Você já possui uma lista com este nome.')).toBeVisible();
  await page.getByLabel(NOME).fill('Outra lista');
  await page.getByLabel(DESCRICAO).fill('x'.repeat(241));
  await salvar.click();
  await expect(page.getByText('A descrição deve ter no máximo 240 caracteres.')).toBeVisible();
  await voltarParaListas(page);
  await expect(page.getByTestId('list-card')).toHaveCount(1);
});

test('LIST-006 - Estados vazios apresentam mensagens e ações específicas.', async ({ page }) => {
  await cadastrar(page, 'list006');
  await expect(page.getByText(
    'Você ainda não possui listas. Crie sua primeira lista para começar.',
  )).toBeVisible();
  await expect(page.getByRole('link', { name: 'Criar lista' })).toBeVisible();
  await page.getByRole('button', { name: 'Concluídas' }).click();
  await expect(page.getByText('Você não possui listas concluídas.')).toBeVisible();
  await page.getByRole('button', { name: 'Ativas' }).click();
  await page.getByLabel('Pesquisar listas').fill('inexistente');
  await expect(page.getByText('Nenhuma lista encontrada para esta pesquisa.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Limpar pesquisa' })).toBeVisible();
  await expect(page.getByTestId('list-card')).toHaveCount(0);
});

test('LIST-007 - Edição persiste nome e descrição após recarga.', async ({ page }) => {
  await cadastrar(page, 'list007');
  await criarLista(page, 'Lista antiga', 'Descrição antiga');
  await voltarParaListas(page);
  const cartao = page.getByTestId('list-card').filter({ hasText: 'Lista antiga' });
  await cartao.getByRole('link', { name: 'Editar' }).click();
  await expect(page.getByLabel(NOME)).toHaveValue('Lista antiga');
  await expect(page.getByLabel(DESCRICAO)).toHaveValue('Descrição antiga');
  await page.getByLabel(NOME).fill('Lista atualizada');
  await page.getByLabel(DESCRICAO).fill('Descrição atualizada');
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page).toHaveURL(/\/listas\/[^/]+$/);
  await expect(page.getByRole('heading', { name: 'Detalhe da lista' })).toBeVisible();
  await voltarParaListas(page);
  await page.reload();
  await expect(page.getByTestId('list-card').filter({ hasText: 'Lista atualizada' })).toBeVisible();
  await expect(page.getByText('Lista antiga')).toHaveCount(0);
});

test('LIST-009 - Cancelar criação e edição não persiste dados.', async ({ page }) => {
  await cadastrar(page, 'list009');
  await abrirNovaLista(page);
  await preencherLista(page, 'Não criar', 'Não persistir');
  await page.getByRole('link', { name: 'Cancelar' }).click();
  await expect(page.getByText('Você ainda não possui listas.')).toBeVisible();
  await criarLista(page, 'Lista original', 'Descrição original');
  await voltarParaListas(page);
  await page.getByTestId('list-card').getByRole('link', { name: 'Editar' }).click();
  await expect(page.getByLabel(NOME)).toHaveValue('Lista original');
  await preencherLista(page, 'Nome descartado', 'Descrição descartada');
  await page.getByRole('link', { name: 'Cancelar' }).click();
  await voltarParaListas(page);
  await expect(page.getByText('Lista original')).toBeVisible();
  await expect(page.getByText('Nome descartado')).toHaveCount(0);
});

test('LIST-005 - Pesquisa ignora caixa e acentos e permanece combinada ao filtro.', async ({ page }) => {
  await cadastrar(page, 'list005');
  for (const nome of ['Farmácia', 'FARMACIA antiga', 'Mercado']) {
    await criarLista(page, nome);
    await voltarParaListas(page);
  }
  await page.getByLabel('Pesquisar listas').fill('farmacia');
  await expect(page.getByTestId('list-card')).toHaveCount(2);
  await expect(page.getByText('Farmácia', { exact: true })).toBeVisible();
  await expect(page.getByText('FARMACIA antiga', { exact: true })).toBeVisible();
  await expect(page.getByText('Mercado', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Concluídas' }).click();
  await expect(page.getByText('Nenhuma lista encontrada para esta pesquisa.')).toBeVisible();
  await page.getByRole('button', { name: 'Ativas' }).click();
  await expect(page.getByTestId('list-card')).toHaveCount(2);
});

test('LIST-012 - Recurso inexistente exibe indisponibilidade sem revelar detalhes.', async ({ page }) => {
  await cadastrar(page, 'list012');
  await page.goto('/listas/00000000-0000-0000-0000-000000000000');
  await expect(page.getByRole('alert').filter({
    hasText: 'Lista não encontrada ou indisponível para sua conta.',
  })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Adicionar item' })).toHaveCount(0);
});

test('LIST-003 - Participante pode criar lista própria com o mesmo nome.', async ({ page }) => {
  const participante = await cadastrar(page, 'list003-participante');
  await sair(page);
  const proprietario = await cadastrar(page, 'list003-proprietario');
  await criarLista(page, 'Viagem');
  await page.getByRole('link', { name: 'Compartilhar lista' }).click();
  await page.getByLabel('Convidar participante').fill(participante);
  await page.getByRole('button', { name: 'Convidar' }).click();
  await expect(page.getByRole('status')).toContainText('Participante adicionado com sucesso.');
  await page.goto('/listas');
  await sair(page);
  await entrar(page, participante);
  await expect(page.getByTestId('list-card').filter({ hasText: 'Viagem' })).toContainText('Participante');
  await criarLista(page, 'Viagem');
  await voltarParaListas(page);
  const cartoes = page.getByTestId('list-card').filter({ hasText: 'Viagem' });
  await expect(cartoes).toHaveCount(2);
  await expect(cartoes.filter({ hasText: 'Proprietário' })).toHaveCount(1);
  await expect(cartoes.filter({ hasText: 'Participante' })).toHaveCount(1);
  expect(proprietario).not.toBe(participante);
});

test('LIST-008 - Participante não visualiza nem acessa a edição da lista compartilhada.', async ({ page }) => {
  const participante = await cadastrar(page, 'list008-participante');
  await sair(page);
  await cadastrar(page, 'list008-proprietario');
  await criarLista(page, 'Lista compartilhada', 'Dados preservados');
  await page.getByRole('link', { name: 'Compartilhar lista' }).click();
  await page.getByLabel('Convidar participante').fill(participante);
  await page.getByRole('button', { name: 'Convidar' }).click();
  await expect(page.getByRole('status')).toContainText('Participante adicionado com sucesso.');
  await page.goto('/listas');
  await sair(page);
  await entrar(page, participante);
  const cartao = page.getByTestId('list-card').filter({ hasText: 'Lista compartilhada' });
  await expect(cartao.getByRole('link', { name: 'Editar' })).toHaveCount(0);
  await cartao.getByRole('button', { name: 'Abrir' }).click();
  await expect(page.getByText('Participante', { exact: true })).toBeVisible();
  const detalheUrl = page.url();
  await page.goto(`${detalheUrl}/editar`);
  await expect(page.getByRole('alert')).toContainText(
    'Lista não encontrada ou indisponível para sua conta.',
  );
  await voltarParaListas(page);
  await expect(page.getByText('Lista compartilhada')).toBeVisible();
});

test('LIST-004 - Listagem inicia nas ativas, ordenada e com resumos consistentes.', async ({ page }) => {
  await cadastrar(page, 'list004');
  for (const nome of ['Lista mais antiga', 'Lista intermediária', 'Lista mais recente']) {
    await criarLista(page, nome);
    await voltarParaListas(page);
  }
  await expect(page.getByRole('button', { name: 'Ativas' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText('3 listas ativas · 0 itens pendentes')).toBeVisible();
  const cartoes = page.getByTestId('list-card');
  await expect(cartoes).toHaveCount(3);
  await expect(cartoes.nth(0)).toContainText('Lista mais recente');
  await expect(cartoes.nth(1)).toContainText('Lista intermediária');
  await expect(cartoes.nth(2)).toContainText('Lista mais antiga');
  for (let indice = 0; indice < 3; indice++) {
    await expect(cartoes.nth(indice)).toContainText('Proprietário');
    await expect(cartoes.nth(indice)).toContainText('0 itens · 0 pendentes · 0%');
  }
});

test('LIST-010 - Edição concorrente preserva a primeira alteração e oferece recarga.', async ({
  page,
  context,
}) => {
  await cadastrar(page, 'list010');
  await criarLista(page, 'Lista concorrente', 'Versão inicial');
  await voltarParaListas(page);
  const editarUrl = await page.getByTestId('list-card').getByRole('link', { name: 'Editar' })
    .getAttribute('href');
  if (!editarUrl) throw new Error('A ação de edição não apresentou destino.');
  const segundoContexto = await context.newPage();
  await page.goto(editarUrl);
  await segundoContexto.goto(editarUrl);
  await expect(page.getByLabel(NOME)).toHaveValue('Lista concorrente');
  await expect(segundoContexto.getByLabel(NOME)).toHaveValue('Lista concorrente');
  await page.getByLabel(NOME).fill('Alteração preservada');
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page.getByRole('heading', { name: 'Detalhe da lista' })).toBeVisible();
  await segundoContexto.getByLabel(NOME).fill('Alteração obsoleta');
  await segundoContexto.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(segundoContexto.getByRole('alert')).toContainText(
    'Esta lista foi alterada em outro lugar. Recarregue os dados para continuar.',
  );
  await expect(segundoContexto.getByRole('button', { name: 'Recarregar dados' })).toBeVisible();
  await segundoContexto.getByRole('button', { name: 'Recarregar dados' }).click();
  await expect(segundoContexto.getByLabel(NOME)).toHaveValue('Alteração preservada');
  await segundoContexto.close();
});

test('LIST-011 - Pesquisa alcança lista após a primeira página sem duplicá-la.', async ({ page }) => {
  test.setTimeout(120_000);
  await cadastrar(page, 'list011');
  await criarLista(page, 'Alvo distante');
  await voltarParaListas(page);
  for (let indice = 1; indice <= 30; indice++) {
    await criarLista(page, `Lista ${indice.toString().padStart(2, '0')}`);
    await voltarParaListas(page);
  }
  await expect(page.getByTestId('list-card')).toHaveCount(30);
  await expect(page.getByText('Alvo distante', { exact: true })).toHaveCount(0);
  await page.getByLabel('Pesquisar listas').fill('alvo distante');
  const resultado = page.getByTestId('list-card').filter({ hasText: 'Alvo distante' });
  await expect(resultado).toHaveCount(1);
  await page.getByRole('button', { name: 'Todas' }).click();
  await expect(resultado).toHaveCount(1);
});
