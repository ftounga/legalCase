/**
 * SF-DT-21-02 : contrat API de l'outil décisionnel "Indemnité forfaitaire
 * pour travail dissimulé" (art. L.8223-1 Code du travail). FR uniquement —
 * pas d'équivalent législatif en droit belge.
 *
 * Consomme l'API figée dans SF-DT-21-01 (backend) :
 *   - POST  /api/v1/case-files/{caseFileId}/travail-dissimule
 *   - GET   /api/v1/case-files/{caseFileId}/travail-dissimule
 */

export interface TravailDissimuleRequest {
  salaireMensuelReference: number;
}

export interface TravailDissimuleResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  indemniteForfaitaire: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
