// F-286 / SF-286-01 — modèle de la stratégie de dossier unifiée.
// Couche LLM de synthèse EN LECTURE des verdicts d'outils CALCULÉS + synthèse.

export type CaseStrategyStatus = 'READY' | 'EMPTY' | 'EMPTY_INPUT';

export interface CaseStrategy {
  /** READY = une reco existe ; EMPTY = jamais générée ; EMPTY_INPUT = 0 outil calculé (rien d'inventé). */
  status: CaseStrategyStatus;
  /** Markdown de la recommandation, ou null. */
  content: string | null;
  /** ISO-8601, ou null. */
  generatedAt: string | null;
  /** Nombre de verdicts d'outils calculés pris en compte. */
  toolsConsidered: number;
  modelUsed: string | null;
}
