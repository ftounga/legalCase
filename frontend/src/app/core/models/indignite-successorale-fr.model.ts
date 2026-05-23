/**
 * SF-216-20 : modèles TypeScript de l'outil "Indignité successorale"
 * (F-FA-INDIGNITE-SUCCESSORALE, FRANCE uniquement — art. 726-729-1 Cciv +
 * Loi n°2001-1135 du 3/12/2001 + Loi n°2022-1617 du 23/12/2022).
 *
 * Contrat API importé de SF-216-19 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/indignite-successorale
 *   GET  /api/v1/case-files/{caseFileId}/indignite-successorale
 */

/** Motif d'indignité envisagé (art. 726-727 Cciv). */
export type MotifIndignite =
  | 'MEURTRE_TENTE'
  | 'MEURTRE'
  | 'TEMOIGNAGE_FAUX'
  | 'OBSTRUCTION_TESTAMENT'
  | 'NON_SIGNALEMENT_HOMICIDE'
  | 'VIOLENCES_GRAVES'
  | 'AUTRE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/indignite-successorale`.
 *
 * 6 champs (cf. backend `IndigniteSuccessoraleRequest`).
 */
export interface IndigniteSuccessoraleRequest {
  motifIndignite: MotifIndignite | null;
  condamnationPrononcee: boolean | null;
  indigniteJudiciaireDemandee: boolean | null;
  pardonTestamentaireDetecte: boolean | null;
  /** ISO date YYYY-MM-DD. */
  dateOuvertureSuccession: string | null;
  nbCoheritiersRestants: number | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface IndigniteSuccessoraleResponse {
  caseFileId: string;
  /** "PLEIN_DROIT" | "JUDICIAIRE" | "PARDONNEE" | "AUCUNE". */
  typeIndignite: string;
  /**
   * "INDIGNITE_PLEIN_DROIT" | "INDIGNITE_JUDICIAIRE_POSSIBLE" |
   * "PARDON_NEUTRALISANT" | "DELAI_FORCLOS" | "CONDAMNATION_REQUISE" |
   * "PAS_D_INDIGNITE".
   */
  verdictIndignite: string;
  pardonNeutralisant: boolean;
  representationPossible: boolean;
  delaiAction: string;
  delaiForclos: boolean;
  effetDevolution: string;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
