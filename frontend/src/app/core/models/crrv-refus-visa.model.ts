/**
 * SF-208-07 : modeles miroirs du contrat API SF-208-03 (backend, PR #915)
 * pour l'outil decisionnel "CRRV recours refus de visa 2 mois" (F-IM-23).
 * FR uniquement (CESEDA L.312-1 a L.312-3 + D.312-3).
 */

export type TypeVisaCrrv =
  | 'COURT_SEJOUR'
  | 'LONG_SEJOUR'
  | 'REGROUPEMENT_FAMILIAL'
  | 'ETUDIANT'
  | 'AUTRE';

export type StatutCrrv =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface CrrvRefusVisaRequest {
  dateNotificationRefus: string; // YYYY-MM-DD
  typeVisa: TypeVisaCrrv;
  motifRefus: string | null;
  recoursForme: boolean;
  dateRecours?: string | null;
}

export interface CrrvRefusVisaResponse {
  caseFileId: string;
  dateNotificationRefus: string;
  typeVisa: TypeVisaCrrv;
  motifRefus: string | null;
  recoursForme: boolean;
  dateRecours: string | null;
  country: 'FRANCE';
  dateExpirationRecoursCrrv: string;
  joursRestants: number;
  statut: StatutCrrv;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface TypeVisaCrrvOption {
  code: TypeVisaCrrv;
  label: string;
}

export const TYPES_VISA_CRRV: TypeVisaCrrvOption[] = [
  { code: 'COURT_SEJOUR', label: 'Court sejour (Schengen)' },
  { code: 'LONG_SEJOUR', label: 'Long sejour (>= 3 mois)' },
  { code: 'REGROUPEMENT_FAMILIAL', label: 'Regroupement familial' },
  { code: 'ETUDIANT', label: 'Etudiant' },
  { code: 'AUTRE', label: 'Autre visa' },
];
