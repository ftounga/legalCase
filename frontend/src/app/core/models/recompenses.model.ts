/**
 * SF-FA-15-02 : modèles frontend pour l'outil décisionnel "Récompenses"
 * (art. 1437 et 1469 Cciv) — single-country FR DROIT_FAMILLE.
 *
 * Contrat API importé de SF-FA-15-01 (backend mergé PR #572).
 */

export type RegimeMatrimonialRecompenses =
  | 'COMMUNAUTE_LEGALE'
  | 'PARTICIPATION_AUX_ACQUETS'
  | 'COMMUNAUTE_UNIVERSELLE';

export type TypeOperationRecompense =
  | 'DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE'
  | 'DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE';

export type NatureBienRecompense =
  | 'ACQUISITION_BIEN_PROPRE'
  | 'CONSERVATION_BIEN_PROPRE'
  | 'AMELIORATION_BIEN_PROPRE'
  | 'DEPENSES_USUELLES'
  | 'AUTRE';

export type RegleApplicableRecompense =
  | 'DEPENSE_FAITE'
  | 'PLUS_FAIBLE_DEPENSE_OU_PROFIT'
  | 'PROFIT_SUBSISTANT_PLUS_FORTE';

export type DirectionRecompense =
  | 'COMMUNAUTE_DOIT_PROPRE'
  | 'PROPRE_DOIT_COMMUNAUTE';

/**
 * Une opération unitaire à analyser (input avocat).
 */
export interface OperationRecompense {
  id: string;
  type: TypeOperationRecompense;
  natureBien: NatureBienRecompense;
  montantDepenseEur: number;
  valeurInitialeBienEur: number | null;
  valeurActuelleBienEur: number | null;
  description: string | null;
}

/**
 * Détail d'une récompense calculée (output backend).
 */
export interface RecompenseDetail {
  operationId: string;
  type: TypeOperationRecompense;
  natureBien: NatureBienRecompense;
  regleApplicable: RegleApplicableRecompense;
  depenseFaiteEur: number;
  profitSubsistantEur: number | null;
  montantRecompenseEur: number;
  directionRecompense: DirectionRecompense;
  baseJuridiqueOperation: string;
  formule: string;
  description: string | null;
}

export interface RecompensesRequest {
  regimeMatrimonial: RegimeMatrimonialRecompenses;
  operations: OperationRecompense[];
}

export interface RecompensesResponse {
  caseFileId: string;
  regimeMatrimonial: RegimeMatrimonialRecompenses;
  recompenses: RecompenseDetail[];
  totalRecompensesDuesParCommunauteEur: number;
  totalRecompensesDuesParEpouxEur: number;
  soldeNetPourEpouxEur: number;
  baseJuridiqueGenerale: string;
  messages: string[];
  country: string;
}

export interface RegimeOption {
  code: RegimeMatrimonialRecompenses;
  label: string;
}

export interface TypeOperationOption {
  code: TypeOperationRecompense;
  label: string;
}

export interface NatureBienOption {
  code: NatureBienRecompense;
  label: string;
}

export const REGIME_OPTIONS: ReadonlyArray<RegimeOption> = [
  { code: 'COMMUNAUTE_LEGALE', label: 'Communauté légale (régime légal par défaut)' },
  { code: 'PARTICIPATION_AUX_ACQUETS', label: 'Participation aux acquêts' },
  { code: 'COMMUNAUTE_UNIVERSELLE', label: 'Communauté universelle' },
];

export const TYPE_OPERATION_OPTIONS: ReadonlyArray<TypeOperationOption> = [
  { code: 'DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE', label: 'Dépense propre au profit de la communauté' },
  { code: 'DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE', label: 'Dépense communauté au profit d’un propre' },
];

export const NATURE_BIEN_OPTIONS: ReadonlyArray<NatureBienOption> = [
  { code: 'ACQUISITION_BIEN_PROPRE', label: 'Acquisition d’un bien propre' },
  { code: 'CONSERVATION_BIEN_PROPRE', label: 'Conservation d’un bien propre' },
  { code: 'AMELIORATION_BIEN_PROPRE', label: 'Amélioration d’un bien propre' },
  { code: 'DEPENSES_USUELLES', label: 'Dépenses usuelles / nécessaires' },
  { code: 'AUTRE', label: 'Autre' },
];
