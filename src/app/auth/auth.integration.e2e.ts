import { expect, Page, test } from '@playwright/test';

const ENDPOINT_LOGIN = '**/api/v1/auth/sessions';
const ENDPOINT_LOGOUT = '**/api/v1/auth/sessions/current';
const ENDPOINT_SESSAO = '**/api/v1/auth/session';
const EMAIL = 'E-mail';
const EMAIL_VALIDO = 'maria@example.com';
const ENTRAR = 'Entrar';
const MINHAS_LISTAS = 'Minhas Listas';
const SAIR = 'Sair';
const SENHA = 'Senha';
const SENHA_VALIDA = 'senha123';
const ROTA_ENTRAR = '/entrar';
const ROTA_LISTAS = '/listas';
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: EMAIL_VALIDO,
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-23T00:00:00Z',
};

async function configurarSessao(page: Page, estaValida: () => boolean): Promise<void> {
  await page.route(ENDPOINT_SESSAO, async (route) => {
    if (estaValida()) {
      await route.fulfill({ status: 200, json: SESSION_RESPONSE });
      return;
    }
    await route.fulfill({ status: 401, json: {} });
  });
}

test('INT-1 - Restaura uma sessão válida após recarga e abre o login quando ela expira.', async ({ page }) => {
  let sessaoValida = true;
  let consultasDaSessao = 0;
  await page.route(ENDPOINT_SESSAO, async (route) => {
    consultasDaSessao++;
    if (sessaoValida) {
      await route.fulfill({ status: 200, json: SESSION_RESPONSE });
      return;
    }
    await route.fulfill({ status: 401, json: {} });
  });
  await page.goto(ROTA_LISTAS);
  await page.reload();
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).toBeVisible();
  expect(consultasDaSessao).toBeGreaterThan(0);
  sessaoValida = false;
  await page.reload();
  await expect(page).toHaveURL(new RegExp(`${ROTA_ENTRAR}$`));
});

test('INT-2 - Voltar após sair não revela dados protegidos.', async ({ page }) => {
  let sessaoValida = false;
  await configurarSessao(page, () => sessaoValida);
  await page.route(ENDPOINT_LOGIN, async (route) => {
    sessaoValida = true;
    await route.fulfill({ status: 200, json: SESSION_RESPONSE });
  });
  await page.route(ENDPOINT_LOGOUT, async (route) => {
    sessaoValida = false;
    await route.fulfill({ status: 204 });
  });
  await page.goto(ROTA_ENTRAR);
  await page.getByRole('textbox', { name: EMAIL }).fill(EMAIL_VALIDO);
  await page.getByLabel(SENHA).fill(SENHA_VALIDA);
  await page.getByRole('button', { name: ENTRAR }).click();
  await expect(page).toHaveURL(new RegExp(`${ROTA_LISTAS}$`));
  await page.getByRole('button', { name: SAIR }).click();
  await expect(page).toHaveURL(new RegExp(`${ROTA_ENTRAR}$`));
  await page.goBack();
  await expect(page).toHaveURL(new RegExp(`${ROTA_ENTRAR}$`));
  await expect(page.getByRole('heading', { name: MINHAS_LISTAS })).not.toBeVisible();
});
