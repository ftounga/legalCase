/**
 * F-163 / SF-163-02a — Whitelist des `tool_id` dont le composant décisionnel
 * frontend a été refactoré pour supporter le mode simulateur autonome
 * (`@Input() standaloneMode`).
 *
 * Cette whitelist est lue par :
 *   - `SimulatorsCatalogPageComponent.onCardClick()` : si le `tool_id` est
 *     présent, le clic navigue vers `/simulators/:toolId` (runner) au lieu
 *     d'ouvrir le dialog pédagogique « Créer un dossier » (rétrocompat).
 *   - `SimulatorRunnerPageComponent` : si le `tool_id` reçu n'est pas dans
 *     la whitelist, affiche un message « Disponible bientôt » (le composant
 *     n'a pas encore le mode standalone implémenté).
 *
 * SF-163-02b/c/d enrichiront progressivement cette whitelist au fur et à
 * mesure que les composants seront refactorés (vagues par domaine / pays).
 */
export const STANDALONE_READY_TOOL_IDS: ReadonlySet<string> = new Set<string>([
  'F-DT-08-licenciement-validity',
]);
