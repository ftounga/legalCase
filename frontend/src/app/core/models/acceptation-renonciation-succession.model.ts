/**
 * F-210 SF-210-04 : modèles TypeScript pour l'outil "Acceptation /
 * renonciation à succession" (Cciv art. 768+, 771-772, 783, 791).
 * Backend : SF-210-03.
 */

export type QualiteHeritier = 'PREMIER_RANG' | 'SECOND_RANG';

export type SuccessionOption =
  | 'ACCEPTATION_PURE_SIMPLE'
  | 'ACCEPTATION_CONCURRENCE_ACTIF'
  | 'RENONCIATION';

export type IntentionExprimee = SuccessionOption | 'INCERTAIN';

export interface AcceptationRenonciationSuccessionRequest {
  dateOuvertureSuccession: string; // ISO YYYY-MM-DD
  qualiteHeritier: QualiteHeritier;
  actifBrutEur: number;
  passifEur: number;
  actesEquivalentAcceptationDejaPosesDetected: boolean;
  inventaireRealise: boolean;
  dettesIncertainesDetected: boolean;
  intentionExprimee: IntentionExprimee;
}

export interface AcceptationRenonciationSuccessionResponse {
  caseFileId: string;
  dateOuvertureSuccession: string;
  qualiteHeritier: QualiteHeritier;
  actifBrutEur: number;
  passifEur: number;
  actesEquivalentAcceptationDejaPosesDetected: boolean;
  inventaireRealise: boolean;
  dettesIncertainesDetected: boolean;
  intentionExprimee: IntentionExprimee | string;
  optionsOuvertes: SuccessionOption[];
  optionRecommandee: SuccessionOption;
  delaiRestantJours: number;
  delaiTotalJours: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
