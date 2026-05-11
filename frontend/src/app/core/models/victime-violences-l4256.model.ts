/**
 * SF-208-08 : modeles miroirs du contrat API SF-208-04 (backend, PR #915)
 * pour l'outil decisionnel "Victime de violences L.425-6" (F-IM-24).
 * FR uniquement (CESEDA L.425-6 + Cciv 515-9 a 515-13).
 */

export type EligibiliteScoreL4256 =
  | 'ELIGIBLE_PLEIN_DROIT'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE';

export interface VictimeViolencesL4256Request {
  dateOrdonnanceProtection: string; // YYYY-MM-DD
  juridiction: string;
  dureeProtectionMois: number;
  dateExpirationProtection?: string | null;
  enfantsAcharge: number;
  nationalite?: string | null;
}

export interface VictimeViolencesL4256Response {
  caseFileId: string;
  dateOrdonnanceProtection: string;
  juridiction: string;
  dureeProtectionMois: number;
  dateExpirationProtectionEffective: string;
  enfantsAcharge: number;
  nationalite: string | null;
  country: 'FRANCE';
  eligibiliteScore: EligibiliteScoreL4256;
  criteresValides: string[];
  criteresManquants: string[];
  dureeTitreSejourMois: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
