import { test, expect } from '@playwright/test';
import { loginLocal, logout, TEST_USER } from '../helpers/auth.helper';

/**
 * Smoke tests — Authentification
 *
 * Couvre les chemins critiques :
 * - Page de login accessible et correctement structurée
 * - Login local valide → redirection vers /case-files
 * - Login local invalide → message d'erreur visible
 * - Utilisateur non connecté → redirigé vers /login
 * - Logout → redirigé vers /login
 *
 * Prérequis : un compte local vérifié avec E2E_LOCAL_EMAIL / E2E_LOCAL_PASSWORD
 */

test.describe('Auth — chemins critiques', () => {

  test('la page /login charge avec les deux onglets', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('tab', { name: /se connecter/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /s'inscrire/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /google/i })).toBeVisible();
  });

  test('login local valide → redirigé vers /case-files', async ({ page }) => {
    await loginLocal(page);
    await expect(page).toHaveURL(/\/case-files/);
    // La liste des dossiers est visible
    await expect(page.locator('app-case-files-list')).toBeVisible({ timeout: 8000 });
  });

  test('login local invalide → message d\'erreur affiché', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(TEST_USER.email);
    await page.getByLabel('Mot de passe').fill('mauvais-mot-de-passe');
    await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
    // Reste sur /login et affiche une erreur
    await expect(page).toHaveURL(/\/login/);
    await expect(
      page.getByText(/incorrect|invalide|erreur/i).or(page.locator('mat-error, .error-message'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('accès /case-files sans session → redirigé vers /login', async ({ page }) => {
    // Pas de cookie de session
    await page.goto('/case-files');
    await expect(page).toHaveURL(/\/login/, { timeout: 8000 });
  });

  test('logout → redirigé vers /login', async ({ page }) => {
    await loginLocal(page);
    await logout(page);
    await expect(page).toHaveURL(/\/login/);
    // Retenter d'accéder à une page protégée → reste sur /login
    await page.goto('/case-files');
    await expect(page).toHaveURL(/\/login/, { timeout: 8000 });
  });

});
