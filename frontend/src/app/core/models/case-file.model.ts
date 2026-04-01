export interface CaseFile {
  id: string;
  title: string;
  legalDomain: string;
  description: string | null;
  status: string;
  createdAt: string;
  lastDocumentDeletedAt: string | null;
  riskLevel: string | null;
  riskScore: number | null;
}

export interface CreateCaseFileRequest {
  title: string;
  description?: string;
}
