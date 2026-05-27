/**
 * SF-219-02b : modèle frontend de l'outil F-219 — RCC BE longue carrière.
 *
 * <p>BE uniquement — <b>Loi du 26/12/2013</b> (pacte de solidarité entre
 * les générations) + <b>CCT n° 17 du 19/12/1974</b> (CNT — RCC) +
 * <b>AR du 03/05/2007 art. 3</b> (régime spécifique longue carrière —
 * conditions âge ≥ 59 ans à la fin du contrat + carrière ≥ 40 ans).
 * Aucun équivalent strict en droit français.</p>
 *
 * <p>Contrat figé dans SF-219-02 backend
 * ({@code RccBeLongueCarriereResponse} +
 * {@code RccBeLongueCarriereRequest}).</p>
 *
 * <p>Verdict 4 états :
 * <ul>
 *   <li>{@code ELIGIBLE_RCC_LONGUE_CARRIERE} — toutes conditions remplies,
 *       RCC longue carrière ouvert + indemnité complémentaire indicative
 *       (50 % de la différence rémunération nette / allocation chômage).</li>
 *   <li>{@code INELIGIBLE_AGE_INSUFFISANT} — âge à la fin du contrat
 *       &lt; 59 ans.</li>
 *   <li>{@code INELIGIBLE_CARRIERE_INSUFFISANTE} — carrière professionnelle
 *       &lt; 40 ans.</li>
 *   <li>{@code INELIGIBLE_DEMISSION} — la démission est exclue du dispositif
 *       (CCT n° 17 art. 3 — réservé au licenciement par l'employeur).</li>
 * </ul></p>
 */

/**
 * Verdict 4 états d'éligibilité RCC longue carrière.
 *
 * <p>Hiérarchie de priorité (côté backend Validator) :
 * démission &gt; âge &gt; carrière &gt; éligible.</p>
 */
export type RccBeLongueCarriereVerdict =
  | 'ELIGIBLE_RCC_LONGUE_CARRIERE'
  | 'INELIGIBLE_AGE_INSUFFISANT'
  | 'INELIGIBLE_CARRIERE_INSUFFISANTE'
  | 'INELIGIBLE_DEMISSION';

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/rcc-be-longue-carriere`.
 *
 * <p>Champs obligatoires : {@code ageFinContrat}, {@code anneesCarriereTotale},
 * {@code dateFinContrat}, {@code licenciementEffectif}. Couple
 * financier optionnel ({@code remunerationNetteMensuelleReference}
 * + {@code allocationChomageMensuelleEstimee}) — pilote le calcul de
 * l'indemnité complémentaire indicative si éligible.</p>
 */
export interface RccBeLongueCarriereRequest {
  /** Obligatoire — âge en années à la date prévue de fin de contrat, ≥ 0. */
  ageFinContrat: number;
  /** Obligatoire — carrière professionnelle totale en années (ETP), ≥ 0. */
  anneesCarriereTotale: number;
  /** Obligatoire — ISO yyyy-MM-dd. */
  dateFinContrat: string;
  /** Obligatoire — true si licenciement employeur (false = démission, exclue). */
  licenciementEffectif: boolean;
  /** Optionnel — rémunération nette mensuelle de référence (€). */
  remunerationNetteMensuelleReference: number | null;
  /** Optionnel — estimation ONEM de l'allocation mensuelle de chômage (€). */
  allocationChomageMensuelleEstimee: number | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier.
 */
export interface RccBeLongueCarriereResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;

  // -- Echo inputs --
  ageFinContrat: number;
  anneesCarriereTotale: number;
  dateFinContrat: string;
  licenciementEffectif: boolean;
  remunerationNetteMensuelleReference: number | null;
  allocationChomageMensuelleEstimee: number | null;

  // -- Verdict & conditions --
  /** Verdict 4 états — ELIGIBLE (vert) ou 3 motifs d'inéligibilité (rouge). */
  verdict: RccBeLongueCarriereVerdict;
  /** Convenience flag — true si verdict = ELIGIBLE_RCC_LONGUE_CARRIERE. */
  eligible: boolean;
  /** Âge fin contrat ≥ 59 ans. */
  conditionAgeRemplie: boolean;
  /** Carrière ≥ 40 ans. */
  conditionCarriereRemplie: boolean;
  /** Licenciement effectif (pas démission). */
  conditionLicenciementRemplie: boolean;

  // -- Indemnité complémentaire indicative --
  /**
   * 0,5 × (remunerationNetteMensuelleReference - allocationChomageMensuelleEstimee).
   * Non nul uniquement si éligible et si les 2 montants financiers sont fournis.
   * Plancher à 0 si la différence est négative.
   */
  indemniteComplementaireMensuelle: number | null;

  // -- Synthèse & métadonnées légales --
  /** Synthèse en langage avocat. */
  synthese: string;
  /** Base juridique humanisée (Loi 26/12/2013 + CCT 17 + AR 03/05/2007). */
  baseJuridique: string;
  /** Avertissement éventuel (ex : indemnité indicative à confirmer ONSS/ONEM). */
  avertissement: string | null;
}
