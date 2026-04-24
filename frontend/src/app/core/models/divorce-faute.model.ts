/**
 * SF-FA-09-02 : modèle frontend pour l'outil décisionnel F-FA-09
 * "Divorce pour faute" (FR uniquement, art. 242 Cciv).
 * Contrat importé du backend SF-FA-09-01 (PR #515).
 */

export type FauteCode =
  | 'ADULTERE'
  | 'VIOLENCES'
  | 'ABANDON'
  | 'OUTRAGES'
  | 'DEVOIR_ASSISTANCE'
  | 'DEVOIR_FIDELITE'
  | 'DEVOIR_COMMUNAUTE_VIE'
  | 'AUTRE';

export type VerdictProbabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
export type VerdictTorts = 'EXCLUSIF_DEFENDEUR' | 'PARTAGES' | 'IMPREDICTIBLE';

export interface DivorceFauteRequest {
  fautesInvoquees: FauteCode[];
  preuvesDocumentaires: boolean;
  tortsAdverseInvoques: boolean;
  dureeMariageAnnees: number;
  revenusAnnuelsDemandeurEur: number;
  revenusAnnuelsDefendeurEur: number;
  dateDepotAssignation?: string | null;
}

export interface DivorceFauteResponse {
  caseFileId: string;
  fautesInvoquees: FauteCode[];
  preuvesDocumentaires: boolean;
  tortsAdverseInvoques: boolean;
  dureeMariageAnnees: number;
  revenusAnnuelsDemandeurEur: number;
  revenusAnnuelsDefendeurEur: number;
  dateDepotAssignation: string | null;
  country: 'FRANCE';
  nombreFautesInvoquees: number;
  solidariteeFautesOk: boolean;
  risqueTortsPartages: boolean;
  scoreGlobal: number;
  verdictProbabiliteDivorceFaute: VerdictProbabilite;
  verdictTortsEstimes: VerdictTorts;
  damagesInteretsArt266FourchetteMin: number;
  damagesInteretsArt266FourchetteMax: number;
  prestationCompensatoireFourchetteMin: number;
  prestationCompensatoireFourchetteMax: number;
  criteresNonRemplis: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface FauteOption {
  code: FauteCode;
  label: string;
}

export const FAUTES_FR: FauteOption[] = [
  { code: 'ADULTERE', label: 'Adultère' },
  { code: 'VIOLENCES', label: 'Violences conjugales' },
  { code: 'ABANDON', label: 'Abandon du domicile' },
  { code: 'OUTRAGES', label: 'Outrages / injures' },
  { code: 'DEVOIR_ASSISTANCE', label: 'Devoir d\'assistance' },
  { code: 'DEVOIR_FIDELITE', label: 'Devoir de fidélité' },
  { code: 'DEVOIR_COMMUNAUTE_VIE', label: 'Devoir communauté de vie' },
  { code: 'AUTRE', label: 'Autre' },
];

export function fauteLabel(code: FauteCode): string {
  return FAUTES_FR.find((f) => f.code === code)?.label ?? code;
}

export function verdictProbabiliteLabel(v: VerdictProbabilite): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}

export function verdictTortsLabel(v: VerdictTorts): string {
  switch (v) {
    case 'EXCLUSIF_DEFENDEUR': return 'Tort exclusif du défendeur';
    case 'PARTAGES': return 'Torts partagés';
    case 'IMPREDICTIBLE': return 'Imprédictible';
  }
}
