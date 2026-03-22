export interface CaseFile {
  id: string;
  title: string;
  legalDomain: string;
  description: string | null;
  status: string;
  createdAt: string;
}

export interface CreateCaseFileRequest {
  title: string;
  description?: string;
}
