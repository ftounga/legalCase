import { test, expect } from '@playwright/test';
import { loginLocal } from '../helpers/auth.helper';

/**
 * Happy path — Flux avocat complet
 *
 * Simule ce qu'un avocat fait la première fois :
 * login → voir liste dossiers → créer un dossier → vérifier qu'il apparaît
 *
 * Ce test traverse toutes les couches : auth → workspace → API → DB → frontend.
 * Si ce test échoue, l'application est fondamentalement inutilisable.
 */

const DOSSIER_TITLE = `E2E Licenciement Test ${Date.now()}`;

test.describe('Happy path — flux avocat', () => {

  test('login → liste dossiers → créer dossier → vérifier présence', async ({ page }) => {
    // 1. Login
    await loginLocal(page);
    await expect(page).toHaveURL(/\/case-files/);
    await expect(page.locator('app-case-files-list')).toBeVisible({ timeout: 8000 });

    // 2. Ouvrir la dialog de création
    await page.getByRole('button', { name: /nouveau dossier/i }).click();
    await expect(page.getByRole('heading', { name: 'Nouveau dossier' })).toBeVisible();

    // 3. Remplir le formulaire
    await page.getByLabel('Titre').fill(DOSSIER_TITLE);
    await page.getByLabel('Description').fill('Dossier créé automatiquement par les tests E2E');

    // 4. Soumettre
    await page.getByRole('button', { name: 'Créer le dossier' }).click();

    // 5. La dialog se ferme
    await expect(page.getByRole('heading', { name: 'Nouveau dossier' })).not.toBeVisible({ timeout: 5000 });

    // 6. Le dossier apparaît dans la liste (c'est l'assertion critique)
    await expect(page.getByRole('link', { name: DOSSIER_TITLE })).toBeVisible({ timeout: 8000 });
  });

  test('login → cliquer sur un dossier → ouvrir le détail', async ({ page }) => {
    await loginLocal(page);
    await expect(page).toHaveURL(/\/case-files/);

    // Attendre qu'il y ait au moins un dossier (créé par le test précédent)
    const firstLink = page.locator('mat-cell a.case-file-link').first();
    await expect(firstLink).toBeVisible({ timeout: 8000 });

    const dossierTitle = await firstLink.textContent();
    await firstLink.click();

    // La page détail se charge
    await expect(page).toHaveURL(/\/case-files\/[0-9a-f-]{36}/, { timeout: 5000 });
    // Le titre du dossier est visible sur la page détail
    await expect(page.getByText(dossierTitle!.trim())).toBeVisible({ timeout: 5000 });
  });

});
