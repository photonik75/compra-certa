import { Browser, BrowserContext, expect, Page, test } from '@playwright/test';

const SENHA = 'Senha segura 123';

async function cadastrar(page: Page, id: string, email?: string): Promise<string> {
  const conta = email ?? `${id}.${Date.now()}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(conta);
  await page.getByLabel('Senha', { exact: true }).fill(SENHA);
  await page.getByLabel('Confirmar senha').fill(SENHA);
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByRole('heading', { name: 'Minhas listas' })).toBeVisible();
  return conta;
}

async function criarLista(page: Page, nome: string): Promise<void> {
  await page.goto('/listas/nova');
  await page.getByLabel('Nome da lista').fill(nome);
  await page.getByRole('button', { name: 'Salvar lista' }).click();
  await expect(page.getByRole('heading', { name: nome })).toBeVisible();
}

async function convidar(page: Page, email: string): Promise<void> {
  if (!/compartilhar/.test(page.url())) await page.getByRole('link', { name: /Compartilhar/ }).click();
  await page.getByLabel('Convidar participante').fill(email);
  await page.getByRole('button', { name: 'Convidar' }).click();
}

async function contaSeparada(browser: Browser, id: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  const email = await cadastrar(page, id);
  return { context, page, email };
}

async function abrirLista(page: Page, nome: string): Promise<void> {
  await page.goto('/listas');
  await page.getByTestId('list-card').filter({ hasText: nome })
    .getByRole('button', { name: 'Abrir' }).click();
}

async function sair(page: Page): Promise<void> {
  await page.goto('/listas');
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

async function ultimoLinkMailpit(context: BrowserContext, email: string): Promise<string> {
  const mail = await context.newPage();
  await mail.goto('http://localhost:8025');
  await mail.getByText(email).first().click();
  const link = mail.getByRole('link', { name: /convites\/aceitar/ }).first();
  await expect(link).toBeVisible();
  const href = await link.getAttribute('href');
  await mail.close();
  if (!href) throw new Error('Link do convite não encontrado na caixa de e-mail.');
  return href;
}

test('SHARE-001 - Conta existente recebe vínculo e enxerga a lista.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share001-member');
  await cadastrar(page, 'share001-owner');
  await criarLista(page, 'Lista compartilhada');
  await convidar(page, membro.email);
  await expect(page.getByRole('status')).toContainText('Participante adicionado');
  await abrirLista(membro.page, 'Lista compartilhada');
  await expect(membro.page.getByText('Participante')).toBeVisible();
  await membro.context.close();
});

test('SHARE-002 - Visitante cadastra-se pelo convite e o aceita.', async ({ page, context }) => {
  test.setTimeout(60_000);
  await cadastrar(page, 'share002-owner');
  await criarLista(page, 'Convite por e-mail');
  const email = `share002.${Date.now()}@example.com`;
  await convidar(page, email);
  await expect(page.getByRole('status')).toContainText('Convite enviado');
  const link = await ultimoLinkMailpit(context, email);
  await sair(page);
  await cadastrar(page, 'ignored', email);
  await page.goto(link);
  await page.getByRole('button', { name: 'Aceitar convite' }).click();
  await expect(page.getByRole('heading', { name: 'Convite por e-mail' })).toBeVisible();
});

test('SHARE-003 - Cadastro sem abrir convite não concede acesso.', async ({ page, browser }) => {
  await cadastrar(page, 'share003-owner');
  await criarLista(page, 'Acesso pendente');
  const email = `share003.${Date.now()}@example.com`;
  await convidar(page, email);
  const visitante = await contaSeparada(browser, 'unused');
  await visitante.context.close();
  const contexto = await browser.newContext();
  const nova = await contexto.newPage();
  await cadastrar(nova, 'ignored', email);
  await expect(nova.getByText('Acesso pendente')).toHaveCount(0);
  await contexto.close();
});

test('SHARE-004 - Convites inválidos e repetidos mostram mensagens específicas.', async ({ page }) => {
  const owner = await cadastrar(page, 'share004-owner');
  await criarLista(page, 'Validação de convite');
  await page.getByRole('link', { name: /Compartilhar/ }).click();
  await page.getByLabel('Convidar participante').fill('email-invalido');
  await page.getByRole('button', { name: 'Convidar' }).click();
  await expect(page.getByRole('alert')).toContainText('Informe um e-mail válido');
  await convidar(page, owner);
  await expect(page.getByRole('status')).toContainText('proprietário já possui acesso');
  const pendente = `share004.pending.${Date.now()}@example.com`;
  await convidar(page, pendente);
  await convidar(page, pendente);
  await expect(page.getByRole('status')).toContainText('Já existe um convite pendente');
});

test('SHARE-005 - Convite pendente pode ser reenviado com nova validade.', async ({ page }) => {
  await cadastrar(page, 'share005-owner');
  await criarLista(page, 'Reenvio de convite');
  const email = `share005.${Date.now()}@example.com`;
  await convidar(page, email);
  const convite = page.getByRole('region', { name: 'Convites pendentes' }).getByText(email).locator('..');
  await convite.getByRole('button', { name: 'Reenviar' }).click();
  await expect(page.getByRole('status')).toContainText('Convite reenviado com sucesso');
});

test('SHARE-006 - Convite cancelado torna-se indisponível.', async ({ page, context }) => {
  await cadastrar(page, 'share006-owner');
  await criarLista(page, 'Convite cancelado');
  const email = `share006.${Date.now()}@example.com`;
  await convidar(page, email);
  const link = await ultimoLinkMailpit(context, email);
  await page.getByText(email).locator('..').getByRole('button', { name: 'Cancelar convite' }).click();
  await sair(page);
  await page.goto(link);
  await expect(page.getByRole('alert')).toContainText(/cancelado|inválido|indisponível/i);
});

test('SHARE-007 - Conta divergente não consome o convite.', async ({ page, browser, context }) => {
  await cadastrar(page, 'share007-owner');
  await criarLista(page, 'Convite protegido');
  const correto = `share007.correct.${Date.now()}@example.com`;
  await convidar(page, correto);
  const link = await ultimoLinkMailpit(context, correto);
  const errado = await contaSeparada(browser, 'share007-wrong');
  await errado.page.goto(link);
  await errado.page.getByRole('button', { name: 'Aceitar convite' }).click();
  await expect(errado.page.getByRole('alert')).toContainText(/e-mail|aceitar/i);
  await errado.context.close();
  await sair(page);
  await cadastrar(page, 'ignored', correto);
  await page.goto(link);
  await page.getByRole('button', { name: 'Aceitar convite' }).click();
  await expect(page.getByRole('heading', { name: 'Convite protegido' })).toBeVisible();
});

test('SHARE-008 - Participante consulta e edita itens, sem administrar a lista.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share008-member');
  await cadastrar(page, 'share008-owner');
  await criarLista(page, 'Permissões de membro');
  await convidar(page, membro.email);
  await abrirLista(membro.page, 'Permissões de membro');
  await expect(membro.page.getByRole('link', { name: 'Adicionar item' })).toBeVisible();
  await expect(membro.page.getByRole('link', { name: 'Editar lista' })).toHaveCount(0);
  await membro.context.close();
});

test('SHARE-009 - Remoção revoga rapidamente a sessão do participante.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share009-member');
  await cadastrar(page, 'share009-owner');
  await criarLista(page, 'Remoção colaborativa');
  await convidar(page, membro.email);
  await abrirLista(membro.page, 'Remoção colaborativa');
  const artigo = page.getByRole('region', { name: 'Pessoas com acesso' }).getByText(membro.email).locator('..');
  await artigo.getByRole('button', { name: 'Remover' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Remover' }).click();
  await expect(membro.page).toHaveURL(/\/listas$/, { timeout: 5_000 });
  await membro.context.close();
});

test('SHARE-010 - Participante cancela e depois confirma sua saída.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share010-member');
  await cadastrar(page, 'share010-owner');
  await criarLista(page, 'Saída confirmada');
  await convidar(page, membro.email);
  await abrirLista(membro.page, 'Saída confirmada');
  await membro.page.getByRole('link', { name: /Compartilhar/ }).click();
  await membro.page.getByRole('button', { name: 'Sair da lista' }).click();
  await membro.page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
  await expect(membro.page.getByRole('region', { name: 'Pessoas com acesso' })).toBeVisible();
  await membro.page.getByRole('button', { name: 'Sair da lista' }).click();
  await membro.page.getByRole('dialog').getByRole('button', { name: 'Sair da lista' }).click();
  await expect(membro.page).toHaveURL(/\/listas$/);
  await membro.context.close();
});

test('SHARE-011 - Acesso exibe pessoas ao autorizado e preserva privacidade.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share011-member');
  await cadastrar(page, 'share011-owner');
  await criarLista(page, 'Privacidade');
  await convidar(page, membro.email);
  await expect(page.getByRole('region', { name: 'Pessoas com acesso' })).toContainText(membro.email);
  const alheio = await contaSeparada(browser, 'share011-other');
  await alheio.page.goto(page.url());
  await expect(alheio.page.getByRole('status')).toContainText('indisponível');
  await expect(alheio.page.getByText(membro.email)).toHaveCount(0);
  await membro.context.close();
  await alheio.context.close();
});

test('SHARE-012 - Dois contextos autorizados convergem após reconexão.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share012-member');
  await cadastrar(page, 'share012-owner');
  await criarLista(page, 'Reconexão compartilhada');
  const listaUrl = page.url();
  await convidar(page, membro.email);
  await membro.page.goto(listaUrl);
  await expect(membro.page.getByRole('heading', { name: 'Reconexão compartilhada' })).toBeVisible();
  await membro.context.setOffline(true);
  await expect(membro.page.getByRole('status')).toContainText('Sem conexão');
  await membro.context.setOffline(false);
  await expect(membro.page.getByRole('status')).toContainText('Sincronizada');
  await membro.context.close();
});

test('SHARE-013 - Exclusão revoga membro e convite pendente.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share013-member');
  await cadastrar(page, 'share013-owner');
  await criarLista(page, 'Compartilhamento excluído');
  await convidar(page, membro.email);
  await expect(page.getByRole('status')).toContainText('Participante adicionado');
  await convidar(page, `share013.pending.${Date.now()}@example.com`);
  await expect(page.getByRole('status')).toContainText('Convite enviado');
  await page.goBack();
  await page.reload();
  await page.getByRole('button', { name: 'Concluir lista' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click();
  await expect(page.getByText('Lista concluída com sucesso.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Excluir' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
  await membro.page.goto('/listas');
  await expect(membro.page.getByTestId('list-card').filter({
    hasText: 'Compartilhamento excluído',
  })).toHaveCount(0);
  await membro.context.close();
});

test('SHARE-014 - Lista concluída mantém relações apenas para consulta.', async ({ page, browser }) => {
  const membro = await contaSeparada(browser, 'share014-member');
  await cadastrar(page, 'share014-owner');
  await criarLista(page, 'Compartilhamento concluído');
  const listaUrl = page.url();
  await convidar(page, membro.email);
  await expect(page.getByRole('status')).toContainText('Participante adicionado');
  await page.goBack();
  await page.reload();
  await page.getByRole('button', { name: 'Concluir lista' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Concluir', exact: true }).click();
  await expect(page.getByText('Lista concluída com sucesso.', { exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: /Compartilhar/ })).toHaveCount(0);
  await membro.page.goto(listaUrl);
  await expect(membro.page.getByRole('heading', { name: 'Compartilhamento concluído' })).toBeVisible();
  await expect(membro.page.getByRole('link', { name: 'Adicionar item' })).toHaveCount(0);
  await membro.context.close();
});
