import { defineConfig, devices } from '@playwright/test';

/**
 * Config dédiée aux captures d'écran marketing.
 * Viewport 1920×1080, fond propre, pas de retry.
 *
 * Lancer avec :
 *   cd e2e && npx playwright test --config=screenshots/playwright.screenshots.config.ts
 *
 * Prérequis :
 *   - Backend sur :8080 (profil local ou staging via E2E_BASE_URL)
 *   - Frontend sur :4200 (ou E2E_BASE_URL)
 *   - Un dossier avec synthèse complète existe en base (voir README dans ce dossier)
 */
export default defineConfig({
  testDir: '.',
  testMatch: 'capture.spec.ts',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'off',
    screenshot: 'off', // géré manuellement dans le script
    video: 'off',
    viewport: { width: 1920, height: 1080 },
    deviceScaleFactor: 2, // retina — captures 2x pour qualité pro
  },
  projects: [
    {
      name: 'chromium-hd',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1920, height: 1080 } },
    },
  ],
  globalSetup: '../global-setup.ts',
});
