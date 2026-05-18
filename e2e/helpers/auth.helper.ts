import { Page, expect } from '@playwright/test';

export const TEST_USER = {
  email: process.env['E2E_LOCAL_EMAIL'] ?? 'e2e@legalcase.test',
  password: process.env['E2E_LOCAL_PASSWORD'] ?? 'E2ePassword1!',
};

/**
 * Remplit le formulaire de login local et le soumet.
 *
 * La page /login est pré-rendue en SSG (`ng-server-context="ssg"`) : le bouton
 * « Se connecter » arrive `disabled="true"` dans le HTML statique. Tant
 * qu'Angular n'a pas hydraté la page, `.fill()` écrit bien dans les `<input>`
 * du DOM mais n'alimente pas les `FormControl` de la reactive form — le
 * formulaire reste invalide et le bouton reste désactivé.
 *
 * On ré-essaie donc le remplissage jusqu'à ce que le bouton devienne actif :
 * c'est la preuve que l'hydratation est terminée et que les valeurs ont bien
 * été prises en compte par le formulaire.
 */
export async function submitLoginForm(
  page: Page,
  email: string,
  password: string
): Promise<void> {
  const emailField = page.getByLabel('Email');
  const passwordField = page.getByLabel('Mot de passe');
  // exact: true pour ne pas matcher "Se connecter avec Google" / "Se connecter avec Microsoft"
  const submitBtn = page.getByRole('button', { name: 'Se connecter', exact: true });

  await expect(async () => {
    await emailField.fill(email);
    await passwordField.fill(password);
    await expect(submitBtn).toBeEnabled({ timeout: 1000 });
  }).toPass({ timeout: 15000 });

  await submitBtn.click();
}

/**
 * Connecte l'utilisateur de test via l'auth locale (email/mdp).
 * Prérequis : le compte doit exister en base et être vérifié.
 */
export async function loginLocal(page: Page): Promise<void> {
  // /login/ avec slash final : évite la redirection 301 de nginx
  // (qui rétrograde au passage le schéma en http).
  await page.goto('/login/');
  await submitLoginForm(page, TEST_USER.email, TEST_USER.password);
  await page.waitForURL('**/case-files', { timeout: 10000 });
}

/**
 * Déconnecte l'utilisateur courant via le menu utilisateur du shell.
 */
export async function logout(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Menu utilisateur' }).click();
  await page.getByRole('menuitem', { name: /déconnecter/i }).click();
  await page.waitForURL('**/login', { timeout: 5000 });
}
