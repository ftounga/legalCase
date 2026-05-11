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
 * SF-163-02b/c/d enrichissent progressivement cette whitelist au fur et à
 * mesure que les composants sont refactorés (vagues par domaine / pays).
 */
export const STANDALONE_READY_TOOL_IDS: ReadonlySet<string> = new Set<string>([
  // SF-163-02a — pilote (Travail FR).
  'F-DT-08-licenciement-validity',
  // SF-163-02c — Famille FR + BE.
  'F-FA-05-partage-immobilier',
  'F-FA-08-divorce-alteration',
  'F-FA-09-divorce-faute',
  'F-FA-10-divorce-accepte',
  'F-FA-11-desunion-irremediable-be',
  'F-FA-13-revisions-post-divorce',
  'F-FA-14-ordonnance-protection',
  'F-FA-15-recompenses',
  'F-FA-19-autorite-parentale',
  'F-FA-19-changement-residence',
  'F-FA-19-desaccords-parentaux',
  'F-FA-21-separation-corps',
  'F-FA-24-rapport-succession',
  'F-FA-24-reserve-heriditaire',
  'F-FA-26-changement-etat-civil',
]);
