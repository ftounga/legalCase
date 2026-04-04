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

export interface DashboardSummary {
  openCases: DashboardOpenCase[];
  openCasesCount: number;
  urgentDeadlines: DashboardDeadline[];
  staleChecks: DashboardStaleCheck[];
  recentAnalyses: DashboardAnalysis[];
}
