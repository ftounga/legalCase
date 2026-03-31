export interface SuperAdminWorkspace {
  id: string;
  name: string;
  slug: string;
  planCode: string;
  status: string;
  expiresAt: string | null;
  memberCount: number;
  createdAt: string;
}

export interface SuperAdminUsage {
  workspaceId: string;
  workspaceName: string;
  totalTokensInput: number;
  totalTokensOutput: number;
  totalCost: number;
}

export interface SuperAdminUser {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  workspaceCount: number;
}

export interface SuperAdminMetrics {
  totalWorkspaces: number;
  activeWorkspaces30d: number;
  inactiveWorkspaces30d: number;
  trialWorkspaces: number;
  paidWorkspaces: number;
  conversionRatePct: number;
  analysesLast7Days: number;
  analysesLast30Days: number;
  newWorkspacesLast30Days: number;
}
