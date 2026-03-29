import { test } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

const OUTPUT_DIR = path.join(__dirname, 'output');

async function shot(page: import('@playwright/test').Page, name: string) {
  if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  const filePath = path.join(OUTPUT_DIR, `${name}.png`);
  await page.screenshot({ path: filePath, fullPage: false });
  console.log(`✅ Capture : ${name}.png`);
}

async function login(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(process.env['E2E_LOCAL_EMAIL'] ?? 'e2e@legalcase.test');
  await page.getByLabel('Mot de passe').fill(process.env['E2E_LOCAL_PASSWORD'] ?? 'E2ePassword1!');
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
  await page.waitForLoadState('networkidle', { timeout: 15000 });
  // Naviguer explicitement vers /case-files (contourne /onboarding)
  await page.goto('/case-files');
  await page.waitForLoadState('networkidle', { timeout: 15000 });
}

async function skipTour(page: import('@playwright/test').Page) {
  const skipBtn = page.locator('.tour-card__skip');
  if (await skipBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
    await skipBtn.click();
    await page.waitForTimeout(400);
  }
}

test.use({ timeout: 60000 });

test.describe('Captures marketing AI LegalCase', () => {

  // ─── 01 — Liste des dossiers ─────────────────────────────────────────────
  test('01-liste-dossiers', async ({ page }) => {
    await login(page);
    await skipTour(page);
    await page.waitForSelector('mat-row, .case-files-table, .empty-state', { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(600);
    await shot(page, '01-liste-dossiers');
  });

  // ─── 02 — Product tour étape 0 (Bienvenue) ───────────────────────────────
  test('02-tour-bienvenue', async ({ page }) => {
    await login(page);
    // Forcer le tour en supprimant les clés localStorage
    await page.evaluate(() => {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith('onboarding_tour_done_')) localStorage.removeItem(key);
      }
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);
    await shot(page, '02-tour-bienvenue');
  });

  // ─── 03 — Tour étape 1 : bouton "Nouveau dossier" surligné ───────────────
  test('03-tour-nouveau-dossier', async ({ page }) => {
    await login(page);
    await page.evaluate(() => {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith('onboarding_tour_done_')) localStorage.removeItem(key);
      }
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);

    const nextBtn = page.locator('button', { hasText: 'Suivant' });
    if (await nextBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await nextBtn.click();
      await page.waitForTimeout(500);
    }
    await shot(page, '03-tour-nouveau-dossier');
  });

  // ─── 04 — Page dossier (détail) ──────────────────────────────────────────
  test('04-detail-dossier', async ({ page }) => {
    await login(page);
    await skipTour(page);
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      await page.locator('.case-file-link').first().click().catch(() => {});
    }
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);
    await shot(page, '04-detail-dossier');
  });

  // ─── 05 — Zone upload (section Documents) ────────────────────────────────
  test('05-upload-zone', async ({ page }) => {
    await login(page);
    await skipTour(page);
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      await page.locator('.case-file-link').first().click().catch(() => {});
    }
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);
    // Scroller vers la section Documents (utilise evaluate pour éviter les fermetures de contexte)
    await page.evaluate(() => window.scrollBy(0, 600));
    await page.waitForTimeout(400);
    await shot(page, '05-upload-zone');
  });

  // ─── 06 — Section Analyse IA ──────────────────────────────────────────────
  test('06-analyse-ia', async ({ page }) => {
    await login(page);
    await skipTour(page);
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}`);
    } else {
      await page.locator('.case-file-link').first().click().catch(() => {});
    }
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(600);
    // Scroller vers la section Analyse IA
    await page.evaluate(() => window.scrollBy(0, 1200));
    await page.waitForTimeout(400);
    await shot(page, '06-analyse-ia');
  });

  // ─── 07 — Synthèse (haut de page) ────────────────────────────────────────
  test('07-synthese-haut', async ({ page }) => {
    await login(page);
    await skipTour(page);
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}/synthesis`);
    } else {
      await page.locator('[data-tour-target="synthesis-link"]').first().click().catch(() => {});
    }
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await shot(page, '07-synthese-haut');
  });

  // ─── 08 — Synthèse (section risques scrollée) ────────────────────────────
  test('08-synthese-risques', async ({ page }) => {
    await login(page);
    await skipTour(page);
    const demoCaseId = process.env['DEMO_CASE_ID'];
    if (demoCaseId) {
      await page.goto(`/case-files/${demoCaseId}/synthesis`);
    } else {
      await page.locator('[data-tour-target="synthesis-link"]').first().click().catch(() => {});
    }
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);
    // Scroller vers les risques
    await page.evaluate(() => window.scrollBy(0, 800));
    await page.waitForTimeout(500);
    await shot(page, '08-synthese-risques');
  });

});
