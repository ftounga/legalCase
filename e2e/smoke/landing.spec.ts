import { test, expect } from '@playwright/test';

/**
 * Smoke tests — Landing page V3 (F-158)
 *
 * Garantissent que la landing reflète le repositionnement F-158 :
 * - Hero « chaîne de valeur » : de vos pièces jusqu'aux conclusions (SF-158-04)
 * - Plateforme de 250+ outils décisionnels (vs analyseur de docs)
 * - Pricing V7 (SOLO 99 / TEAM 219 / PRO 429) — pas V1 (59/119/249)
 * - Composant catalogue 250+ outils filtrable visible
 * - Section OCR + Vision visible
 * - Lien Blog dans la nav
 */

test.describe('Landing V3 — repositionnement F-158', () => {

  test('H1 nomme la chaîne de valeur jusqu\'aux conclusions', async ({ page }) => {
    await page.goto('/');
    const h1 = page.locator('h1').first();
    await expect(h1).toContainText('conclusions', { timeout: 8000 });
    await expect(h1).not.toContainText('92');
  });

  test('stat hero — 250+ outils décisionnels intégrés', async ({ page }) => {
    await page.goto('/');
    const counter = page.locator('.hero-stats .counter[data-target="250"]');
    await expect(counter).toBeVisible();
  });

  test('pricing V7 — Solo 99, Team 219, Pro 429 affichés', async ({ page }) => {
    await page.goto('/');
    // Scroll vers la section pricing pour s'assurer du rendu
    await page.locator('#tarifs').scrollIntoViewIfNeeded();
    await expect(page.locator('.pricing-card .price-amount').nth(1)).toContainText('99 €');
    await expect(page.locator('.pricing-card .price-amount').nth(2)).toContainText('219 €');
    await expect(page.locator('.pricing-card .price-amount').nth(3)).toContainText('429 €');
  });

  test('catalogue 250+ outils — composant rendu et au moins 1 card', async ({ page }) => {
    await page.goto('/');
    const showcase = page.locator('app-landing-tools-showcase');
    await showcase.scrollIntoViewIfNeeded();
    await expect(showcase).toBeVisible();
    await expect(showcase.locator('.tool-card').first()).toBeVisible();
  });

  test('section OCR + Vision présente', async ({ page }) => {
    await page.goto('/');
    const ocrCard = page.locator('.ocr-vision-card__badge', { hasText: 'Legal OCR' });
    const visionCard = page.locator('.ocr-vision-card__badge', { hasText: 'Legal Vision' });
    await ocrCard.scrollIntoViewIfNeeded();
    await expect(ocrCard).toBeVisible();
    await expect(visionCard).toBeVisible();
  });

  test('section #conclusions (rédaction de conclusions) présente', async ({ page }) => {
    await page.goto('/');
    const section = page.locator('section#conclusions');
    await section.scrollIntoViewIfNeeded();
    await expect(section).toBeVisible();
    await expect(section).toContainText('projet de conclusions');
  });

  test('nav header expose un lien /blog', async ({ page }) => {
    await page.goto('/');
    const blogLink = page.locator('header nav a[href="/blog"]');
    await expect(blogLink).toBeVisible();
  });

  test('catalogue — filtre Belgique réduit la liste', async ({ page }) => {
    await page.goto('/');
    const showcase = page.locator('app-landing-tools-showcase');
    await showcase.scrollIntoViewIfNeeded();
    const initialCount = await showcase.locator('.tool-card:not(.tool-card--empty)').count();
    expect(initialCount).toBeGreaterThan(50);

    // Cliquer sur le filtre Belgique
    await showcase.locator('.filter-row[aria-label="Pays"] .chip', { hasText: 'Belgique' }).click();
    const beCount = await showcase.locator('.tool-card:not(.tool-card--empty)').count();
    expect(beCount).toBeLessThan(initialCount);
    expect(beCount).toBeGreaterThan(0);
  });
});
