// F-283 / SF-283-01 — modèle des phases procédurales (progression datée du dossier).

/** Référentiel des phases (miroir de l'enum backend CasePhaseType). */
export type CasePhaseType =
  | 'SAISINE'
  | 'CONCILIATION'
  | 'MISE_EN_ETAT'
  | 'FOND'
  | 'JUGEMENT'
  | 'APPEL'
  | 'CASSATION'
  | 'EXECUTION';

/** Ordre + libellé d'affichage FR de chaque phase. */
export const CASE_PHASE_OPTIONS: ReadonlyArray<{ value: CasePhaseType; label: string }> = [
  { value: 'SAISINE', label: 'Saisine' },
  { value: 'CONCILIATION', label: 'Conciliation (BCO)' },
  { value: 'MISE_EN_ETAT', label: 'Mise en état' },
  { value: 'FOND', label: 'Jugement au fond' },
  { value: 'JUGEMENT', label: 'Jugement rendu' },
  { value: 'APPEL', label: 'Appel' },
  { value: 'CASSATION', label: 'Cassation' },
  { value: 'EXECUTION', label: 'Exécution' },
];

export function casePhaseLabel(phase: CasePhaseType): string {
  return CASE_PHASE_OPTIONS.find((o) => o.value === phase)?.label ?? phase;
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
