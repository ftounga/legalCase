export interface UserUsageSummary {
  userId: string;
  userEmail: string;
  tokensInput: number;
  tokensOutput: number;
  totalCost: number;
}

export interface CaseFileUsageSummary {
  caseFileId: string;
  caseFileTitle: string;
  tokensInput: number;
  tokensOutput: number;
  totalCost: number;
}

export interface WorkspaceUsageSummary {
  totalTokensInput: number;
  totalTokensOutput: number;
  totalCost: number;
  byUser: UserUsageSummary[];
  byCaseFile: CaseFileUsageSummary[];
}
