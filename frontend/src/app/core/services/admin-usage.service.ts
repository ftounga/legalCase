import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkspaceUsageSummary } from '../models/workspace-usage-summary.model';

@Injectable({ providedIn: 'root' })
export class AdminUsageService {
  private readonly apiUrl = '/api/v1/admin/usage';

  constructor(private http: HttpClient) {}

  getSummary(): Observable<WorkspaceUsageSummary> {
    return this.http.get<WorkspaceUsageSummary>(this.apiUrl);
  }
}
