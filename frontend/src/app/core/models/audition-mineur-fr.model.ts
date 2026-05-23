/**
 * SF-216-14 : modèles TypeScript de l'outil "Audition du mineur par le JAF"
 * (F-FA-AUDITION-MINEUR, FRANCE uniquement — art. 388-1 Cciv + art. 1074-1
 * à 1074-3 CPC + CIDE art. 12 + Cass. 1ère civ., 18/3/2015, n°14-11.392).
 *
 * Contrat API importé de SF-216-13 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/audition-mineur
 *   GET  /api/v1/case-files/{caseFileId}/audition-mineur
 */

/** Capacité de discernement appréciée par le juge (art. 388-1 Cciv). */
export type CapaciteDiscernement = 'CERTAINE' | 'PROBABLE' | 'DOUTEUSE' | 'INCONNUE';

/** Type de procédure civile dans laquelle l'audition est sollicitée. */
export type ProcedureAudition =
  | 'DIVORCE'
  | 'AUTORITE_PARENTALE'
  | 'GARDE'
  | 'SUCCESSION'
  | 'AUTRE';

/** Modalité de l'audition (art. 388-1 al. 3 Cciv + art. 1074-2 CPC). */
export type ModaliteAudition = 'SEUL' | 'AVEC_AVOCAT' | 'AVEC_TIERS';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/audition-mineur`.
 *
 * 7 champs (cf. backend `AuditionMineurRequest`).
 */
export interface AuditionMineurRequest {
  ageEnfant: number | null;
  capaciteDiscernement: CapaciteDiscernement | null;
  demandeFormalisee: boolean | null;
  demandeParEnfantLuiMeme: boolean | null;
  refusMotive: boolean | null;
  motivationRefus: string | null;
  procedureEnCours: ProcedureAudition | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface AuditionMineurResponse {
  caseFileId: string;
  conditionsRemplies: boolean;
  droitAuditionReconnu: boolean;
  modaliteRecommandee: ModaliteAudition;
  refusContestable: boolean;
  verdict: string;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
