/**
 * SF-216-18 : modèles TypeScript de l'outil "Adoption internationale"
 * (F-FA-ADOPTION-INTERNATIONALE, FRANCE uniquement — art. 370-3 à 370-5
 * Cciv + Convention La Haye du 29/5/1993 + Loi n°2001-111 du 6/2/2001
 * (agrément) + Loi n°2022-219 du 21/2/2022 (réforme adoption)).
 *
 * Contrat API importé de SF-216-17 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/adoption-internationale
 *   GET  /api/v1/case-files/{caseFileId}/adoption-internationale
 */

/** Forme d'adoption demandée ou possible (réutilise les valeurs FR). */
export type FormeAdoption = 'PLENIERE' | 'SIMPLE';

/** Voie procédurale d'une adoption internationale. */
export type VoieProcedureAdoption = 'OAA_AGREE' | 'VOIE_AUTONOME' | 'ORGANISME_ETAT';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/adoption-internationale`.
 *
 * 9 champs (cf. backend `AdoptionInternationaleRequest`).
 */
export interface AdoptionInternationaleRequest {
  paysOrigineEnfant: string | null;
  conventionLaHayeApplicable: boolean | null;
  agrement2025: boolean | null;
  ageAdoptant: number | null;
  ageAdopte: number | null;
  adoptantMarie: boolean | null;
  voieProcedure: VoieProcedureAdoption | null;
  formeAdoptionDemandee: FormeAdoption | null;
  exequaturRequis: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface AdoptionInternationaleResponse {
  caseFileId: string;
  conditionsRemplies: boolean;
  voieProcedure: VoieProcedureAdoption;
  conventionApplicable: boolean;
  alerteKafala: boolean;
  exequaturRequis: boolean;
  delaiEstime: string;
  verdict: string;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
