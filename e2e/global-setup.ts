import { request } from '@playwright/test';

/**
 * Nettoyage avant chaque run E2E — supprime tous les dossiers du compte e2e.
 *
 * Le nettoyage passe par l'API REST (et non par une connexion SQL directe) :
 * la suite cible un environnement distant (staging), une connexion
 * `psql -h localhost` n'a aucun sens et échoue silencieusement. Sans nettoyage,
 * le workspace E2E accumule les dossiers d'un run à l'autre et finit par buter
 * sur la limite de dossiers ouverts du plan (`402 CASE_FILE_OPEN_LIMIT_EXCEEDED`),
 * ce qui casse la création de dossier dans happy-path et case-analysis-flow.
 *
 * On utilise le contexte `request` de Playwright et NON le `fetch` global de
 * Node : sur le runner CI, `fetch` (undici) échouait avec « fetch failed » en
 * atteignant staging, alors que la pile réseau de Playwright atteint bien
 * staging (les tests navigateur passent). Le contexte `request` gère en outre
 * les cookies de session automatiquement (pas d'extraction manuelle de
 * `Set-Cookie`) et suit les redirections nginx.
 *
 * Échec silencieux : un nettoyage raté ne doit pas bloquer le run.
 */
async function globalSetup(): Promise<void> {
  const baseURL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';
  const email = process.env['E2E_LOCAL_EMAIL'] ?? 'e2e@legalcase.test';
  const password = process.env['E2E_LOCAL_PASSWORD'] ?? 'E2ePassword1!';

  const ctx = await request.newContext({ baseURL, ignoreHTTPSErrors: true });
  try {
    const loginRes = await ctx.post('/api/v1/auth/login', {
      data: { email, password },
    });
    if (!loginRes.ok()) {
      console.warn(`[E2E] Login de nettoyage échoué (${loginRes.status()}) — run sans nettoyage.`);
      return;
    }

    // Le cookie de session posé par le login est réutilisé automatiquement
    // par les requêtes suivantes du même contexte.
    const listRes = await ctx.get('/api/v1/case-files', {
      params: { page: 0, size: 200 },
    });
    if (!listRes.ok()) {
      console.warn(`[E2E] Liste des dossiers inaccessible (${listRes.status()}) — run sans nettoyage.`);
      return;
    }

    const ids: string[] = (((await listRes.json()) as { content?: { id: string }[] }).content ?? [])
      .map(c => c.id);

    for (const id of ids) {
      await ctx.delete(`/api/v1/case-files/${id}`);
    }
    console.log(`[E2E] ${ids.length} dossier(s) de test nettoyé(s).`);
  } catch (e) {
    console.warn('[E2E] Nettoyage des dossiers de test impossible :', (e as Error).message);
  } finally {
    await ctx.dispose();
  }
}

export default globalSetup;
