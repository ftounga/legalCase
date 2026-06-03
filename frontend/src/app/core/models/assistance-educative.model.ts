/**
 * SF-222-04 : modèles TypeScript de l'outil "Assistance éducative" — mineur en
 * danger (F-FA-ASSISTANCE-EDUCATIVE, FRANCE uniquement — art. 375 et s. Cciv).
 *
 * Contrat API figé côté backend SF-222-04 (POST/GET) :
 *   POST /api/v1/case-files/{caseFileId}/assistance-educative-analysis
 *   GET  /api/v1/case-files/{caseFileId}/assistance-educative-analysis
 *
 * Invariant « 1 situation = 1 outil » : UN SEUL outil oriente vers les 4 issues
 * d'UNE situation (mineur en danger) : AED (administrative ASE), AEMO (judiciaire,
 * milieu ouvert), OPP / placement (judiciaire, retrait) ou pas de mesure.
 *
 * L'outil CONSEILLE l'avocat sur le danger (art. 375 Cciv) ; la mesure judiciaire
 * est ordonnée par le juge des enfants, la mesure administrative par l'ASE.
 */

/** Verdict 4 niveaux de l'orientation assistance éducative. */
export type VerdictAssistanceEducative =
  | 'AED'
  | 'AEMO'
  | 'OPP_PLACEMENT'
  | 'PAS_DE_MESURE';

/** Requête POST `/api/v1/case-files/{caseFileId}/assistance-educative-analysis`. */
export interface AssistanceEducativeRequest {
  dangerCaracterise: boolean | null;
  urgence: boolean | null;
  adhesionFamille: boolean | null;
  maintienMilieuFamilialPossible: boolean | null;
  mesureAmiableASEEnvisageable: boolean | null;
}

/** Réponse POST / GET — résultat persisté pour le dossier. */
export interface AssistanceEducativeResponse {
  caseFileId: string;
  verdict: VerdictAssistanceEducative;
  juridiction: string;
  mesureOrientee: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
}
