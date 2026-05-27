/**
 * SF-219-01b : modèle frontend de l'outil F-219 — RCC métiers lourds BE
 * (CCT 17 + AR 03/05/2007 art. 3).
 *
 * <p>BE uniquement — pas d'équivalent FR direct (la pré-retraite a été
 * supprimée en France en 2010 ; dispositifs spécifiques amiante / C2P
 * pénibilité sont distincts).</p>
 *
 * <p>Contrat figé dans SF-219-01 backend ({@code RccBeMetiersLourdsResponse}
 * + {@code RccBeMetiersLourdsRequest}).</p>
 *
 * <p>Verdict binaire :
 * <ul>
 *   <li>{@code ELIGIBLE} (vert) — toutes les conditions cumulatives sont
 *       remplies (licenciement effectif ; âge ≥ 58 ; carrière ≥ 35 ;
 *       5/10 OU 7/15 en métier lourd). L'indemnité complémentaire est
 *       calculée séparément par {@code rcc-be-indemnite-complementaire}
 *       (F-207 SF-207-07).</li>
 *   <li>{@code INELIGIBLE} (rouge) — au moins une condition manque. La
 *       raison précise est portée par {@link RccBeMetiersLourdsRaisonIneligibilite}.</li>
 * </ul></p>
 */

/**
 * Type de métier lourd ouvrant droit au RCC métiers lourds (régime de
 * chômage avec complément d'entreprise, ex-prépension BE).
 *
 * <ul>
 *   <li>{@code EQUIPES_SUCCESSIVES_NUIT} — travail en équipes successives
 *       comportant des prestations de nuit.</li>
 *   <li>{@code INTERRUPTIONS_HORAIRES_VARIABLES} — travail en interruptions
 *       à horaires variables (jour-nuit alternés).</li>
 *   <li>{@code TRAVAIL_PENIBLE} — manutention manuelle de charges,
 *       construction, mines, métiers réputés physiquement éprouvants par
 *       les annexes AR.</li>
 *   <li>{@code AUTRE} — autre métier lourd non listé explicitement, à
 *       justifier par l'avocat en référence aux arrêtés ministériels.</li>
 * </ul>
 */
export type RccBeMetiersLourdsType =
  | 'EQUIPES_SUCCESSIVES_NUIT'
  | 'INTERRUPTIONS_HORAIRES_VARIABLES'
  | 'TRAVAIL_PENIBLE'
  | 'AUTRE';

/** Verdict binaire — voir doc en-tête. */
export type RccBeMetiersLourdsVerdict =
  | 'ELIGIBLE'
  | 'INELIGIBLE';

/**
 * Raison précise d'inéligibilité (ordre stable côté backend pour message
 * exploitable côté UI).
 *
 * <ol>
 *   <li>{@code DEMISSION_NON_ELIGIBLE} — l'employeur n'a pas licencié.</li>
 *   <li>{@code AGE_INSUFFISANT} — âge &lt; 58 à la fin du contrat.</li>
 *   <li>{@code CARRIERE_INSUFFISANTE} — carrière totale &lt; 35 ans.</li>
 *   <li>{@code DUREE_METIER_LOURD_INSUFFISANTE} — ni 5/10 ni 7/15.</li>
 * </ol>
 */
export type RccBeMetiersLourdsRaisonIneligibilite =
  | 'DEMISSION_NON_ELIGIBLE'
  | 'AGE_INSUFFISANT'
  | 'CARRIERE_INSUFFISANTE'
  | 'DUREE_METIER_LOURD_INSUFFISANTE';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds}.
 *
 * <p>Tous les champs sont requis côté backend (annotations
 * {@code @NotNull / @Min / @Max}). Plages :</p>
 * <ul>
 *   <li>{@code ageFinContrat} : [18, 80]</li>
 *   <li>{@code anneesCarriereTotal} : [0, 60]</li>
 *   <li>{@code anneesMetierLourdRecent10} : [0, 10]</li>
 *   <li>{@code anneesMetierLourdRecent15} : [0, 15]</li>
 * </ul>
 */
export interface RccBeMetiersLourdsRequest {
  /** Âge à la fin du contrat (années). */
  ageFinContrat: number;
  /** Nombre total d'années de carrière professionnelle. */
  anneesCarriereTotal: number;
  /** Années passées en métier lourd dans les 10 dernières années (≥ 5 ⇒ OK). */
  anneesMetierLourdRecent10: number;
  /** Années passées en métier lourd dans les 15 dernières années (≥ 7 ⇒ OK). */
  anneesMetierLourdRecent15: number;
  /** Type de métier lourd. */
  typeMetierLourd: RccBeMetiersLourdsType;
  /** True = licenciement (éligible), false = démission (inéligible). */
  licenciementEffectif: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface RccBeMetiersLourdsResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  ageFinContrat: number;
  anneesCarriereTotal: number;
  anneesMetierLourdRecent10: number;
  anneesMetierLourdRecent15: number;
  typeMetierLourd: RccBeMetiersLourdsType;
  licenciementEffectif: boolean;

  // -- Outputs métier --
  /** Verdict binaire. */
  verdict: RccBeMetiersLourdsVerdict;
  /** Raison précise d'inéligibilité (null si éligible). */
  raisonIneligibilite: RccBeMetiersLourdsRaisonIneligibilite | null;
  /** Synthèse humaine du verdict + justification (1-2 phrases). */
  synthese: string;
  /** Base juridique courte (CCT 17 + AR 03/05/2007 art. 3). */
  baseJuridique: string;
  /** Avertissement éventuel (liste métiers lourds, annexes AR à vérifier…). */
  avertissement: string;
}
