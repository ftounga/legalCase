/**
 * SF-DT-31-02 : modèle frontend pour l'outil décisionnel F-DT-31
 * "Transaction / protocole transactionnel" (FR uniquement, art. 2044 Cciv).
 * Contrat importé du backend SF-DT-31-01.
 */

export type ConcessionEmployeur =
  | 'INDEMNITE_TRANSACTIONNELLE'
  | 'MAINTIEN_AVANTAGES_SOCIAUX'
  | 'LETTRE_RECOMMANDATION'
  | 'LEVEE_NON_CONCURRENCE'
  | 'DELAI_PRESCRIPTION_ANTICIPE'
  | 'AUTRE';

export type ConcessionSalarie =
  | 'RENONCIATION_ACTION_PRUDHOMALE'
  | 'RENONCIATION_PARTAGE_BENEFICES'
  | 'AUTRE';

export type RupturePrealable =
  | 'LICENCIEMENT_PERSONNEL'
  | 'LICENCIEMENT_ECONOMIQUE'
  | 'RUPTURE_CONVENTIONNELLE'
  | 'DEMISSION'
  | 'FIN_CDD'
  | 'AUTRE';

export type VerdictValiditeContrat = 'VALIDE' | 'CONTESTABLE' | 'NULLE';

export interface TransactionRequest {
  dateSignature: string;
  concessionsEmployeur: ConcessionEmployeur[];
  concessionsSalarie: ConcessionSalarie[];
  indemniteTransactionnelleEur: number;
  salaireMensuelBrutEur: number;
  ancienneteAnnees: number;
  renonciationActionExpresse: boolean;
  delaiReflexion15jOk: boolean;
  rupturePrealable: RupturePrealable;
  presenceAvocatAssistance: boolean;
  viceConsentementAllégué: boolean;
}

export interface TransactionResponse {
  caseFileId: string;
  // Inputs (echo)
  dateSignature: string;
  concessionsEmployeur: ConcessionEmployeur[];
  concessionsSalarie: ConcessionSalarie[];
  indemniteTransactionnelleEur: number;
  salaireMensuelBrutEur: number;
  ancienneteAnnees: number;
  renonciationActionExpresse: boolean;
  delaiReflexion15jOk: boolean;
  rupturePrealable: RupturePrealable;
  presenceAvocatAssistance: boolean;
  viceConsentementAllégué: boolean;
  // Outputs
  concessionsReciproquesCaracterisees: boolean;
  ratioConcessionsEmployeurPct: number;
  indemniteTransactionnelleSuperieureMacron: boolean;
  scoreValidite: number;
  verdictValiditeContrat: VerdictValiditeContrat;
  risqueNulliteRetenu: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface ConcessionEmployeurOption {
  code: ConcessionEmployeur;
  label: string;
}

export interface ConcessionSalarieOption {
  code: ConcessionSalarie;
  label: string;
}

export interface RupturePrealableOption {
  code: RupturePrealable;
  label: string;
}

export const CONCESSIONS_EMPLOYEUR: ConcessionEmployeurOption[] = [
  { code: 'INDEMNITE_TRANSACTIONNELLE', label: 'Indemnité transactionnelle' },
  { code: 'MAINTIEN_AVANTAGES_SOCIAUX', label: 'Maintien des avantages sociaux' },
  { code: 'LETTRE_RECOMMANDATION', label: 'Lettre de recommandation' },
  { code: 'LEVEE_NON_CONCURRENCE', label: 'Levée de la clause de non-concurrence' },
  { code: 'DELAI_PRESCRIPTION_ANTICIPE', label: 'Délai de prescription anticipé' },
  { code: 'AUTRE', label: 'Autre' },
];

export const CONCESSIONS_SALARIE: ConcessionSalarieOption[] = [
  { code: 'RENONCIATION_ACTION_PRUDHOMALE', label: 'Renonciation à l\'action prud\'homale' },
  { code: 'RENONCIATION_PARTAGE_BENEFICES', label: 'Renonciation au partage des bénéfices' },
  { code: 'AUTRE', label: 'Autre' },
];

export const RUPTURES_PREALABLE: RupturePrealableOption[] = [
  { code: 'LICENCIEMENT_PERSONNEL', label: 'Licenciement (motif personnel)' },
  { code: 'LICENCIEMENT_ECONOMIQUE', label: 'Licenciement (motif économique)' },
  { code: 'RUPTURE_CONVENTIONNELLE', label: 'Rupture conventionnelle' },
  { code: 'DEMISSION', label: 'Démission' },
  { code: 'FIN_CDD', label: 'Fin de CDD' },
  { code: 'AUTRE', label: 'Autre' },
];

export function concessionEmployeurLabel(code: ConcessionEmployeur): string {
  return CONCESSIONS_EMPLOYEUR.find((c) => c.code === code)?.label ?? code;
}

export function concessionSalarieLabel(code: ConcessionSalarie): string {
  return CONCESSIONS_SALARIE.find((c) => c.code === code)?.label ?? code;
}

export function rupturePrealableLabel(code: RupturePrealable): string {
  return RUPTURES_PREALABLE.find((c) => c.code === code)?.label ?? code;
}

export function verdictValiditeLabel(v: VerdictValiditeContrat): string {
  switch (v) {
    case 'VALIDE': return 'Transaction valide';
    case 'CONTESTABLE': return 'Transaction contestable';
    case 'NULLE': return 'Risque de nullité';
  }
}
