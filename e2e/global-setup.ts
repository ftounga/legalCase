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
 * Échec silencieux : un nettoyage raté ne doit pas bloquer le run.
 */
async function globalSetup(): Promise<void> {
  const baseUrl = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';
  const email = process.env['E2E_LOCAL_EMAIL'] ?? 'e2e@legalcase.test';
  const password = process.env['E2E_LOCAL_PASSWORD'] ?? 'E2ePassword1!';

  try {
    const loginRes = await fetch(`${baseUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!loginRes.ok) {
      console.warn(`[E2E] Login de nettoyage échoué (${loginRes.status}) — run sans nettoyage.`);
      return;
    }

    // Cookie de session : on ne garde que la paire name=value de chaque Set-Cookie.
    const cookie = (loginRes.headers.getSetCookie?.() ?? [])
      .map(c => c.split(';')[0])
      .join('; ');

    const listRes = await fetch(`${baseUrl}/api/v1/case-files?page=0&size=200`, {
      headers: { Cookie: cookie },
    });
    if (!listRes.ok) {
      console.warn(`[E2E] Liste des dossiers inaccessible (${listRes.status}) — run sans nettoyage.`);
      return;
    }

    const ids: string[] = (((await listRes.json()) as { content?: { id: string }[] }).content ?? [])
      .map(c => c.id);

    for (const id of ids) {
      await fetch(`${baseUrl}/api/v1/case-files/${id}`, {
        method: 'DELETE',
        headers: { Cookie: cookie },
      });
    }
    console.log(`[E2E] ${ids.length} dossier(s) de test nettoyé(s).`);
  } catch (e) {
    console.warn('[E2E] Nettoyage des dossiers de test impossible :', (e as Error).message);
  }
}

export default globalSetup;
