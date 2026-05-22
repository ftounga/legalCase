/**
 * SF-216-16 : modèles TypeScript de l'outil "Adoption de l'enfant du conjoint
 * (adoption intra-familiale)" (F-FA-ADOPTION-INTRA, FRANCE uniquement —
 * art. 343-1 al. 2 Cciv + art. 345-1 Cciv + art. 360-362 Cciv +
 * Loi n°2022-219 du 21/2/2022).
 *
 * Contrat API importé de SF-216-15 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/adoption-intra-fr
 *   GET  /api/v1/case-files/{caseFileId}/adoption-intra-fr
 */

/** Forme d'adoption demandée ou possible en intra-familial. */
export type FormeAdoption = 'PLENIERE' | 'SIMPLE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/adoption-intra-fr`.
 *
 * 8 champs (cf. backend `AdoptionIntraFrRequest`).
 */
export interface AdoptionIntraFrRequest {
  formeAdoptionDemandee: FormeAdoption | null;
  ageAdoptant: number | null;
  ageAdopte: number | null;
  mariageOuPacsAdoptantParent: boolean | null;
  consentementEnfant: boolean | null;
  consentementAutreParentBiologique: boolean | null;
  decheanceAutreParent: boolean | null;
  autreParentDecede: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface AdoptionIntraFrResponse {
  caseFileId: string;
  formesPossibles: FormeAdoption[];
  formeDemandee: FormeAdoption;
  conditionsRemplies: boolean;
  alerteIrreversibilite: boolean;
  consentementEnfantRequis: boolean;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
