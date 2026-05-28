import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-11b — Helper partagé pour le pré-remplissage IA de
 * {@code CongeEducationPayeRegionSectionComponent} (F-219, BE uniquement
 * — Loi du 22/01/1985 Section 6 régionalisée 2014 — WBR / FLA VOV /
 * BXL).
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
 *   <li>{@code region} — région du lieu de travail. Donnée employeur
 *       (siège d'exploitation au sens DmfA ONSS), pas extractible de
 *       manière fiable depuis le dossier salarié individuel ; risque
 *       d'erreur élevé si déduit du domicile salarié (région compétente
 *       = lieu de travail, pas domicile). Saisie avocat depuis le
 *       contrat de travail / fiche signalétique employeur.</li>
 *   <li>{@code typeFormation} — qualification juridique de la formation
 *       selon la typologie régionale (5 cas dont HORS_LISTE_AGREEE) —
 *       relève du jugement avocat sur la base de la convention de
 *       formation et de la liste agréée régionale en vigueur.</li>
 *   <li>{@code heuresFormationAnnuelles} — donnée de la convention de
 *       formation / programme pédagogique, non extractible de la pipeline
 *       Travail BE actuelle.</li>
 *   <li>{@code tauxOccupation} — donnée employeur (DmfA ONSS).
 *       Théoriquement déductible d'un contrat de travail mais V1 :
 *       pipeline Travail BE n'extrait pas le taux d'occupation
 *       contractuel — saisie avocat depuis le contrat.</li>
 *   <li>{@code publicFragilise} — qualification juridique au sens du
 *       décret VOV FLA (peu qualifié, sans CESS notamment) — saisie
 *       avocat depuis le dossier formation initiale.</li>
 *   <li>{@code formationAgreeeRegion} — paramétrage juridique (liste
 *       agréée régionale, périodiquement révisée) — vérification avocat
 *       requise auprès du ministère régional de l'emploi / formation.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code conge-education-paye-region-section-prefill-rules.spec.ts}.</p>
 */
export const CongeEducationPayeRegionSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (contrat de travail, convention de formation,
   * liste agréée régionale, qualifications initiales du travailleur).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
