import { defineConfig, devices } from '@playwright/test';

// Config DÉDIÉE validation Conclusions V2 — SANS global-setup (ne pas wiper le dossier de test).
export default defineConfig({
  testDir: './smoke',
  testMatch: /conclusions-v[23].*\.spec\.ts/,
  timeout: 120_000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'https://staging.legalcase.fr',
    trace: 'off',
    screenshot: 'on',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // PAS de globalSetup volontairement.
});
