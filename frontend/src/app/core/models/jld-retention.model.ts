/**
 * SF-208-05 : modèles miroirs du contrat API SF-208-01 (backend, PR #915)
 * pour l'outil decisionnel "JLD retention administrative" (F-IM-21).
 * FR uniquement (CESEDA L.741+, L.743+).
 */

export type MotifPlacementJld =
  | 'EXECUTION_OQTF'
  | 'ITF'
  | 'INTERDICTION_TERRITOIRE'
  | 'DUBLIN_TRANSFERT'
  | 'AUTRE';

export type StatutJld =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface JldRetentionRequest {
  dateNotificationPlacement: string; // YYYY-MM-DD
  motifPlacement: MotifPlacementJld;
  recoursForme: boolean;
  dateRecours?: string | null;
}

export interface JldRetentionResponse {
  caseFileId: string;
  dateNotificationPlacement: string;
  motifPlacement: MotifPlacementJld;
  recoursForme: boolean;
  dateRecours: string | null;
  country: 'FRANCE';
  dateExpirationSaisineJld: string;
  dateAudienceJld: string;
  dateExpirationRecoursAppel: string | null;
  joursRestantsAvantSaisine: number;
  statut: StatutJld;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifPlacementJldOption {
  code: MotifPlacementJld;
  label: string;
}

export const MOTIFS_PLACEMENT_JLD: MotifPlacementJldOption[] = [
  { code: 'EXECUTION_OQTF', label: "Execution d'une OQTF" },
  { code: 'ITF', label: 'Interdiction du territoire francais (ITF)' },
  { code: 'INTERDICTION_TERRITOIRE', label: 'Interdiction administrative du territoire' },
  { code: 'DUBLIN_TRANSFERT', label: 'Transfert Dublin' },
  { code: 'AUTRE', label: 'Autre motif' },
];
