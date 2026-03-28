import { CaseAnalysisResult } from './case-analysis.model';

export interface ShareResponse {
  id: string;
  shareUrl: string;
  expiresAt: string;
  createdAt: string;
}

export interface PublicShareResponse {
  caseFileId: string;
  caseFileTitle: string;
  legalDomain: string | null;
  expiresAt: string;
  synthesis: CaseAnalysisResult;
}
