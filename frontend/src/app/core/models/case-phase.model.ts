// F-283 / SF-283-01 — modèle des phases procédurales (progression datée du dossier).
// SF-283-03 — enum étendu (immigration administrative + Belgique) + suggestions
// par (domaine × pays).

/** Référentiel des phases (miroir de l'enum backend CasePhaseType). */
export type CasePhaseType =
  | 'SAISINE'
  | 'INTRODUCTION'
  | 'RECOURS_PREALABLE'
  | 'CONCILIATION'
  | 'MISE_EN_ETAT'
  | 'CLOTURE'
  | 'FOND'
  | 'JUGEMENT'
  | 'TRIBUNAL_ADMINISTRATIF'
  | 'CNDA'
  | 'CCE'
  | 'APPEL'
  | 'COUR_ADMINISTRATIVE_APPEL'
  | 'CASSATION'
  | 'CONSEIL_ETAT'
  | 'EXECUTION';

/**
 * Libellé d'affichage FR générique par type (fallback). Couvre TOUS les types,
 * y compris les anciennes phases déjà persistées, pour la rétro-compat de la frise.
 */
export const CASE_PHASE_LABELS: Readonly<Record<CasePhaseType, string>> = {
  SAISINE: 'Saisine',
  INTRODUCTION: 'Introduction',
  RECOURS_PREALABLE: 'Recours préalable',
  CONCILIATION: 'Conciliation',
  MISE_EN_ETAT: 'Mise en état',
  CLOTURE: 'Clôture de l’instruction',
  FOND: 'Jugement au fond',
  JUGEMENT: 'Jugement rendu',
  TRIBUNAL_ADMINISTRATIF: 'Tribunal administratif',
  CNDA: 'Cour nationale du droit d’asile',
  CCE: 'Conseil du Contentieux des Étrangers',
  APPEL: 'Appel',
  COUR_ADMINISTRATIVE_APPEL: 'Cour administrative d’appel',
  CASSATION: 'Cassation',
  CONSEIL_ETAT: 'Conseil d’État',
  EXECUTION: 'Exécution',
};

/**
 * Fallback statique d'options (toutes les phases historiques civiles FR travail).
 * Utilisé tant que les suggestions (domaine × pays) ne sont pas chargées.
 */
export const CASE_PHASE_OPTIONS: ReadonlyArray<{ value: CasePhaseType; label: string }> = [
  { value: 'SAISINE', label: 'Saisine' },
  { value: 'CONCILIATION', label: 'Conciliation (BCO)' },
  { value: 'MISE_EN_ETAT', label: 'Mise en état' },
  { value: 'CLOTURE', label: 'Clôture de l’instruction' },
  { value: 'FOND', label: 'Jugement au fond' },
  { value: 'JUGEMENT', label: 'Jugement rendu' },
  { value: 'APPEL', label: 'Appel' },
  { value: 'CASSATION', label: 'Cassation' },
  { value: 'EXECUTION', label: 'Exécution' },
];

export function casePhaseLabel(phase: CasePhaseType): string {
  return CASE_PHASE_LABELS[phase] ?? phase;
}

/** SF-283-03 — phase suggérée pour le formulaire (type + libellé par défaut éditable). */
export interface CasePhaseSuggestion {
  type: CasePhaseType;
  defaultLabel: string;
}

export interface CasePhase {
  id: string;
  phase: CasePhaseType;
  label: string | null;
  enteredAt: string; // ISO date (yyyy-MM-dd)
  note: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CasePhaseTimeline {
  phases: CasePhase[];
  currentPhase: CasePhaseType | null;
}

/** Charge utile de création / mise à jour d'une phase. */
export interface CasePhaseInput {
  phase: CasePhaseType;
  label?: string | null;
  enteredAt: string;
  note?: string | null;
}
