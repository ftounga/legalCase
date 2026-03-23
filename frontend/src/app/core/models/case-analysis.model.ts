export interface TimelineEntry {
  date: string;
  evenement: string;
}

export interface CaseAnalysisResult {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  status: string;
  timeline: TimelineEntry[];
  faits: string[];
  pointsJuridiques: string[];
  risques: string[];
  questionsOuvertes: string[];
  modelUsed: string | null;
  updatedAt: string | null;
}

export interface CaseAnalysisVersionSummary {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  updatedAt: string;
}
