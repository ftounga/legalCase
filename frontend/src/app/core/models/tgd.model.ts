/**
 * SF-222-02 : modèles TypeScript de l'outil "Téléphone Grave Danger —
 * éligibilité" (F-FA-TGD, FRANCE uniquement — art. 41-3-1 CPP).
 *
 * Contrat API figé côté backend SF-222-02 (POST/GET) :
 *   POST /api/v1/case-files/{caseFileId}/tgd-analysis
 *   GET  /api/v1/case-files/{caseFileId}/tgd-analysis
 *
 * L'outil CONSEILLE l'avocat sur l'éligibilité ; l'ATTRIBUTION du TGD relève du
 * procureur de la République.
 *
 * Anti-doublon F-FA-14 (ordonnance de protection) : analyzer d'éligibilité au
 * dispositif TGD, distinct de la MESURE TGD recommandée par l'ordonnance de
 * protection.
 */

/** Verdict 3 niveaux de l'analyse d'éligibilité TGD. */
export type VerdictTgd =
  | 'ELIGIBLE_TGD'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE';

/** Requête POST `/api/v1/case-files/{caseFileId}/tgd-analysis` (5 critères). */
export interface TgdRequest {
  dangerGrave: boolean | null;
  violencesAvereesOuVraisemblables: boolean | null;
  interdictionContactProcedure: boolean | null;
  nonCohabitation: boolean | null;
  consentementVictime: boolean | null;
}

/** Réponse POST / GET — résultat persisté pour le dossier. */
export interface TgdResponse {
  caseFileId: string;
  verdict: VerdictTgd;
  criteresManquants: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
}
