import { test, expect } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';
import { loginLocal } from '../helpers/auth.helper';

/**
 * Captures d'écran marketing — AI LegalCase
 *
 * Produit les 8 captures pour le brief designer (1920×1080 @2x).
 * Les fichiers sont enregistrés dans e2e/screenshots/output/.
 *
 * Prérequis : un dossier avec synthèse complète doit exister.
 * Renseigner DEMO_CASE_ID dans .env.local ou en variable d'environnement.
 * Si absent, le script prend le premier dossier trouvé dans la liste.
 */

const OUTPUT_DIR = path.join(__dirname, 'output');

// Helpers
async function shot(page: import('@playwright/test').Page, name: string) {
  if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  const filePath = path.join(OUTPUT_DIR, `${name}.png`);
  await page.screenshot({ path: filePath, fullPage: false });
  console.log(`✅ Capture : ${name}.png`);
}

async function skipTour(page: import('@playwright/test').Page) {
  const skipBtn = page.locator('.tour-card__skip');
  if (await skipBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
    await skipBtn.click();
    await page.waitForTimeout(300);
  }
}

async function waitForTable(page: import('@playwright/test').Page) {
  await page.waitForSelector('mat-table, .case-files-table, mat-row', { timeout: 8000 }).catch(() => {});
}

test.describe('Captures marketing AI LegalCase', () => {

  // ─── 01 — Liste des dossiers ────────────────────────────────────────────

  test('01-liste-dossiers', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);
    await page.waitForTimeout(500); // laisser les données se stabiliser

    await shot(page, '01-liste-dossiers');
  });

  // ─── 02 — Product tour étape 0 (Bienvenue) ──────────────────────────────

  test('02-tour-bienvenue', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');

    // Forcer l'affichage du tour en supprimant la clé localStorage
    await page.evaluate(() => {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith('onboarding_tour_done_')) localStorage.removeItem(key);
      }
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    await waitForTable(page);

    const tourCard = page.locator('.tour-card');
    await expect(tourCard).toBeVisible({ timeout: 5000 });

    await shot(page, '02-tour-bienvenue');
  });

  // ─── 03 — Tour étape 1 : bouton "Nouveau dossier" surligné ──────────────

  test('03-tour-nouveau-dossier', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');

    await page.evaluate(() => {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith('onboarding_tour_done_')) localStorage.removeItem(key);
      }
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    await waitForTable(page);

    const tourCard = page.locator('.tour-card');
    await expect(tourCard).toBeVisible({ timeout: 5000 });
    await page.locator('button', { hasText: 'Suivant' }).click();
    await page.waitForTimeout(400);

    await shot(page, '03-tour-nouveau-dossier');
  });

  // ─── 04 — Page dossier (détail, avec documents) ─────────────────────────

  test('04-detail-dossier', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);

    // Naviguer vers le premier dossier ou le DEMO_CASE_ID
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      const firstLink = page.locator('.case-file-link').first();
      await expect(firstLink).toBeVisible({ timeout: 6000 });
      await firstLink.click();
    }

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);

    await shot(page, '04-detail-dossier');
  });

  // ─── 05 — Bouton "Ajouter des documents" (upload zone) ──────────────────

  test('05-upload-zone', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);

    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      const firstLink = page.locator('.case-file-link').first();
      await expect(firstLink).toBeVisible({ timeout: 6000 });
      await firstLink.click();
    }

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);

    // Scroller jusqu'au bouton upload et zoomer la zone
    const uploadBtn = page.locator('[data-tour-target="upload-trigger-btn"]');
    await uploadBtn.scrollIntoViewIfNeeded();
    await page.waitForTimeout(300);

    await shot(page, '05-upload-zone');
  });

  // ─── 06 — Analyse en cours (spinner + barres progression) ───────────────

  test('06-analyse-en-cours', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);

    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      const firstLink = page.locator('.case-file-link').first();
      await expect(firstLink).toBeVisible({ timeout: 6000 });
      await firstLink.click();
    }

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);

    // Scroller vers la section Analyse IA
    const analyseSection = page.locator('.section-header--first, h2', { hasText: 'Analyse IA' }).first();
    await analyseSection.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(300);

    await shot(page, '06-analyse-ia');
  });

  // ─── 07 — Synthèse complète (haut de page) ──────────────────────────────

  test('07-synthese-haut', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);

    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}/synthesis`);
    } else {
      // Chercher un dossier avec lien synthèse visible
      const firstLink = page.locator('.case-file-link').first();
      await expect(firstLink).toBeVisible({ timeout: 6000 });
      await firstLink.click();
      await page.waitForLoadState('networkidle');

      const synthesisLink = page.locator('[data-tour-target="synthesis-link"]');
      const hasSynthesis = await synthesisLink.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasSynthesis) {
        await synthesisLink.click();
      } else {
        console.warn('⚠️  Aucune synthèse disponible — capture ignorée pour 07/08. Renseigner DEMO_CASE_ID.');
        return;
      }
    }

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);

    await shot(page, '07-synthese-haut');
  });

  // ─── 08 — Synthèse : section risques (scrollée) ─────────────────────────

  test('08-synthese-risques', async ({ page }) => {
    await loginLocal(page);
    await page.waitForLoadState('networkidle');
    await skipTour(page);
    await waitForTable(page);

    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}/synthesis`);
    } else {
      const firstLink = page.locator('.case-file-link').first();
      await expect(firstLink).toBeVisible({ timeout: 6000 });
      await firstLink.click();
      await page.waitForLoadState('networkidle');

      const synthesisLink = page.locator('[data-tour-target="synthesis-link"]');
      const hasSynthesis = await synthesisLink.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasSynthesis) {
        await synthesisLink.click();
      } else {
        console.warn('⚠️  Aucune synthèse disponible — capture ignorée pour 08. Renseigner DEMO_CASE_ID.');
        return;
      }
    }

    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);

    // Scroller vers les risques
    const risques = page.locator('h2, h3', { hasText: /risques/i }).first();
    await risques.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(400);

    await shot(page, '08-synthese-risques');
  });

});
