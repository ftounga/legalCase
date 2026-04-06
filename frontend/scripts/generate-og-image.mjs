/**
 * Generates the OG image (1200x630) for AI LegalCase.
 * Run: node frontend/scripts/generate-og-image.mjs
 * Requires playwright-core (available in e2e/node_modules).
 */
import { chromium } from '../../e2e/node_modules/playwright-core/index.mjs';

const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Merriweather:wght@600;700&display=swap');

  * { margin: 0; padding: 0; box-sizing: border-box; }

  body {
    width: 1200px;
    height: 630px;
    background: #1A3A5C;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-family: 'Inter', sans-serif;
    overflow: hidden;
    position: relative;
  }

  /* Subtle grid pattern */
  body::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
    background-size: 40px 40px;
  }

  .content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 32px;
  }

  .logo-row {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .logo-shield {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #C9973A 0%, #E8C068 50%, #C9973A 100%);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 24px rgba(0,0,0,0.3);
  }

  .logo-shield svg {
    width: 44px;
    height: 44px;
    fill: #1A3A5C;
  }

  .logo-text {
    font-family: 'Merriweather', serif;
    font-size: 48px;
    font-weight: 700;
    letter-spacing: -0.5px;
  }

  .logo-text .legal { color: #FFFFFF; }
  .logo-text .case { color: #C9973A; }
  .logo-text .ai { color: rgba(255,255,255,0.6); font-size: 36px; margin-right: 8px; }

  .tagline {
    font-family: 'Merriweather', serif;
    font-size: 32px;
    font-weight: 600;
    color: #FFFFFF;
    text-align: center;
    line-height: 1.4;
    max-width: 800px;
  }

  .subtitle {
    font-size: 20px;
    font-weight: 400;
    color: rgba(255,255,255,0.7);
    text-align: center;
    max-width: 700px;
    line-height: 1.5;
  }

  .badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: rgba(201, 151, 58, 0.15);
    border: 1px solid rgba(201, 151, 58, 0.4);
    border-radius: 24px;
    padding: 10px 24px;
    color: #E8C068;
    font-size: 16px;
    font-weight: 500;
  }

  .divider {
    width: 80px;
    height: 3px;
    background: #C9973A;
    border-radius: 2px;
  }

  .bottom {
    position: absolute;
    bottom: 28px;
    display: flex;
    align-items: center;
    gap: 24px;
    color: rgba(255,255,255,0.4);
    font-size: 14px;
  }

  .bottom .dot {
    width: 4px;
    height: 4px;
    background: rgba(255,255,255,0.3);
    border-radius: 50%;
  }
</style>
</head>
<body>
  <div class="content">
    <div class="logo-row">
      <div class="logo-shield">
        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
      </div>
      <div class="logo-text">
        <span class="ai">AI</span><span class="legal">Legal</span><span class="case">Case</span>
      </div>
    </div>
    <div class="divider"></div>
    <div class="tagline">Analyse IA pour avocats<br>en droit du travail</div>
    <div class="subtitle">Synthèse structurée, chronologie, risques juridiques et points de droit en quelques minutes</div>
    <div class="badge">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1.33v13.34M1.33 8h13.34" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
      Essai gratuit 14 jours
    </div>
  </div>
  <div class="bottom">
    <span>NG-Consulting</span>
    <span class="dot"></span>
    <span>Paris, France</span>
    <span class="dot"></span>
    <span>legalcase.ng-itconsulting.com</span>
  </div>
</body>
</html>`;

async function generate() {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
  await page.setContent(html, { waitUntil: 'networkidle' });
  await page.screenshot({
    path: 'frontend/public/og-image.png',
    type: 'png',
    clip: { x: 0, y: 0, width: 1200, height: 630 }
  });
  await browser.close();
  console.log('OG image generated: frontend/public/og-image.png (1200x630)');
}

generate();
