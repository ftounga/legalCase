import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-09b — Helper partagé pour le pré-remplissage IA de
 * {@code ElectionsSocialesBeSectionComponent} (F-219, BE uniquement —
 * Loi du 04/12/2007 + AR du 25/05/2012 + Loi 04/08/1996 art. 49 +
 * Loi 19/03/1991 protection candidats).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli). Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code cycleElectoral} — qualification métier dépendant de la date
 *       du scrutin programmé (cycle 4 ans national : 2024 / 2028 / 2032).
 *       Aucune donnée IA Travail BE ne porte cette qualification.</li>
 *   <li>{@code dateJourY} — date du scrutin choisie par l'employeur dans
 *       la fenêtre nationale 14 j fixée par AR. Donnée employeur, pas
 *       extractible d'un dossier salarié type.</li>
 *   <li>{@code effectifMoyenEtp} — effectif moyen ETP calculé sur 4
 *       trimestres précédents. Donnée RH employeur (bilan social
 *       SD Worx, DmfA, DRS), pas dans le dossier salarié.</li>
 *   <li>{@code uniteTechniqueExploitationConfirmee} — qualification
 *       juridique procédurale (accord paritaire ou décision
 *       juridictionnelle préalable confirmant l'UTE). Donnée procédurale
 *       interne employeur, non extractible.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code elections-sociales-be-section-prefill-rules.spec.ts}.</p>
 */
export const ElectionsSocialesBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * documents RH (bilan social, DmfA), la date AR du cycle et le PV
   * paritaire d'UTE.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
