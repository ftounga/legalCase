// F-282 — modèle du cycle contradictoire (rounds d'échange de conclusions).

export type ContradictoireParty = 'OURS' | 'ADVERSE';

export interface ContradictoireRound {
  id: string;
  roundNumber: number;
  party: ContradictoireParty;
  label: string | null;
  datedAt: string;            // ISO date (yyyy-MM-dd)
  responseDueAt: string | null;
  sourceDocumentId: string | null;
  sourceConclusionId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContradictoireSummary {
  currentRoundNumber: number;
  awaitingParty: ContradictoireParty;
  nextDeadline: string | null;
}

export interface ContradictoireTimeline {
  rounds: ContradictoireRound[];
  summary: ContradictoireSummary;
}

/** Charge utile de création / mise à jour d'un round. */
export interface ContradictoireRoundInput {
  party: ContradictoireParty;
  label?: string | null;
  datedAt: string;
  responseDueAt?: string | null;
  sourceDocumentId?: string | null;
  sourceConclusionId?: string | null;
}
