/**
 * SF-219-03b : modèle frontend de l'outil F-219 — RCC BE
 * <b>entreprise en difficulté / restructuration</b>.
 *
 * <p>BE uniquement — <b>Loi du 26/12/2013</b> (pacte de solidarité entre
 * les générations) + <b>CCT n° 17 du 19/12/1974</b> (CNT — RCC) +
 * <b>AR du 03/05/2007</b> (régime RCC général) + <b>AR de reconnaissance
 * ministérielle</b> de l'entreprise en difficulté ou en restructuration.
 * Aucun équivalent strict en droit français.</p>
 *
 * <p>Contrat figé dans SF-219-03 backend
 * ({@code RccBeEntrepriseDifficulteResponse} +
 * {@code RccBeEntrepriseDifficulteRequest}).</p>
 *
 * <p>Singularité du régime : la <b>reconnaissance ministérielle préalable</b>
 * de l'entreprise (en difficulté ou en restructuration) est la pierre
 * angulaire — sans cette reconnaissance, aucune dérogation d'âge n'est
 * ouverte, indépendamment de l'âge ou de la carrière du travailleur.</p>
 *
 * <p>Verdict 6 états — dont 1 éligible et 5 motifs d'inéligibilité,
 * hiérarchisés côté backend (démission &gt; reconnaissance absente &gt;
 * âge &gt; carrière &gt; ancienneté &gt; éligible).</p>
 */

/**
 * Type de reconnaissance ministérielle de l'entreprise.
 *
 * <ul>
 *   <li>{@code EN_DIFFICULTE} — entreprise reconnue en difficulté par
 *       arrêté ministériel (perte sur exploitation, capitaux propres
 *       négatifs, etc.) — âge réduit fixé par le plan agréé.</li>
 *   <li>{@code EN_RESTRUCTURATION} — entreprise reconnue en
 *       restructuration (licenciement collectif d'ampleur, plan de
 *       restructuration approuvé) — âge réduit également fixé par le
 *       plan agréé.</li>
 *   <li>{@code NON_RECONNUE} — pas de reconnaissance ministérielle :
 *       court-circuit verdict {@code INELIGIBLE_RECONNAISSANCE_ABSENTE}.</li>
 * </ul>
 */
export type RccBeEntrepriseDifficulteType =
  | 'EN_DIFFICULTE'
  | 'EN_RESTRUCTURATION'
  | 'NON_RECONNUE';

/**
 * Verdict 6 états d'éligibilité RCC entreprise en difficulté.
 *
 * <p>Hiérarchie de priorité (côté backend Validator) :
 * démission &gt; reconnaissance absente &gt; âge &gt; carrière &gt;
 * ancienneté &gt; éligible.</p>
 */
export type RccBeEntrepriseDifficulteVerdict =
  | 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE'
  | 'INELIGIBLE_DEMISSION'
  | 'INELIGIBLE_RECONNAISSANCE_ABSENTE'
  | 'INELIGIBLE_AGE_INSUFFISANT'
  | 'INELIGIBLE_CARRIERE_INSUFFISANTE'
  | 'INELIGIBLE_ANCIENNETE_INSUFFISANTE';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-entreprise-difficulte}.
 *
 * <p>Champs obligatoires : {@code typeReconnaissance}, {@code ageReduitPlan},
 * {@code ageFinContrat}, {@code anneesCarriereTotale},
 * {@code anneesAncienneteSecteur}, {@code dateFinContrat},
 * {@code licenciementEffectif}.
 * Couple financier optionnel
 * ({@code remunerationNetteMensuelleReference} +
 * {@code allocationChomageMensuelleEstimee}) — pilote le calcul de
 * l'indemnité complémentaire indicative si éligible.</p>
 */
export interface RccBeEntrepriseDifficulteRequest {
  /** Obligatoire — type de reconnaissance ministérielle. */
  typeReconnaissance: RccBeEntrepriseDifficulteType;
  /**
   * Obligatoire — âge minimum fixé par le plan ministériel (souvent 55
   * ans). Si {@code typeReconnaissance = NON_RECONNUE}, ce champ peut
   * porter une valeur arbitraire (court-circuit verdict).
   */
  ageReduitPlan: number;
  /** Obligatoire — âge en années à la date prévue de fin de contrat, ≥ 0. */
  ageFinContrat: number;
  /** Obligatoire — carrière professionnelle totale en années (ETP), ≥ 0. */
  anneesCarriereTotale: number;
  /** Obligatoire — ancienneté dans le secteur / entreprise reconnue, ≥ 0. */
  anneesAncienneteSecteur: number;
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
export interface RccBeEntrepriseDifficulteResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;

  // -- Echo inputs --
  typeReconnaissance: RccBeEntrepriseDifficulteType;
  ageReduitPlan: number;
  ageFinContrat: number;
  anneesCarriereTotale: number;
  anneesAncienneteSecteur: number;
  dateFinContrat: string;
  licenciementEffectif: boolean;
  remunerationNetteMensuelleReference: number | null;
  allocationChomageMensuelleEstimee: number | null;

  // -- Verdict & conditions --
  /** Verdict 6 états — ELIGIBLE (vert) ou 5 motifs d'inéligibilité (rouge). */
  verdict: RccBeEntrepriseDifficulteVerdict;
  /** Convenience flag — true si verdict = ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE. */
  eligible: boolean;
  /** Reconnaissance ministérielle = EN_DIFFICULTE ou EN_RESTRUCTURATION. */
  conditionReconnaissanceRemplie: boolean;
  /** Âge fin contrat ≥ âge réduit du plan. */
  conditionAgeRemplie: boolean;
  /** Carrière ≥ 10 ans (jours ETP). */
  conditionCarriereRemplie: boolean;
  /** Ancienneté secteur / entreprise reconnue ≥ 5 ans. */
  conditionAncienneteRemplie: boolean;
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
  /** Base juridique humanisée (Loi 26/12/2013 + CCT 17 + AR 03/05/2007 + AR reconnaissance). */
  baseJuridique: string;
  /** Avertissement éventuel (ex : indemnité indicative à confirmer ONSS/ONEM). */
  avertissement: string | null;
}
