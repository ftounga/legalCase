import { test, expect } from '@playwright/test';

/**
 * Smoke tests — Page publique /employeur (F-DRH-01 SF-DRH-01-01)
 *
 * Garantit :
 * - Route /employeur publique (no-auth), charge < 400
 * - Hero avec le titre de l'offre employeur visible
 * - CTA « Réserver une démo » Calendly présent (target _blank)
 * - Aucun prix affiché, pas de self-serve
 * - SEO : title + canonical /employeur
 */

const CALENDLY_URL = 'https://calendly.com/tounga-franck-ng-itconsulting/30min';

test.describe('Page employeur /employeur — F-DRH-01 SF-DRH-01-01', () => {
  test('page /employeur charge et affiche le hero', async ({ page }) => {
    const response = await page.goto('/employeur');
    expect(response?.status()).toBeLessThan(400);

    const h1 = page.locator('h1.emp-hero-title');
    await expect(h1).toBeVisible({ timeout: 8000 });
    await expect(h1).toContainText("exposition prud'homale");
  });

  test('CTA « Réserver une démo » Calendly présent (hero + final)', async ({ page }) => {
    await page.goto('/employeur');
    const ctas = page.locator('a.emp-btn--primary');
    expect(await ctas.count()).toBeGreaterThanOrEqual(2);

    const last = ctas.last();
    await last.scrollIntoViewIfNeeded();
    await expect(last).toBeVisible();
    expect(await last.getAttribute('href')).toBe(CALENDLY_URL);
    expect(await last.getAttribute('target')).toBe('_blank');
  });

  test('section confiance — hébergement EU / RGPD / isolation', async ({ page }) => {
    await page.goto('/employeur');
    const trust = page.locator('.emp-trust');
    await trust.scrollIntoViewIfNeeded();
    await expect(trust).toContainText('Union Européenne');
    await expect(trust).toContainText('RGPD');
  });

  test('SEO — title employeur + canonical /employeur', async ({ page }) => {
    await page.goto('/employeur');
    await expect(page).toHaveTitle(/Employeur/);
    const canonical = await page
      .locator('head link[rel="canonical"]')
      .getAttribute('href');
    expect(canonical).toBe('https://legalcase.fr/employeur');
  });
});
