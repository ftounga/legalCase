import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogEntry } from '../models/audit-log-entry.model';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly apiUrl = '/api/v1/admin/audit-logs';

  constructor(private http: HttpClient) {}

  getAuditLogs(): Observable<AuditLogEntry[]> {
    return this.http.get<AuditLogEntry[]>(this.apiUrl);
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export.csv`, { responseType: 'blob' });
  }
}
