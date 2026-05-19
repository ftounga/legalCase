export interface DashboardOpenCase {
  id: string;
  title: string;
  legalDomain: string;
  status: string;
}

export interface DashboardDeadline {
  id: string;
  label: string;
  dueDate: string;
  caseFileId: string;
  caseFileTitle: string;
}

export interface DashboardStaleCheck {
  caseFileId: string;
  caseFileTitle: string;
  nonCompliantCount: number;
}

export interface DashboardAnalysis {
  id: string;
  caseFileId: string;
  caseFileTitle: string;
  analysisType: string;
  createdAt: string;
}

/** F-249 — un jour de la tendance d'activité hebdomadaire. */
export interface DashboardActivityDay {
  date: string;          // ISO yyyy-MM-dd
  analysesCount: number;
}

export interface DashboardSummary {
  openCases: DashboardOpenCase[];
  openCasesCount: number;
  urgentDeadlines: DashboardDeadline[];
  staleChecks: DashboardStaleCheck[];
  recentAnalyses: DashboardAnalysis[];
  // ── F-249 — enrichissement page d'accueil ──
  userFirstName: string | null;
  casesOpenedThisWeek: number;
  weeklyActivity: DashboardActivityDay[];
}
