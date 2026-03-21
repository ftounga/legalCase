import { Page } from '@playwright/test';

export const TEST_USER = {
  email: process.env['E2E_LOCAL_EMAIL'] ?? 'e2e@legalcase.test',
  password: process.env['E2E_LOCAL_PASSWORD'] ?? 'E2ePassword1!',
};

/**
 * Connecte l'utilisateur de test via l'auth locale (email/mdp).
 * Prérequis : le compte doit exister en base et être vérifié.
 */
export async function loginLocal(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(TEST_USER.email);
  await page.getByLabel('Mot de passe').fill(TEST_USER.password);
  // exact: true pour ne pas matcher "Se connecter avec Google" / "Se connecter avec Microsoft"
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
  await page.waitForURL('**/case-files', { timeout: 10000 });
}

/**
 * Déconnecte l'utilisateur courant via le menu utilisateur du shell.
 */
export async function logout(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Menu utilisateur' }).click();
  await page.getByRole('menuitem', { name: /déconnecter/i }).click();
  await page.waitForURL('**/login', { timeout: 5000 });
}
