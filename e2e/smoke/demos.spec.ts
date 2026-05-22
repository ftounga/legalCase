import { test, expect } from '@playwright/test';

/**
 * Smoke tests — Galerie démos /demos (F-254 SF-254-01)
 *
 * Garantit :
 * - Route /demos publique, charge 200
 * - Au moins 1 card visible
 * - Clic sur la première card ouvre un player YouTube inline
 * - CTAs Essai gratuit (/login) et Calendly présents
 * - Page indexable : title + meta description + canonical + JSON-LD
 */

test.describe('Galerie /demos — F-254 SF-254-01', () => {

  test('page /demos charge 200 et affiche le hero', async ({ page }) => {
    const response = await page.goto('/demos');
    expect(response?.status()).toBeLessThan(400);

    const h1 = page.locator('h1.demos-title');
    await expect(h1).toBeVisible({ timeout: 8000 });
    await expect(h1).toContainText('LegalCase');
  });

  test('au moins 1 card de démo visible dans la grille', async ({ page }) => {
    await page.goto('/demos');
    const cards = page.locator('li.demos-card');
    const count = await cards.count();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('clic sur une card ouvre un player YouTube inline', async ({ page }) => {
    await page.goto('/demos');
    const firstCardButton = page.locator('button.demos-card-thumb').first();
    await firstCardButton.click();

    const iframe = page.locator('.demos-card-player iframe');
    await expect(iframe).toBeVisible();
    const src = await iframe.getAttribute('src');
    expect(src).toContain('youtube-nocookie.com/embed/');
  });

  test('bouton fermer revient à la grille', async ({ page }) => {
    await page.goto('/demos');
    await page.locator('button.demos-card-thumb').first().click();
    await page.locator('button.demos-card-close').click();
    await expect(page.locator('.demos-card-player')).toHaveCount(0);
  });

  test('CTAs de sortie présents — Essai gratuit + Calendly', async ({ page }) => {
    await page.goto('/demos');
    const primary = page.locator('.demos-cta-btn--primary');
    await primary.scrollIntoViewIfNeeded();
    await expect(primary).toBeVisible();
    await expect(primary).toContainText('Essai gratuit');

    const secondary = page.locator('.demos-cta-btn--secondary');
    await expect(secondary).toBeVisible();
    const href = await secondary.getAttribute('href');
    expect(href).toContain('calendly.com');
    expect(await secondary.getAttribute('target')).toBe('_blank');
  });

  test('SEO meta — title, description, canonical, JSON-LD WebPage', async ({ page }) => {
    await page.goto('/demos');
    await expect(page).toHaveTitle(/Démonstrations/);

    const description = await page.locator('head meta[name="description"]').getAttribute('content');
    expect(description).toBeTruthy();
    expect(description!.length).toBeGreaterThan(20);

    const canonical = await page.locator('head link[rel="canonical"]').getAttribute('href');
    expect(canonical).toBe('https://legalcase.fr/demos');

    const jsonLdTags = page.locator('head script[type="application/ld+json"]');
    const jsonLdCount = await jsonLdTags.count();
    expect(jsonLdCount).toBeGreaterThan(0);
  });

  test('landing — CTA « Voir toutes les démos » pointe vers /demos', async ({ page }) => {
    await page.goto('/');
    const cta = page.locator('[data-testid="demos-gallery-link"]');
    await cta.scrollIntoViewIfNeeded();
    await expect(cta).toBeVisible();
    const href = await cta.getAttribute('href');
    expect(href).toBe('/demos');
  });

  test('landing — footer expose un lien « Démonstrations » vers /demos', async ({ page }) => {
    await page.goto('/');
    const footerLink = page.locator('footer a[href="/demos"]');
    await footerLink.scrollIntoViewIfNeeded();
    await expect(footerLink).toBeVisible();
    await expect(footerLink).toContainText('Démonstrations');
  });
});
