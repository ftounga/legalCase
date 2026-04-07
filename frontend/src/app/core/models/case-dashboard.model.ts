export interface DashboardResponse {
  caseFileId: string;
  legalDomain: string;
  riskScore: number | null;
  riskLevel: string | null;
  licenciement: { scoreRisque: number; verdict: string; criteresNonConformes: number; criteresTotal: number } | null;
  indemnites: { country: string; fourchetteBasse: number; fourhetteHaute: number; baremeSource: string } | null;
  anciennete: { annees: number; mois: number; congesTotalJours: number; ecartsDetectes: number } | null;
  titleDecision: { nbRecommandations: number; premierTitreLabel: string | null } | null;
  workRight: { droitTravail: string; titreLabel: string } | null;
  recours: { recoursLabel: string; dateLimite: string | null; dateLimiteDepassee: boolean } | null;
  partage: { soulte: number; coutTotal: number } | null;
  garde: { gardeLabel: string; joursParentA: number; joursParentB: number } | null;
  divorce: { etapesCompletees: number; etapesTotal: number; piecesPresentes: number; piecesTotal: number; progressionPct: number } | null;
}
