import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TimeReportRow } from '../models/time-tracking.models';

@Injectable({ providedIn: 'root' })
export class TimeReportService {
  private readonly apiUrl = '/api/v1/workspace/time-report';

  constructor(private http: HttpClient) {}

  getReport(month: string): Observable<TimeReportRow[]> {
    return this.http.get<TimeReportRow[]>(this.apiUrl, { params: { month } });
  }

  exportCsv(month: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export`, {
      params: { month },
      responseType: 'blob'
    });
  }
}
