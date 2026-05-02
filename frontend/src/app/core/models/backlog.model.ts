export type BacklogStatus =
  | 'PLANNED'
  | 'READY'
  | 'IN_PROGRESS'
  | 'BLOCKED'
  | 'DONE'
  | 'PARTIAL'
  | 'ABSORBED'
  | 'UNKNOWN';

export type BacklogMarketingStatus =
  | 'A_FAIRE'
  | 'REDIGE'
  | 'EN_COURS'
  | 'TERMINE'
  | 'BLOQUE'
  | 'UNKNOWN';

export type BacklogDomain =
  | 'DROIT_TRAVAIL'
  | 'IMMIGRATION'
  | 'FAMILLE'
  | 'TRANSVERSAL'
  | 'MARKETING'
  | 'UNKNOWN';

export type BacklogPriority = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';

export type BacklogFreshnessStatus = 'OK' | 'STALE' | 'ERROR' | 'UNKNOWN';

export interface BacklogFeatureSummary {
  id: string;
  code: string;
  title: string;
  targetVersion: string | null;
  status: BacklogStatus;
  domain: BacklogDomain;
  priority: BacklogPriority;
  updatedAt: string;
}

export interface BacklogSubfeature {
  id: string;
  code: string;
  title: string;
  status: BacklogStatus;
  description: string | null;
  sourceLine: number | null;
  updatedAt: string;
}

export interface BacklogFeatureDetail {
  id: string;
  code: string;
  title: string;
  targetVersion: string | null;
  status: BacklogStatus;
  description: string | null;
  domain: BacklogDomain;
  priority: BacklogPriority;
  sourceFile: string | null;
  sourceLine: number | null;
  parsedAt: string | null;
  updatedAt: string;
  subfeatures: BacklogSubfeature[];
}

export interface BacklogMarketingTaskSummary {
  id: string;
  code: string;
  title: string;
  status: BacklogMarketingStatus;
  category: string | null;
  updatedAt: string;
}

export interface BacklogFreshness {
  lastSyncAt: string | null;
  lastSuccessAt: string | null;
  status: BacklogFreshnessStatus;
  minutesSinceLastSync: number | null;
}

export interface BacklogSyncResult {
  runId: string;
  durationMs: number;
  featuresCount: number;
  subfeaturesCount: number;
  marketingCount: number;
  orphansMarked: number;
  success: boolean;
}

export interface BacklogFeatureFilters {
  status?: BacklogStatus | null;
  domain?: BacklogDomain | null;
  priority?: BacklogPriority | null;
  search?: string | null;
}

export interface BacklogMarketingFilters {
  status?: BacklogMarketingStatus | null;
  search?: string | null;
}
