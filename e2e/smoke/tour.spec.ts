import { test, expect } from '@playwright/test';
import { loginLocal } from '../helpers/auth.helper';

/**
 * Smoke tests — Product tour (SF-67-02)
 *
 * Vérifie que la carte flottante du tour apparaît correctement
 * et que la progression fonctionne sur /case-files.
 *
 * Pré-requis : le localStorage ne doit pas contenir la clé
 * onboarding_tour_done_<workspaceId> pour que le tour se déclenche.
 */

test.describe('Product tour — carte flottante', () => {

  test.beforeEach(async ({ page }) => {
    await loginLocal(page);
    // Supprimer toutes les clés tour pour forcer le déclenchement
    await page.evaluate(() => {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith('onboarding_tour_done_')) {
          localStorage.removeItem(key);
        }
      }
    });
    await page.goto('/case-files');
    await page.waitForLoadState('networkidle');
  });

  // E-01 : Premier accès /case-files → carte flottante visible
  test('E-01: carte flottante visible au 1er accès', async ({ page }) => {
    const card = page.locator('.tour-card');
    await expect(card).toBeVisible();
  });

  // E-02 : Étape 0 → titre "Bienvenue" visible
  test('E-02: étape 0 affiche le titre "Bienvenue"', async ({ page }) => {
    const title = page.locator('.tour-card__title');
    await expect(title).toContainText('Bienvenue');
  });

  // E-03 : Bouton "Suivant" → étape 1, élément [data-tour-target="new-dossier-btn"] a la classe tour-spotlight
  test('E-03: clic "Suivant" → étape 1, new-dossier-btn surligné', async ({ page }) => {
    await page.locator('button', { hasText: 'Suivant' }).click();
    const highlighted = page.locator('[data-tour-target="new-dossier-btn"]');
    await expect(highlighted).toHaveClass(/tour-spotlight/);
  });

  // E-04 : Bouton "Passer" → carte disparaît
  test('E-04: clic "Passer" → carte disparaît', async ({ page }) => {
    await page.locator('.tour-card__skip').click();
    const card = page.locator('.tour-card');
    await expect(card).not.toBeVisible();
  });

  // E-05 : Rechargement /case-files → carte non réaffichée (localStorage posé)
  test('E-05: après skip, rechargement de la page ne ré-affiche pas la carte', async ({ page }) => {
    await page.locator('.tour-card__skip').click();
    await page.reload();
    await page.waitForLoadState('networkidle');
    const card = page.locator('.tour-card');
    await expect(card).not.toBeVisible();
  });

});
