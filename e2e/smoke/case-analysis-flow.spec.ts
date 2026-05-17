import { test, expect } from '@playwright/test';
import { loginLocal } from '../helpers/auth.helper';
import path from 'path';

/**
 * Parcours métier complet — Flux analyse IA
 *
 * Simule le parcours critique d'un avocat :
 * login → créer dossier → uploader document → lancer analyse →
 * attendre résultat → consulter synthèse → exporter PDF
 *
 * Le dossier est nettoyé en fin de test (suppression).
 * Ce test nécessite un backend fonctionnel avec accès IA (staging).
 */

const DOSSIER_TITLE = `[E2E] Analyse Flow ${Date.now()}`;
const PDF_FIXTURE = path.resolve(__dirname, '../fixtures/test-contrat-travail.pdf');

test.describe('Parcours métier — analyse IA complète', () => {

  let caseFileUrl: string;

  test('créer dossier → upload → analyse → synthèse → export PDF', async ({ page }) => {
    test.setTimeout(180_000); // 3 minutes max — l'analyse IA peut être longue

    // ── 1. Login ──
    await loginLocal(page);
    await expect(page).toHaveURL(/\/case-files/);

    // ── 2. Créer un dossier ──
    await page.getByRole('button', { name: /nouveau dossier/i }).click();
    await expect(page.getByRole('heading', { name: 'Nouveau dossier' })).toBeVisible();
    await page.getByLabel('Titre').fill(DOSSIER_TITLE);
    await page.getByLabel('Description').fill('Dossier E2E — test parcours analyse complet');
    await page.getByRole('button', { name: 'Créer le dossier' }).click();

    // Attendre la navigation vers le détail du dossier
    await expect(page).toHaveURL(/\/case-files\/[0-9a-f-]{36}/, { timeout: 10_000 });
    caseFileUrl = page.url();

    // ── 3. Uploader un document ──
    await page.getByRole('button', { name: /ajouter des documents/i }).click();

    // Le file input est caché — on le remplit directement
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(PDF_FIXTURE);

    // Cliquer "Uploader les documents"
    const uploadBtn = page.getByRole('button', { name: /uploader les documents/i });
    await expect(uploadBtn).toBeEnabled({ timeout: 5_000 });
    await uploadBtn.click();

    // Attendre que le document apparaisse dans la liste
    await expect(page.getByText('test-contrat-travail.pdf')).toBeVisible({ timeout: 15_000 });

    // ── 3bis. Basculer sur l'onglet « Analyse » ──
    // F-244 SF-244-01 : depuis la structure en onglets, le bouton d'analyse et
    // le lien synthèse vivent dans l'onglet « Analyse » (masqué par défaut).
    await page.getByRole('tab', { name: 'Analyse' }).click();

    // ── 4. Lancer l'analyse ──
    const analyzeBtn = page.getByRole('button', { name: /analyser le dossier/i });
    await expect(analyzeBtn).toBeEnabled({ timeout: 10_000 });
    await analyzeBtn.click();

    // ── 5. Attendre que l'analyse se termine ──
    // Le lien "Voir la synthèse" apparaît quand l'analyse est DONE
    const synthesisLink = page.getByRole('link', { name: /voir la synthèse/i });
    await expect(synthesisLink).toBeVisible({ timeout: 120_000 }); // 2 min max pour l'IA

    // ── 6. Consulter la synthèse ──
    await synthesisLink.click();
    await expect(page).toHaveURL(/\/synthesis/, { timeout: 10_000 });

    // Vérifier la présence des sections clés
    await expect(page.getByText(/chronologie|timeline/i)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/faits/i).first()).toBeVisible();

    // ── 7. Exporter PDF ──
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 15_000 }),
      page.getByRole('button', { name: /exporter pdf/i }).first().click()
    ]);
    expect(download.suggestedFilename()).toContain('.pdf');
  });

  test.afterAll(async ({ browser }) => {
    // Cleanup : supprimer le dossier créé
    if (!caseFileUrl) return;
    const context = await browser.newContext();
    const page = await context.newPage();
    try {
      await loginLocal(page);
      await page.goto(caseFileUrl);
      await expect(page).toHaveURL(/\/case-files\/[0-9a-f-]{36}/, { timeout: 10_000 });

      // Cliquer sur le bouton supprimer
      const deleteBtn = page.getByRole('button', { name: /supprimer/i });
      if (await deleteBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await deleteBtn.click();
        // Confirmer la suppression dans le dialog
        const confirmBtn = page.getByRole('button', { name: /confirmer|supprimer/i }).last();
        if (await confirmBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
          await confirmBtn.click();
        }
      }
    } catch {
      // Fail-silent — le global-setup nettoiera au prochain run
    } finally {
      await context.close();
    }
  });
});
