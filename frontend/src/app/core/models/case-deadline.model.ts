export interface CaseDeadline {
  id: string;
  label: string;
  dueDate: string; // YYYY-MM-DD
  createdAt: string;
  updatedAt: string;
  source: 'MANUAL' | 'AI' | 'STATUTORY';
  aiStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED' | null;
}
