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

export interface SectionDiff<T> {
  added: T[];
  removed: T[];
  unchanged: T[];
}

export interface TimelineDiffEntry {
  date: string;
  evenement: string;
}

export interface AnalysisDiff {
  from: { id: string; version: number; analysisType: string; updatedAt: string };
  to:   { id: string; version: number; analysisType: string; updatedAt: string };
  faits: SectionDiff<string>;
  pointsJuridiques: SectionDiff<string>;
  risques: SectionDiff<string>;
  questionsOuvertes: SectionDiff<string>;
  timeline: SectionDiff<TimelineDiffEntry>;
}
