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
 *
 * ⚠️ QUARANTAINE (`test.fixme`) — ce test exerce le pipeline IA complet
 * (analyse + génération de synthèse en streaming SSE) dont la durée est
 * non déterministe et dépasse régulièrement 2 min rien que pour le streaming
 * de la synthèse. Il n'a donc pas sa place dans une suite « smoke » censée
 * être rapide et stable. Les sélecteurs et attentes ci-dessous ont été
 * remis à jour (création → liste, bouton upload exact, streaming, reload de
 * la synthèse) ; il reste à le sortir de la suite smoke vers une suite
 * d'intégration dédiée avec un budget de temps adapté avant de le réactiver.
 */

const DOSSIER_TITLE = `[E2E] Analyse Flow ${Date.now()}`;
const PDF_FIXTURE = path.resolve(__dirname, '../fixtures/test-contrat-travail.pdf');

test.describe('Parcours métier — analyse IA complète', () => {

  let caseFileUrl: string;

  test.fixme('créer dossier → upload → analyse → synthèse → export PDF', async ({ page }) => {
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

    // La création ne navigue pas automatiquement vers le détail : le dossier
    // apparaît dans la liste (la dialog se ferme + snackbar). On l'ouvre.
    const dossierLink = page.getByRole('link', { name: DOSSIER_TITLE });
    await expect(dossierLink).toBeVisible({ timeout: 10_000 });
    await dossierLink.click();

    // Attendre la navigation vers le détail du dossier
    await expect(page).toHaveURL(/\/case-files\/[0-9a-f-]{36}/, { timeout: 10_000 });
    caseFileUrl = page.url();

    // ── 3. Uploader un document ──
    // exact: true — le header repliable de la section « DOCUMENTS » (F-244) a
    // role="button" et son nom accessible englobe le texte du bouton ; sans
    // exact on matche 2 éléments (violation strict mode).
    await page.getByRole('button', { name: 'Ajouter des documents', exact: true }).click();

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
    // Le lien "Voir la synthèse" apparaît quand l'analyse est DONE.
    // F-244 SF-244-04 : un 2ᵉ lien "Voir la synthèse" existe désormais dans
    // l'onglet « Décision » (bandeau de couplage). On scope le sélecteur à
    // l'onglet « Analyse » pour rester non ambigu (strict mode Playwright).
    const synthesisLink = page
      .locator('[data-tab-panel="analyse"]')
      .getByRole('link', { name: /voir la synthèse/i });
    await expect(synthesisLink).toBeVisible({ timeout: 120_000 }); // 2 min max pour l'IA

    // ── 6. Consulter la synthèse ──
    await synthesisLink.click();
    await expect(page).toHaveURL(/\/synthesis/, { timeout: 10_000 });

    // La synthèse est générée de façon asynchrone après l'analyse : juste après,
    // la page peut afficher « Synthèse non disponible » (version pas encore
    // requêtable côté API) avant que la synthèse n'apparaisse. On recharge la
    // page jusqu'à obtenir l'en-tête de synthèse (« Synthèse initiale/enrichie »).
    const synthesisHeader = page.getByText(/synthèse (initiale|enrichie)/i).first();
    await expect(async () => {
      if (!(await synthesisHeader.isVisible().catch(() => false))) {
        await page.reload();
      }
      await expect(synthesisHeader).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 150_000 });
    // La synthèse se génère en streaming : attendre la fin de génération avant
    // de poursuivre (les sections de contenu — chronologie, faits… — sont
    // conditionnelles à ce que l'IA a extrait, donc non testées ici).
    await expect(page.getByText(/en cours de génération/i))
      .toBeHidden({ timeout: 120_000 });

    // ── 7. Exporter PDF ──
    // L'export charge pdfmake en import dynamique (gros chunk + VFS polices)
    // après 5 appels HTTP forkJoin — prévoir une marge généreuse.
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 45_000 }),
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
