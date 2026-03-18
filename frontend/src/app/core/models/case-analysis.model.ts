export interface TimelineEntry {
  date: string;
  evenement: string;
}

export interface CaseAnalysisResult {
  status: string;
  timeline: TimelineEntry[];
  faits: string[];
  pointsJuridiques: string[];
  risques: string[];
  questionsOuvertes: string[];
  modelUsed: string | null;
  updatedAt: string | null;
}
