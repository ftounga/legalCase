export interface TimelineEntry {
  date: string;
  evenement: string;
}

export interface AnalysisItem {
  texte: string;
  source: string | null;
  extrait: string | null;
}

export interface CaseAnalysisResult {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  status: string;
  timeline: TimelineEntry[];
  faits: AnalysisItem[];
  pointsJuridiques: AnalysisItem[];
  risques: AnalysisItem[];
  questionsOuvertes: string[];
  piecesManquantes: string[];
  riskLevel: string | null;
  riskScore: number | null;
  modelUsed: string | null;
  updatedAt: string | null;
  analysisDocuments?: { index: number; name: string }[];
}

export interface CaseAnalysisVersionSummary {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  updatedAt: string;
  faitsCount: number | null;
  pointsJuridiquesCount: number | null;
  risquesCount: number | null;
  questionsOuvertesCount: number | null;
  timelineCount: number | null;
}

export interface DiffItem {
  text: string;
  reason: string | null;
}

export interface SectionDiff {
  added: DiffItem[];
  removed: DiffItem[];
  unchanged: DiffItem[];
  enriched: DiffItem[];
}

export interface TimelineDiffItem {
  date: string;
  evenement: string;
  reason: string | null;
}

export interface TimelineSectionDiff {
  added: TimelineDiffItem[];
  removed: TimelineDiffItem[];
  unchanged: TimelineDiffItem[];
  enriched: TimelineDiffItem[];
}

export interface AnalysisDiff {
  from: { id: string; version: number; analysisType: string; updatedAt: string };
  to:   { id: string; version: number; analysisType: string; updatedAt: string };
  faits: SectionDiff;
  pointsJuridiques: SectionDiff;
  risques: SectionDiff;
  questionsOuvertes: SectionDiff;
  timeline: TimelineSectionDiff;
}
