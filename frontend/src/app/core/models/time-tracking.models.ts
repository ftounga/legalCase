export interface TimeEntryResponse {
  id: string;
  caseFileId: string;
  startedAt: string; // ISO
  stoppedAt?: string; // ISO, absent si actif
  durationSeconds?: number;
}

export interface BillingRateResponse {
  ratePerHour: number;
  effectiveFrom: string;
}

export interface BillingRateRequest {
  ratePerHour: number;
}

export interface TimeReportRow {
  caseFileId: string;
  caseFileTitle: string;
  userId: string;
  userEmail: string;
  totalSeconds: number;
  ratePerHour: number | null;
  amount: number | null;
}
