import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogEntry } from '../models/audit-log-entry.model';

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly apiUrl = '/api/v1/admin/audit-logs';

  constructor(private http: HttpClient) {}

  getAuditLogs(from?: string, to?: string, page = 0, size = 20): Observable<AuditLogPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    if (from) params = params.set('from', from);
    if (to)   params = params.set('to', to);
    return this.http.get<AuditLogPage>(this.apiUrl, { params });
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export.csv`, { responseType: 'blob' });
  }
}
