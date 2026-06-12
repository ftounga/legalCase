// F-284 / SF-284-02 — modèle de l'échéancier procédural proactif (lecture seule).

export type EcheancierUrgency = 'OVERDUE' | 'CRITICAL' | 'SOON' | 'UPCOMING';
export type EcheancierKind = 'DEADLINE' | 'CONTRADICTOIRE';
export type EcheancierSource = 'MANUAL' | 'AI' | 'STATUTORY' | 'CONTRADICTOIRE';

export interface EcheancierItem {
  id: string;
  label: string;
  dueDate: string; // YYYY-MM-DD
  daysUntil: number;
  urgency: EcheancierUrgency;
  kind: EcheancierKind;
  source: EcheancierSource;
}

export interface EcheancierCounts {
  overdue: number;
  critical: number;
  soon: number;
  upcoming: number;
  total: number;
}

export interface EcheancierResponse {
  items: EcheancierItem[];
  nextItem: EcheancierItem | null;
  counts: EcheancierCounts;
}
