import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogEntry } from '../models/audit-log-entry.model';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly apiUrl = '/api/v1/admin/audit-logs';

  constructor(private http: HttpClient) {}

  getAuditLogs(from?: string, to?: string): Observable<AuditLogEntry[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to)   params = params.set('to', to);
    return this.http.get<AuditLogEntry[]>(this.apiUrl, { params });
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export.csv`, { responseType: 'blob' });
  }
}
