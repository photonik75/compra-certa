import { expect, Page, test } from '@playwright/test';

const EMAIL = 'E-mail';
const SENHA = 'Senha';
const SENHA_PADRAO = 'Senha segura 123';
const MINHAS_LISTAS = 'Minhas Listas';
const LOGIN = '/entrar';
const CADASTRO = '/cadastro';
const LISTAS = '/listas';
const EMAIL_EXISTENTE = process.env.E2E_AUTH_EMAIL ?? 'pessoa@exemplo.com';
const SENHA_EXISTENTE = process.env.E2E_AUTH_PASSWORD ?? SENHA_PADRAO;
const EMAIL_BLOQUEIO = process.env.E2E_AUTH_LOCK_EMAIL ?? 'bloqueio@exemplo.com';
const SENHA_BLOQUEIO = process.env.E2E_AUTH_LOCK_PASSWORD ?? SENHA_PADRAO;
const EMAIL_REDEFINICAO = process.env.E2E_AUTH_RESET_EMAIL ?? 'redefinicao@exemplo.com';
const SENHA_REDEFINICAO = process.env.E2E_AUTH_RESET_PASSWORD ?? SENHA_PADRAO;
const LINK_VALIDO = process.env.E2E_AUTH_VALID_RESET_URL ?? '/redefinir-senha#token=valido';
const LINK_EXPIRADO = process.env.E2E_AUTH_EXPIRED_RESET_URL ?? '/redefinir-senha#token=expirado';
const LINK_INVALIDO = process.env.E2E_AUTH_INVALID_RESET_URL ?? '/redefinir-senha#token=invalido';

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
  await page.goto(LISTAS);
  await expect(page).toHaveURL(/\/entrar\?returnUrl=/);
  await page.getByRole('textbox', { name: EMAIL }).fill(EMAIL_EXISTENTE);
  await page.getByLabel(SENHA).fill(SENHA_EXISTENTE);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(new RegExp(`${LISTAS}$`));
  await sair(page);
  await page.goBack();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).not.toBeVisible();
  await page.goto(LISTAS);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

test('AUTH-005 - Login inválido não revela se o e-mail existe.', async ({ page }) => {
  for (const [email, senha] of [
    [EMAIL_EXISTENTE, 'Senha incorreta 123'],
    [emailUnico('inexistente'), SENHA_EXISTENTE],
  ]) {
    await entrar(page, email, senha);
    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  }
});

test('AUTH-006 - Cinco falhas bloqueiam novas tentativas temporariamente.', async ({ page }) => {
  for (let tentativa = 0; tentativa < 5; tentativa++) {
    await entrar(page, EMAIL_BLOQUEIO, 'Senha incorreta 123');
    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  }
  await entrar(page, EMAIL_BLOQUEIO, SENHA_BLOQUEIO);
  await expect(page.getByRole('alert')).toHaveText(
    'Muitas tentativas de acesso. Tente novamente em 15 minutos',
  );
});

test('AUTH-007 - Manter-me conectado conserva o acesso no mesmo navegador.', async ({ page }) => {
  await page.goto(LOGIN);
  await page.getByRole('textbox', { name: EMAIL }).fill(EMAIL_EXISTENTE);
  await page.getByLabel(SENHA).fill(SENHA_EXISTENTE);
  await page.getByRole('checkbox', { name: 'Manter-me conectado' }).check();
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.reload();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
});

test('AUTH-008 - Recuperação não revela se a conta existe.', async ({ page }) => {
  const confirmacao = 'Se houver uma conta para este e-mail, enviaremos as instruções';
  for (const email of [EMAIL_EXISTENTE, emailUnico('inexistente')]) {
    await page.goto('/recuperar-senha');
    await page.getByLabel(EMAIL).fill(email);
    await page.getByRole('button', { name: 'Enviar instruções' }).click();
    await expect(page.getByText('Solicitação de recuperação enviada com sucesso.')).toBeVisible();
    await expect(page.getByRole('status')).toHaveText(confirmacao);
  }
});

test('AUTH-009 - Redefinição válida invalida o link e a senha anterior.', async ({ page }) => {
  const novaSenha = `Nova senha ${Date.now()}`;
  await redefinir(page, LINK_VALIDO, novaSenha);
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await entrar(page, EMAIL_REDEFINICAO, SENHA_REDEFINICAO);
  await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos');
  await entrar(page, EMAIL_REDEFINICAO, novaSenha);
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  await sair(page);
  await redefinir(page, LINK_VALIDO, 'Outra senha 123');
  await expect(page.getByRole('link', { name: 'Solicitar nova recuperação' })).toBeVisible();
});

test('AUTH-010 - Links expirado e inválido não alteram a senha.', async ({ page }) => {
  for (const link of [LINK_EXPIRADO, LINK_INVALIDO]) {
    await redefinir(page, link, 'Senha que não será aceita 123');
    await expect(page.getByRole('alert')).toContainText('O link de recuperação é inválido');
    await expect(page.getByRole('link', { name: 'Solicitar nova recuperação' })).toBeVisible();
  }
  await entrar(page, EMAIL_EXISTENTE, SENHA_EXISTENTE);
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
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
