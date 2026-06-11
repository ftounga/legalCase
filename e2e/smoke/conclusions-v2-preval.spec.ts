import { test, expect } from '@playwright/test';
import { loginLocal } from '../helpers/auth.helper';

// Validation visuelle Conclusions V2 (F-264 éditeur, F-265 co-rédaction, F-266 export/survol)
// sur le dossier de pré-validation déjà généré. Lancer avec --config=conclusions-v2.config.ts
// (E2E_BASE_URL=staging, SANS global-setup pour ne pas supprimer le dossier).
const CASE_ID = process.env['PREVAL_CASE_ID'] ?? '45fb8045-4d4f-4ed1-a9ec-bc876d7d9dc3';

test('Conclusions V2 — éditeur + co-rédaction + export rendus en mode édition', async ({ page }) => {
  test.setTimeout(120_000);
  await loginLocal(page);

  // Aller au détail dossier, onglet Décision
  await page.goto(`/case-files/${CASE_ID}`);
  await page.getByRole('tab', { name: 'Décision' }).click();

  // Section conclusions : l'acte généré doit être présent
  const section = page.locator('app-conclusions-section');
  await expect(section).toBeVisible({ timeout: 20_000 });
  await section.scrollIntoViewIfNeeded();

  // Entrer en mode édition
  const editBtn = section.getByRole('button', { name: /modifier/i }).first();
  await expect(editBtn).toBeVisible({ timeout: 15_000 });
  await editBtn.click();

  // F-264 — barre d'outils markdown + aperçu live
  const editor = section.locator('textarea').first();
  await expect(editor).toBeVisible();
  // boutons barre d'outils (au moins Gras + Titre)
  await expect(section.getByRole('button', { name: /gras/i })).toBeVisible();
  await expect(section.getByRole('button', { name: /titre/i }).first()).toBeVisible();
  // aperçu formaté (réutilise app-conclusion-document)
  await expect(section.locator('app-conclusion-document')).toBeVisible();

  // F-265 — co-rédaction : sélecteur de section + champ instruction
  await expect(section.getByLabel(/section à régénérer/i)).toBeVisible();
  await expect(section.getByLabel(/instruction de régénération/i)).toBeVisible();

  // Preuve visuelle du mode édition (F-264 + F-265)
  await page.screenshot({ path: 'test-results/conclusions-v2-edit-mode.png', fullPage: true });

  // Sortir du mode édition (les contrôles d'export vivent en lecture)
  await section.getByRole('button', { name: /annuler/i }).first().click();
  await expect(editor).toBeHidden({ timeout: 10_000 });

  // F-266 — export à en-tête cabinet : bouton-toggle → révèle le champ
  const cabinetToggle = section.locator('[data-testid="toggle-cabinet-header-btn"]');
  await expect(cabinetToggle).toBeVisible({ timeout: 10_000 });
  await cabinetToggle.scrollIntoViewIfNeeded();
  await cabinetToggle.click();
  await expect(section.getByLabel(/en-tête du cabinet/i)).toBeVisible({ timeout: 10_000 });
  await page.screenshot({ path: 'test-results/conclusions-v2-export-header.png', fullPage: true });

  // Capture textuelle des contrôles trouvés (log)
  const toolbarBtns = await section.locator('app-markdown-toolbar button, [role="toolbar"] button').count();
  console.log(`[conclusions-v2] boutons barre d'outils: ${toolbarBtns}`);
});
