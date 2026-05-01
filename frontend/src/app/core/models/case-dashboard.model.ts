/**
 * F-167 SF-167-01 — DTO d'une tile générique du dashboard agrégé.
 *
 * Mappe le record backend `fr.ailegalcase.casefile.DashboardTile` (champs
 * sérialisés tels quels par Jackson). Coexiste avec les 9 records typés
 * existants (`licenciement`, `indemnites`, …) pendant la transition F-167.
 * SF-167-05 fusionnera et supprimera les records typés.
 */
export interface DashboardTile {
  toolId: string;
  theme: 'INDEMNITES' | 'VALIDITE' | 'DELAIS' | 'DOCUMENTS' | 'DIAGNOSTIC';
  label: string;
  primaryValue: string;
  secondaryValue?: string | null;
  alertLevel?: 'OK' | 'WARNING' | 'ALERT' | null;
}

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

  // F-167 SF-167-01 — Liste générique de tiles décisionnelles.
  tiles?: DashboardTile[];
}
