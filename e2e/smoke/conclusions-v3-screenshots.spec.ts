import { test } from '@playwright/test';
import { loginLocal } from '../helpers/auth.helper';

// Captures F-267 (page dédiée conclusions) + F-268 (onglets outils). Config conclusions-v2 (sans global-setup).
const CASE_ID = process.env['DEMO_CASE_ID'] ?? '';

test('F-267 + F-268 — captures page conclusions + onglets outils', async ({ page }) => {
  test.setTimeout(120_000);
  await loginLocal(page);

  // F-267 — page dédiée conclusions (la « feuille »)
  await page.goto(`/case-files/${CASE_ID}/conclusions`);
  await page.waitForTimeout(3500);
  await page.screenshot({ path: 'test-results/f267-page-conclusions.png', fullPage: true });
  // capture viewport (haut de page : en-tête + début feuille)
  await page.screenshot({ path: 'test-results/f267-page-conclusions-top.png', fullPage: false });

  // F-268 — onglets outils (onglet Décision)
  await page.goto(`/case-files/${CASE_ID}?section=decision`);
  await page.waitForTimeout(3500);
  await page.screenshot({ path: 'test-results/f268-onglets-outils.png', fullPage: false });
  // déplier un autre onglet de thème pour preuve
  const tabs = page.locator('app-decisional-tools-panel [role="tab"]');
  const n = await tabs.count();
  console.log(`[f268] onglets de thème: ${n}`);
  if (n > 1) {
    await tabs.nth(1).click();
    await page.waitForTimeout(800);
    await page.screenshot({ path: 'test-results/f268-onglet-2.png', fullPage: false });
  }
});
