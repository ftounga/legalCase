export type MotifOqt =
  | 'SEJOUR_IRREGULIER_ART_7'
  | 'REFUS_SEJOUR_APRES_DEMANDE'
  | 'FIN_SEJOUR_REGULIER'
  | 'AUTRE';

export type TypeRecours = 'ANNULATION_30J' | 'EXTREME_URGENCE_5JO';

export type StatutRecoursAnnul =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface Annexe13BeRequest {
  dateNotificationAnnexe13: string; // YYYY-MM-DD
  delaiDepartImposeJours: number;
  motifOqt: MotifOqt;
  transfertImminent: boolean;
  recoursForme: boolean;
  dateRecours?: string | null;
  typeRecours?: TypeRecours | null;
}

export interface Annexe13BeResponse {
  caseFileId: string;
  dateNotificationAnnexe13: string;
  delaiDepartImposeJours: number;
  motifOqt: MotifOqt;
  transfertImminent: boolean;
  recoursForme: boolean;
  dateRecours: string | null;
  typeRecours: TypeRecours | null;
  country: 'BELGIQUE';
  dateExpirationDelaiDepart: string;
  dateExpirationRecoursAnnulation: string;
  dateExpirationRecoursExtremeUrgence: string;
  joursRestantsAvantExpirationAnnulation: number;
  statutRecoursAnnulation: StatutRecoursAnnul;
  dateAudiencePrevisionnelle: string | null;
  dateDecisionPrevisionnelle: string | null;
  referedDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifOqtOption {
  code: MotifOqt;
  label: string;
}

export const MOTIFS_OQT: MotifOqtOption[] = [
  { code: 'SEJOUR_IRREGULIER_ART_7', label: 'Séjour irrégulier art. 7' },
  { code: 'REFUS_SEJOUR_APRES_DEMANDE', label: 'Refus de séjour après demande' },
  { code: 'FIN_SEJOUR_REGULIER', label: 'Fin de séjour régulier' },
  { code: 'AUTRE', label: 'Autre' },
];

export interface TypeRecoursOption {
  code: TypeRecours;
  label: string;
}

export const TYPES_RECOURS: TypeRecoursOption[] = [
  { code: 'ANNULATION_30J', label: 'Annulation 30 jours' },
  { code: 'EXTREME_URGENCE_5JO', label: 'Extrême urgence 5 jours ouvrables' },
];
