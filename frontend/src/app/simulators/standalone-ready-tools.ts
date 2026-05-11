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

  // SF-163-02b — Travail FR (19).
  'F-DT-10-rupture-conv-validity',
  'F-DT-11-harcelement-licenciement-nul',
  'F-DT-12-discrimination-dommages-interets',
  'F-DT-13-licenciement-economique',
  'F-DT-15-inaptitude',
  'F-DT-16-licenciement-nul-detection',
  'F-DT-17-indemnite-precarite-cdd',
  'F-DT-18-fin-mission-interim',
  'F-DT-19-heures-sup',
  'F-DT-21-travail-dissimule',
  'F-DT-22-requalification-cdd-cdi',
  'F-DT-23-requalification-interim-cdi',
  'F-DT-24-non-concurrence',
  'F-DT-26-conges-payes-indemnite',
  'F-DT-30-protection-rp',
  'F-DT-31-transaction',
  'F-DT-33-at-mp',
  'F-DT-34-refere-prudhomal',
  'F-DT-35-contestation-are-fr',

  // SF-163-02b — Travail BE (3).
  'F-DT-27-motif-grave-be',
  'F-DT-28-avantages-conventionnels-be',
  'F-DT-29-credit-temps-be',

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
  // SF-163-02d — Immigration FR (17 outils dispatcher backend).
  'F-IM-08-oqtf-avec-delai-fr',
  'F-IM-08-oqtf-sans-delai-fr',
  'F-IM-08-referes-admin-fr',
  'F-IM-09-aes-metiers-tension',
  'F-IM-09-aes-famille',
  'F-IM-09-aes-etudiant',
  'F-IM-09-aes-humanitaire',
  'F-IM-11-changement-statut',
  'F-IM-12-asile-avance',
  'F-IM-13-naturalisation',
  'F-IM-17-regime-algerien',
  'F-IM-19-mineurs',
  'F-IM-20-mesures-eloignement',
  'F-IM-21-jld-retention-fr',
  'F-IM-22-dublin-recours-fr',
  'F-IM-23-crrv-refus-visa-fr',
  'F-IM-24-victime-violences-l4256-fr',
  // SF-163-02d — Immigration BE (5 outils dispatcher backend).
  'F-IM-08-annexe13-be',
  'F-IM-14-9bis-humanitaire-be',
  'F-IM-14-9ter-medical-be',
  'F-IM-14-40bis-cohabitant-ue-be',
  'F-IM-14-40ter-familial-belge-be',
]);
