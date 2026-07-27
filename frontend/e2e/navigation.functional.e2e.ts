import { expect, test } from '@playwright/test';

test('NAV-001/002/006/007 - menu autenticado navega e destaca as seções', async ({ page }) => {
  await page.goto('/listas');
  const menu = page.getByRole('navigation', { name: 'Navegação principal' });
  await expect(menu).toBeVisible();
  await expect(page.getByText('Larissa Barros')).toBeVisible();
  await expect(menu.getByRole('link', { name: 'Minhas listas' })).toHaveAttribute('aria-current', 'page');
  await menu.getByRole('link', { name: 'Categorias' }).click();
  await expect(page).toHaveURL(/\/categorias$/);
  await expect(menu.getByRole('link', { name: 'Categorias' })).toHaveAttribute('aria-current', 'page');
  await menu.getByRole('link', { name: 'Produtos' }).click();
  await expect(page).toHaveURL(/\/produtos$/);
  await expect(menu.getByRole('link', { name: 'Produtos' })).toHaveAttribute('aria-current', 'page');
});

test('NAV-003/004 - logout remove o menu e protege as rotas internas', async ({ page }) => {
  await page.goto('/listas');
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Navegação principal' })).not.toBeVisible();
  await page.goto('/produtos');
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

test('NAV-005/010 - drawer mobile controla foco, fundo e fechamento', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/listas');
  const toggle = page.getByRole('button', { name: 'Abrir menu' });
  await toggle.click();
  await expect(page.getByRole('complementary', { name: 'Menu da área autenticada' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Fechar menu' })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(toggle).toBeFocused();
});
