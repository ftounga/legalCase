export interface CaseFile {
  id: string;
  title: string;
  legalDomain: string;
  description: string | null;
  status: string;
  createdAt: string;
  lastDocumentDeletedAt: string | null;
}

export interface CreateCaseFileRequest {
  title: string;
  description?: string;
}
