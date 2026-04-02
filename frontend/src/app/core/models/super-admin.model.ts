export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

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

export interface QueueHealth {
  name: string;
  messagesReady: number;
  messagesUnacknowledged: number;
  consumers: number;
  available: boolean;
}

export interface JobStats {
  done: number;
  failed: number;
  processing: number;
  pending: number;
}

export interface PipelineHealth {
  queues: QueueHealth[];
  jobsLast24h: JobStats;
  jobsLast7d: JobStats;
  rabbitmqAvailable: boolean;
}
