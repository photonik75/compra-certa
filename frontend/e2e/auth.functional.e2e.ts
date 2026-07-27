import { Browser, expect, Page, test } from '@playwright/test';

const EMAIL = 'E-mail';
const SENHA = 'Senha';
const SENHA_PADRAO = 'Senha segura 123';
const MINHAS_LISTAS = 'Minhas Listas';
const LOGIN = '/entrar';
const CADASTRO = '/cadastro';
const LISTAS = '/listas';
const MAILPIT = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025';

function emailUnico(prefixo: string): string {
  return `${prefixo}.${Date.now()}@example.com`;
}

async function abrirCadastro(page: Page): Promise<void> {
  await page.goto(CADASTRO);
  await expect(page.getByRole('heading', { name: 'Crie sua conta' })).toBeVisible();
}

async function cadastrar(page: Page, email: string, senha = SENHA_PADRAO): Promise<void> {
  await abrirCadastro(page);
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel(EMAIL).fill(email);
  await page.getByLabel(SENHA, { exact: true }).fill(senha);
  await page.getByLabel('Confirmar senha').fill(senha);
  await page.getByRole('button', { name: 'Criar conta' }).click();
}

async function entrar(page: Page, email: string, senha: string): Promise<void> {
  await page.goto(LOGIN);
  await page.getByRole('textbox', { name: EMAIL }).fill(email);
  await page.getByLabel(SENHA).fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

async function sair(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
}

async function solicitarRecuperacao(page: Page, email: string): Promise<void> {
  await page.goto('/recuperar-senha');
  await page.getByLabel(EMAIL).fill(email);
  await page.getByRole('button', { name: 'Enviar instruções' }).click();
}

async function localizarLinkRecebido(browser: Browser, email: string): Promise<string> {
  const caixa = await browser.newPage();
  await caixa.goto(MAILPIT);
  const pesquisa = caixa.getByRole('textbox', { name: 'Search' });
  await pesquisa.fill(email);
  await pesquisa.press('Enter');
  const mensagem = caixa.locator('a.message').filter({ hasText: email });
  await expect(mensagem).toHaveCount(1);
  await mensagem.click();
  const link = caixa.locator('a[href^="http://localhost:4200/redefinir-senha#token="]');
  await expect(link).toHaveCount(1);
  const href = await link.getAttribute('href');
  await caixa.close();
  if (!href) throw new Error('A mensagem não apresentou o link de redefinição.');
  return href;
}

async function redefinir(page: Page, link: string, senha: string): Promise<void> {
  await page.goto(link);
  await page.getByLabel('Nova senha', { exact: true }).fill(senha);
  await page.getByLabel('Confirmar nova senha').fill(senha);
  await page.getByRole('button', { name: 'Redefinir senha' }).click();
}

test('AUTH-001 - Cadastro válido cria uma conta e mantém o acesso após recarga.', async ({
  page,
}) => {
  await cadastrar(page, emailUnico('auth001'));
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sair' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
});

test('AUTH-002 - Cadastro inválido exibe erros e não autentica o visitante.', async ({ page }) => {
  await abrirCadastro(page);
  const criarConta = page.getByRole('button', { name: 'Criar conta' });
  await expect(criarConta).toBeDisabled();
  await page.getByLabel('Nome').fill(' ');
  await page.getByLabel(EMAIL).fill('email inválido');
  await page.getByLabel(SENHA, { exact: true }).fill('1234567');
  await page.getByLabel('Confirmar senha').fill('diferente');
  await expect(page.getByText('Por favor, informe seu nome')).toBeVisible();
  await expect(page.getByText('Por favor, informe um e-mail válido')).toBeVisible();
  await expect(page.getByText('A senha deve ter entre 8 e 128 caracteres')).toBeVisible();
  await expect(page.getByText('As senhas devem ser idênticas')).toBeVisible();
  await page.getByLabel(SENHA, { exact: true }).fill('x'.repeat(129));
  await expect(criarConta).toBeDisabled();
  await page.goto(LISTAS);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

test('AUTH-003 - E-mail duplicado ignora caixa e não cria outra conta.', async ({ page }) => {
  const email = emailUnico('auth003');
  await cadastrar(page, email);
  await sair(page);
  await cadastrar(page, email.toUpperCase());
  await expect(page.getByRole('dialog', { name: 'E-mail já foi cadastrado' })).toBeVisible();
  await page.goto(LISTAS);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

test('AUTH-004 - Login retorna à rota solicitada e logout protege histórico e rota interna.', async ({
  page,
}) => {
  const email = emailUnico('auth004');
  await cadastrar(page, email);
  await sair(page);
  await page.goto(LISTAS);
  await expect(page).toHaveURL(/\/entrar\?returnUrl=/);
  await page.getByRole('textbox', { name: EMAIL }).fill(email);
  await page.getByLabel(SENHA).fill(SENHA_PADRAO);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(new RegExp(`${LISTAS}$`));
  await sair(page);
  await page.goBack();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).not.toBeVisible();
  await page.goto(LISTAS);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

test('AUTH-005 - Login inválido não revela se o e-mail existe.', async ({ page }) => {
  const emailExistente = emailUnico('auth005');
  await cadastrar(page, emailExistente);
  await sair(page);
  for (const [email, senha] of [
    [emailExistente, 'Senha incorreta 123'],
    [emailUnico('inexistente'), SENHA_PADRAO],
  ]) {
    await entrar(page, email, senha);
    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  }
});

test('AUTH-006 - Cinco falhas bloqueiam novas tentativas temporariamente.', async ({ page }) => {
  const email = emailUnico('auth006');
  await cadastrar(page, email);
  await sair(page);
  for (let tentativa = 0; tentativa < 5; tentativa++) {
    await entrar(page, email, 'Senha incorreta 123');
    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  }
  await entrar(page, email, SENHA_PADRAO);
  await expect(page.getByRole('alert')).toHaveText(
    'Muitas tentativas de acesso. Tente novamente em 15 minutos',
  );
});

test('AUTH-007 - Manter-me conectado conserva o acesso no mesmo navegador.', async ({ page }) => {
  const email = emailUnico('auth007');
  await cadastrar(page, email);
  await sair(page);
  await page.goto(LOGIN);
  await page.getByRole('textbox', { name: EMAIL }).fill(email);
  await page.getByLabel(SENHA).fill(SENHA_PADRAO);
  await page.getByRole('checkbox', { name: 'Manter-me conectado' }).check();
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
});

test('AUTH-008 - Recuperação não revela se a conta existe.', async ({ page, browser }) => {
  const confirmacao = 'Se houver uma conta para este e-mail, enviaremos as instruções';
  const emailExistente = emailUnico('auth008');
  const emailInexistente = emailUnico('inexistente');
  await cadastrar(page, emailExistente);
  await sair(page);
  for (const email of [emailExistente, emailInexistente]) {
    await solicitarRecuperacao(page, email);
    await expect(page.getByText('Solicitação de recuperação enviada com sucesso.')).toBeVisible();
    await expect(page.getByRole('status')).toHaveText(confirmacao);
  }
  await localizarLinkRecebido(browser, emailExistente);
  const caixa = await browser.newPage();
  await caixa.goto(MAILPIT);
  const pesquisa = caixa.getByRole('textbox', { name: 'Search' });
  await pesquisa.fill(emailInexistente);
  await pesquisa.press('Enter');
  await expect(caixa.getByText('No results for')).toBeVisible();
  await caixa.close();
});

test('AUTH-009 - Redefinição válida invalida o link e a senha anterior.', async ({
  page,
  browser,
}) => {
  const email = emailUnico('auth009');
  const novaSenha = `Nova senha ${Date.now()}`;
  await cadastrar(page, email);
  const outroAcesso = await browser.newPage();
  await solicitarRecuperacao(outroAcesso, email);
  const link = await localizarLinkRecebido(browser, email);
  await redefinir(outroAcesso, link, novaSenha);
  await expect(outroAcesso.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await entrar(outroAcesso, email, SENHA_PADRAO);
  await expect(outroAcesso.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  await entrar(outroAcesso, email, novaSenha);
  await expect(outroAcesso.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  await sair(outroAcesso);
  await redefinir(outroAcesso, link, 'Outra senha 123');
  await expect(outroAcesso.getByRole('link', { name: 'Solicitar nova recuperação' })).toBeVisible();
  await outroAcesso.close();
});

test('AUTH-010 - Links expirado e inválido não alteram a senha.', async ({ page, browser }) => {
  const email = emailUnico('auth010');
  await cadastrar(page, email);
  await sair(page);
  await solicitarRecuperacao(page, email);
  const link = await localizarLinkRecebido(browser, email);
  const linkInvalido = `${link.slice(0, -1)}x`;
  await redefinir(page, linkInvalido, 'Senha que não será aceita 123');
  await expect(page.getByRole('alert')).toContainText('O link de recuperação é inválido');
  await expect(page.getByRole('link', { name: 'Solicitar nova recuperação' })).toBeVisible();
  await page.waitForTimeout(10_100);
  const novaPagina = await browser.newPage();
  await redefinir(novaPagina, link, 'Senha que também não será aceita 123');
  await expect(novaPagina.getByRole('alert')).toContainText('O link de recuperação é inválido');
  await expect(novaPagina.getByRole('link', { name: 'Solicitar nova recuperação' })).toBeVisible();
  await entrar(novaPagina, email, SENHA_PADRAO);
  await expect(novaPagina.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  await novaPagina.close();
});

test('AUTH-011 - Controles são operáveis por teclado e erros são anunciados.', async ({ page }) => {
  await abrirCadastro(page);
  for (const id of ['nome', 'email', 'senha', 'confirmar-senha']) {
    await page.keyboard.press('Tab');
    await expect(page.locator(`#${id}`)).toBeFocused();
    if (id === 'senha' || id === 'confirmar-senha') await page.keyboard.press('Tab');
  }
  const senha = page.getByLabel(SENHA, { exact: true });
  await senha.fill(SENHA_PADRAO);
  await page.getByRole('button', { name: 'Mostrar' }).first().click();
  await expect(senha).toHaveAttribute('type', 'text');
  await expect(senha).toHaveValue(SENHA_PADRAO);
  await page.getByLabel('Nome').fill(' ');
  await expect(page.getByText('Por favor, informe seu nome')).toHaveAttribute('role', 'alert');
  await expect(page.getByLabel('Nome')).toHaveAttribute('aria-invalid', 'true');
  await expect(page.getByLabel('Nome')).toHaveAttribute('aria-describedby', 'erro-nome');
});

test('AUTH-012 - Login inválido exibe mensagens de campo e não solicita autenticação.', async ({
  page,
}) => {
  await page.goto(LOGIN);
  const entrarButton = page.getByRole('button', { name: 'Entrar' });
  await expect(entrarButton).toBeDisabled();
  await page.getByRole('textbox', { name: EMAIL }).fill('email inválido');
  await page.getByLabel(SENHA).fill('1234567');
  await expect(page.getByText('Por favor, informe um e-mail válido')).toBeVisible();
  await expect(page.getByText('A senha deve ter pelo menos 8 caracteres')).toBeVisible();
  await page.getByRole('textbox', { name: EMAIL }).fill(`${'a'.repeat(243)}@example.com`);
  await expect(page.getByText('Por favor, informe um e-mail válido')).toBeVisible();
  await expect(entrarButton).toBeDisabled();
  await page.goto(LISTAS);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});
