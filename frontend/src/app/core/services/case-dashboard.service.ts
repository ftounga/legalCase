import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardResponse } from '../models/case-dashboard.model';

@Injectable({ providedIn: 'root' })
export class CaseDashboardService {
  constructor(private http: HttpClient) {}

  get(caseFileId: string): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`/api/v1/case-files/${caseFileId}/dashboard`);
  }
}
