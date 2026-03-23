export interface AuditLogEntry {
  id: string;
  action: string;
  userEmail: string;
  caseFileId: string | null;
  caseFileTitle: string;
  createdAt: string;
}
