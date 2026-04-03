import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import {
  TimeEntryResponse,
  BillingRateResponse,
  BillingRateRequest
} from '../models/time-tracking.models';

@Injectable({ providedIn: 'root' })
export class TimeService {
  private readonly apiUrl = '/api/v1';

  readonly activeEntry = signal<TimeEntryResponse | null>(null);
  readonly entries = signal<TimeEntryResponse[]>([]);

  constructor(private http: HttpClient) {}

  startTimer(caseFileId: string): Observable<TimeEntryResponse> {
    return this.http
      .post<TimeEntryResponse>(`${this.apiUrl}/case-files/${caseFileId}/time-entries/start`, {})
      .pipe(tap(entry => this.activeEntry.set(entry)));
  }

  stopTimer(entryId: string): Observable<TimeEntryResponse> {
    return this.http
      .post<TimeEntryResponse>(`${this.apiUrl}/time-entries/${entryId}/stop`, {})
      .pipe(
        tap(stopped => {
          this.activeEntry.set(null);
          this.entries.update(list =>
            list.map(e => (e.id === stopped.id ? stopped : e))
          );
        })
      );
  }

  loadEntries(caseFileId: string): Observable<void> {
    return this.http
      .get<TimeEntryResponse[]>(`${this.apiUrl}/case-files/${caseFileId}/time-entries`)
      .pipe(
        tap(list => {
          this.entries.set(list);
          const active = list.find(e => !e.stoppedAt) ?? null;
          this.activeEntry.set(active);
        }),
        map(() => void 0)
      );
  }

  getBillingRate(): Observable<BillingRateResponse | null> {
    return this.http.get<BillingRateResponse | null>(`${this.apiUrl}/workspace/billing-rate`);
  }

  getMemberRates(): Observable<Record<string, BillingRateResponse>> {
    return this.http.get<Record<string, BillingRateResponse>>(
      `${this.apiUrl}/workspace/members/billing-rates`
    );
  }

  setRateForMember(userId: string, rate: number): Observable<BillingRateResponse> {
    const body: BillingRateRequest = { ratePerHour: rate };
    return this.http.put<BillingRateResponse>(
      `${this.apiUrl}/workspace/members/${userId}/billing-rate`, body
    );
  }

  /**
   * Formate une durée en secondes vers "1h 23min 45s".
   * Omissions :
   *  - heures absentes si 0
   *  - minutes absentes si 0 et heures aussi 0
   */
  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;

    const ss = String(s).padStart(2, '0');
    const mm = String(m).padStart(2, '0');

    if (h > 0) {
      return `${h}h ${mm}min ${ss}s`;
    }
    if (m > 0) {
      return `${m}min ${ss}s`;
    }
    return `${s}s`;
  }
}
