export type MotifOqtf =
  | 'REFUS_TITRE'
  | 'EXPIRATION_TITRE'
  | 'SEJOUR_IRREGULIER'
  | 'RETRAIT_TITRE'
  | 'AUTRE';

export type StatutDelai =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface OqtfAvecDelaiRequest {
  dateNotificationOqtf: string; // YYYY-MM-DD
  motifOqtf: MotifOqtf;
  recoursForme: boolean;
  dateRecours?: string | null;
}

export interface OqtfAvecDelaiResponse {
  caseFileId: string;
  dateNotificationOqtf: string;
  motifOqtf: MotifOqtf;
  recoursForme: boolean;
  dateRecours: string | null;
  country: 'FRANCE';
  dateExpirationDdv: string;
  dateExpirationDelaiRecours: string;
  joursRestantsAvantExpirationDelai: number;
  statutDelaiRecours: StatutDelai;
  dateAudiencePrevisionnelle: string | null;
  dateDecisionTaPrevisionnelle: string | null;
  referedDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifOqtfOption {
  code: MotifOqtf;
  label: string;
}

export const MOTIFS_OQTF: MotifOqtfOption[] = [
  { code: 'REFUS_TITRE', label: 'Refus de titre' },
  { code: 'EXPIRATION_TITRE', label: 'Expiration de titre' },
  { code: 'SEJOUR_IRREGULIER', label: 'Séjour irrégulier' },
  { code: 'RETRAIT_TITRE', label: 'Retrait de titre' },
  { code: 'AUTRE', label: 'Autre' },
];
