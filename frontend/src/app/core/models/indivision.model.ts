/**
 * SF-FA-22-02 : modèles frontend pour l'outil décisionnel "Indivision
 * post-communautaire" (art. 815 et s. Cciv) — single-country FR DROIT_FAMILLE.
 *
 * Contrat API importé de SF-FA-22-01 (backend).
 */

export type NatureBien =
  | 'IMMOBILIER'
  | 'MOBILIER'
  | 'COMPTES_BANCAIRES'
  | 'TITRES_FINANCIERS'
  | 'FONDS_COMMERCE'
  | 'AUTRE';

export type TentativePartage =
  | 'PROPOSITION_NOTAIRE'
  | 'MEDIATION'
  | 'EXPERTISE_VALORISATION'
  | 'LICITATION_AMIABLE'
  | 'AUCUNE';

export type VerdictIndivision =
  | 'PARTAGE_AMIABLE_POSSIBLE'
  | 'PARTAGE_JUDICIAIRE_RECOMMANDE'
  | 'LICITATION_REQUISE'
  | 'MEDIATION_PREALABLE';

export interface IndivisionRequest {
  dateOrigineIndivision: string;
  natureBiens: NatureBien[];
  valeurEstimeeTotaleEur: number;
  nbIndivisaires: number;
  quotesPart: number[];
  tentativesPartageAmiable: TentativePartage[];
  consentementPartageGlobal: boolean;
  occupationBienParUnIndivisaire: boolean;
  indivisionDureeAnnees: number;
  demandeMesuresConservatoires: boolean;
  conflitOuvertEntreIndivisaires: boolean;
}

export interface IndivisionResponse {
  caseFileId: string;
  // Echoed input
  dateOrigineIndivision: string;
  natureBiens: NatureBien[];
  valeurEstimeeTotaleEur: number;
  nbIndivisaires: number;
  quotesPart: number[];
  tentativesPartageAmiable: TentativePartage[];
  consentementPartageGlobal: boolean;
  occupationBienParUnIndivisaire: boolean;
  indivisionDureeAnnees: number;
  demandeMesuresConservatoires: boolean;
  conflitOuvertEntreIndivisaires: boolean;

  // Computed output
  scoreEligibilitePartageJudiciaire: number;
  verdictRecommandation: VerdictIndivision;
  indemniteOccupationDueEur: number;
  expertiseNotarialeRecommandee: boolean;
  licitationRecommandee: boolean;
  delaiProcedurePartageJudiciaireMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface NatureBienOption {
  code: NatureBien;
  label: string;
}

export interface TentativePartageOption {
  code: TentativePartage;
  label: string;
}

export const NATURE_BIEN_OPTIONS: ReadonlyArray<NatureBienOption> = [
  { code: 'IMMOBILIER', label: 'Immobilier' },
  { code: 'MOBILIER', label: 'Mobilier' },
  { code: 'COMPTES_BANCAIRES', label: 'Comptes bancaires' },
  { code: 'TITRES_FINANCIERS', label: 'Titres financiers' },
  { code: 'FONDS_COMMERCE', label: 'Fonds de commerce' },
  { code: 'AUTRE', label: 'Autre' },
];

export const TENTATIVE_PARTAGE_OPTIONS: ReadonlyArray<TentativePartageOption> = [
  { code: 'PROPOSITION_NOTAIRE', label: 'Proposition notariale' },
  { code: 'MEDIATION', label: 'Médiation' },
  { code: 'EXPERTISE_VALORISATION', label: 'Expertise de valorisation' },
  { code: 'LICITATION_AMIABLE', label: 'Licitation amiable' },
  { code: 'AUCUNE', label: 'Aucune' },
];

export function natureBienLabel(code: NatureBien): string {
  return NATURE_BIEN_OPTIONS.find(o => o.code === code)?.label ?? code;
}

export function tentativePartageLabel(code: TentativePartage): string {
  return TENTATIVE_PARTAGE_OPTIONS.find(o => o.code === code)?.label ?? code;
}

export function verdictIndivisionLabel(verdict: VerdictIndivision): string {
  switch (verdict) {
    case 'PARTAGE_AMIABLE_POSSIBLE': return 'Partage amiable possible';
    case 'PARTAGE_JUDICIAIRE_RECOMMANDE': return 'Partage judiciaire recommandé';
    case 'LICITATION_REQUISE': return 'Licitation requise';
    case 'MEDIATION_PREALABLE': return 'Médiation préalable';
  }
}
