/**
 * SF-DT-22-02 : modèles TypeScript pour l'outil décisionnel
 * "Requalification CDD → CDI" (art. L.1245-1, L.1245-2, L.1242-1 à L.1242-6
 *  Code du travail, FR uniquement).
 *
 * Contrat API figé dans SF-DT-22-01 (backend).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/requalification-cdd-cdi
 */

/**
 * Motif de recours au CDD invoqué par l'employeur (art. L.1242-2).
 * 7 valeurs canoniques + AUTRE (motif n'entrant dans aucune catégorie).
 */
export type MotifCddInvoque =
  | 'ACCROISSEMENT_TEMPORAIRE'
  | 'REMPLACEMENT_SALARIE'
  | 'EMPLOI_SAISONNIER'
  | 'EMPLOI_USAGE'
  | 'CONTRAT_VENDANGE'
  | 'INSERTION_PROFESSIONNELLE'
  | 'AUTRE';

/**
 * Type de motif interdit (art. L.1242-1, L.1242-6).
 */
export type MotifInterditType =
  | 'EMPLOI_PERMANENT'
  | 'REMPLACEMENT_GREVISTE'
  | 'TRAVAUX_DANGEREUX'
  | 'AUTRE';

/**
 * Verdict de probabilité de requalification produit par le scoring backend.
 */
export type VerdictProbabiliteRequalification = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/**
 * Élément d'une succession de CDD (chaque CDD avec ses bornes et son motif).
 */
export interface CddSuccession {
  /** ISO YYYY-MM-DD. */
  dateDebut: string;
  /** ISO YYYY-MM-DD. */
  dateFin: string;
  /** Texte libre (motif de recours). */
  motif: string;
}

export interface RequalificationCddCdiRequest {
  motifCddInvoque: MotifCddInvoque;
  motifInterdit: boolean;
  motifInterditType?: MotifInterditType | null;
  successionCdd: CddSuccession[];
  delaiCarenceRespecte: boolean;
  dureeContratMois: number;
  salaireMensuelBrutEur: number;
  /** ISO YYYY-MM-DD. */
  dateFinDernierContrat: string;
}

export interface RequalificationCddCdiResponse {
  caseFileId: string;
  motifCddInvoque: MotifCddInvoque;
  motifInterdit: boolean;
  motifInterditType: MotifInterditType | null;
  successionCdd: CddSuccession[];
  delaiCarenceRespecte: boolean;
  dureeContratMois: number;
  salaireMensuelBrutEur: number;
  dateFinDernierContrat: string;
  scoreRequalification: number;
  verdictProbabiliteRequalification: VerdictProbabiliteRequalification;
  indemniteRequalificationEur: number;
  indemnitePrecariteEur: number;
  totalDommagesIndemniteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface MotifCddInvoqueOption {
  code: MotifCddInvoque;
  label: string;
}

export interface MotifInterditTypeOption {
  code: MotifInterditType;
  label: string;
}

/**
 * 7 motifs CDD valides art. L.1242-2.
 */
export const MOTIF_CDD_INVOQUE_OPTIONS: MotifCddInvoqueOption[] = [
  { code: 'ACCROISSEMENT_TEMPORAIRE', label: "Accroissement temporaire d'activité" },
  { code: 'REMPLACEMENT_SALARIE', label: 'Remplacement de salarié absent' },
  { code: 'EMPLOI_SAISONNIER', label: 'Emploi saisonnier' },
  { code: 'EMPLOI_USAGE', label: "Emploi par nature temporaire (usage)" },
  { code: 'CONTRAT_VENDANGE', label: 'Contrat vendanges' },
  { code: 'INSERTION_PROFESSIONNELLE', label: "Insertion professionnelle (politique de l'emploi)" },
  { code: 'AUTRE', label: 'Autre motif' },
];

/**
 * 4 types de motif interdit art. L.1242-1 / L.1242-6.
 */
export const MOTIF_INTERDIT_TYPE_OPTIONS: MotifInterditTypeOption[] = [
  { code: 'EMPLOI_PERMANENT', label: "Pourvoir un emploi lié à l'activité normale et permanente" },
  { code: 'REMPLACEMENT_GREVISTE', label: 'Remplacement de salarié gréviste' },
  { code: 'TRAVAUX_DANGEREUX', label: 'Travaux particulièrement dangereux (L.1242-6)' },
  { code: 'AUTRE', label: 'Autre interdiction' },
];

export function motifCddInvoqueLabel(code: MotifCddInvoque): string {
  return MOTIF_CDD_INVOQUE_OPTIONS.find((o) => o.code === code)?.label ?? code;
}

export function motifInterditTypeLabel(code: MotifInterditType): string {
  return MOTIF_INTERDIT_TYPE_OPTIONS.find((o) => o.code === code)?.label ?? code;
}

export function verdictProbabiliteLabel(v: VerdictProbabiliteRequalification): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
