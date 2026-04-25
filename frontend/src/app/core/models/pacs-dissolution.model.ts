/**
 * SF-FA-20-02 : modèles TypeScript pour l'outil décisionnel
 * "Dissolution PACS art. 515-7 Cciv" (F-FA-20, FRANCE uniquement).
 *
 * Contrat figé par SF-FA-20-01 (backend, parallèle).
 */

export type ModeDissolutionPacs =
  | 'DECLARATION_UNILATERALE'
  | 'DECLARATION_CONJOINTE'
  | 'MARIAGE_PARTENAIRES'
  | 'MARIAGE_TIERS'
  | 'DECES';

export type RegimeBiensPacs =
  | 'SEPARATION_BIENS'
  | 'INDIVISION_AMENAGEE'
  | 'INDIVISION_PAR_DEFAUT';

export type CreanceAlleguee =
  | 'CONTRIBUTION_DESEQUILIBRE'
  | 'INVESTISSEMENT_BIEN_PROPRE'
  | 'ENRICHISSEMENT_INJUSTE'
  | 'PRESTATION_TRAVAIL_NON_REMUNEREE'
  | 'AUCUNE';

export type VerdictPacs =
  | 'LIQUIDATION_AMIABLE'
  | 'MEDIATION_OU_JAF'
  | 'CONTENTIEUX_INEVITABLE'
  | 'RIEN_A_FAIRE';

export interface PacsDissolutionRequest {
  /** ISO YYYY-MM-DD — date de conclusion du PACS. */
  dateConclusionPacs: string;
  modeDissolution: ModeDissolutionPacs;
  /** ISO YYYY-MM-DD — date d'enregistrement de la dissolution. */
  dateDissolution: string;
  /** Durée d'union en années entières (depuis la conclusion). */
  dureeUnionAnnees: number;
  regimeBiens: RegimeBiensPacs;
  patrimoineCommunSignificatif: boolean;
  creancesAlleguees: CreanceAlleguee[];
  /** Nombre d'enfants communs (entiers ≥ 0). */
  enfantsCommuns: number;
  /** ISO YYYY-MM-DD — date de notification au partenaire (déclaration unilatérale). */
  dateNotificationPartenaire: string;
}

export interface PacsDissolutionResponse {
  caseFileId: string;
  // Echo input
  dateConclusionPacs: string;
  modeDissolution: ModeDissolutionPacs;
  dateDissolution: string;
  dureeUnionAnnees: number;
  regimeBiens: RegimeBiensPacs;
  patrimoineCommunSignificatif: boolean;
  creancesAlleguees: CreanceAlleguee[];
  enfantsCommuns: number;
  dateNotificationPartenaire: string;
  // Output
  dissolutionValide: boolean;
  delaiNotificationOk: boolean;
  dureeUnionEligibleCreances: boolean;
  scoreCreancesProbables: number;
  verdictRecommandation: VerdictPacs;
  creancesPotentielleVisibles: CreanceAlleguee[];
  delaiPrescriptionAnnees: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

// ---------------------------------------------------------------------------
// Options pour les mat-select.
// ---------------------------------------------------------------------------

export interface ModeDissolutionOption {
  code: ModeDissolutionPacs;
  label: string;
}

export const MODES_DISSOLUTION_FR: ModeDissolutionOption[] = [
  { code: 'DECLARATION_UNILATERALE', label: 'Déclaration unilatérale (notification au partenaire)' },
  { code: 'DECLARATION_CONJOINTE', label: 'Déclaration conjointe (commun accord)' },
  { code: 'MARIAGE_PARTENAIRES', label: 'Mariage entre les partenaires PACS' },
  { code: 'MARIAGE_TIERS', label: 'Mariage de l\'un des partenaires avec un tiers' },
  { code: 'DECES', label: 'Décès de l\'un des partenaires' },
];

export interface RegimeBiensPacsOption {
  code: RegimeBiensPacs;
  label: string;
}

export const REGIMES_BIENS_PACS_FR: RegimeBiensPacsOption[] = [
  { code: 'SEPARATION_BIENS', label: 'Séparation de biens (régime légal depuis 2007)' },
  { code: 'INDIVISION_AMENAGEE', label: 'Indivision aménagée (clause notariée)' },
  { code: 'INDIVISION_PAR_DEFAUT', label: 'Indivision par défaut (PACS conclu avant 2007)' },
];

export interface CreanceAllegueeOption {
  code: CreanceAlleguee;
  label: string;
}

export const CREANCES_ALLEGUEES_FR: CreanceAllegueeOption[] = [
  { code: 'CONTRIBUTION_DESEQUILIBRE', label: 'Contribution disproportionnée aux charges (art. 515-4)' },
  { code: 'INVESTISSEMENT_BIEN_PROPRE', label: 'Investissement dans un bien propre du partenaire' },
  { code: 'ENRICHISSEMENT_INJUSTE', label: 'Enrichissement sans cause (art. 1303)' },
  { code: 'PRESTATION_TRAVAIL_NON_REMUNEREE', label: 'Prestation de travail non rémunérée pour le partenaire' },
  { code: 'AUCUNE', label: 'Aucune créance alléguée' },
];

// ---------------------------------------------------------------------------
// Helpers d'affichage.
// ---------------------------------------------------------------------------

export function modeDissolutionLabel(code: ModeDissolutionPacs): string {
  return MODES_DISSOLUTION_FR.find((o) => o.code === code)?.label ?? code;
}

export function regimeBiensPacsLabel(code: RegimeBiensPacs): string {
  return REGIMES_BIENS_PACS_FR.find((o) => o.code === code)?.label ?? code;
}

export function creanceAllegueeLabel(code: CreanceAlleguee): string {
  return CREANCES_ALLEGUEES_FR.find((o) => o.code === code)?.label ?? code;
}

export function verdictPacsLabel(v: VerdictPacs): string {
  switch (v) {
    case 'LIQUIDATION_AMIABLE': return 'Liquidation amiable';
    case 'MEDIATION_OU_JAF': return 'Médiation ou saisine JAF';
    case 'CONTENTIEUX_INEVITABLE': return 'Contentieux inévitable';
    case 'RIEN_A_FAIRE': return 'Rien à faire (dissolution validée)';
  }
}
