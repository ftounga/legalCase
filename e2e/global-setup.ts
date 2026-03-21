import { execSync } from 'child_process';

/**
 * Nettoyage avant chaque run E2E :
 * supprime les dossiers créés lors des runs précédents pour le compte e2e.
 */
async function globalSetup() {
  const pgPassword = 'legalcase';
  const sql = `
    DELETE FROM case_files WHERE workspace_id = (
      SELECT w.id FROM workspaces w
      JOIN users u ON w.owner_user_id = u.id
      WHERE u.email = 'e2e@legalcase.test'
    );
  `;
  try {
    execSync(
      `PGPASSWORD=${pgPassword} psql -h localhost -U legalcase -d legalcasedb -c "${sql.replace(/\n/g, ' ')}"`,
      { stdio: 'pipe' }
    );
    console.log('[E2E] Dossiers de test nettoyés.');
  } catch (e) {
    console.warn('[E2E] Impossible de nettoyer les dossiers de test (DB inaccessible ?)');
  }
}

export default globalSetup;
