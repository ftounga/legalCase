export type MotifSansDelai =
  | 'RISQUE_FUITE'
  | 'TROUBLE_ORDRE_PUBLIC'
  | 'OQTF_PRECEDENTE_INEXECUTEE'
  | 'AUTRE';

export type StatutDelaiSd =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface OqtfSansDelaiRequest {
  dateHeureNotificationOqtf: string; // ISO datetime
  motifSansDelai: MotifSansDelai;
  placementCra: boolean;
  recoursForme: boolean;
  dateHeureRecours?: string | null;
}

export interface OqtfSansDelaiResponse {
  caseFileId: string;
  dateHeureNotificationOqtf: string;
  motifSansDelai: MotifSansDelai;
  placementCra: boolean;
  recoursForme: boolean;
  dateHeureRecours: string | null;
  country: 'FRANCE';
  dateHeureExpirationDelaiRecours: string;
  heuresRestantes: number;
  statutDelaiRecours: StatutDelaiSd;
  dateHeureAudiencePrevisionnelle: string | null;
  dateDecisionPrevisionnelle: string | null;
  refereDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifSansDelaiOption {
  code: MotifSansDelai;
  label: string;
}

export const MOTIFS_SANS_DELAI: MotifSansDelaiOption[] = [
  { code: 'RISQUE_FUITE', label: 'Risque de fuite' },
  { code: 'TROUBLE_ORDRE_PUBLIC', label: 'Trouble à l’ordre public' },
  { code: 'OQTF_PRECEDENTE_INEXECUTEE', label: 'OQTF précédente inexécutée' },
  { code: 'AUTRE', label: 'Autre' },
];
