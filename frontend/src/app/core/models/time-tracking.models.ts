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
