import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TimeReportRow } from '../models/time-tracking.models';

interface TimeReportResponse {
  month: string;
  lines: TimeReportRow[];
}

@Injectable({ providedIn: 'root' })
export class TimeReportService {
  private readonly apiUrl = '/api/v1/workspace/time-report';

  constructor(private http: HttpClient) {}

  getReport(month: string): Observable<TimeReportRow[]> {
    return this.http.get<TimeReportResponse>(this.apiUrl, { params: { month } }).pipe(
      map(response => response.lines)
    );
  }

  getMyReport(month: string): Observable<TimeReportRow[]> {
    return this.http.get<TimeReportResponse>(`${this.apiUrl}/my`, { params: { month } }).pipe(
      map(response => response.lines)
    );
  }

  exportCsv(month: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export`, {
      params: { month },
      responseType: 'blob'
    });
  }
}
