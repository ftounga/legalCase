import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BacklogFeatureDetail,
  BacklogFeatureFilters,
  BacklogFeatureSummary,
  BacklogFreshness,
  BacklogMarketingFilters,
  BacklogMarketingTaskSummary,
  BacklogSyncResult,
} from '../models/backlog.model';
import { PageResponse } from '../models/super-admin.model';

@Injectable({ providedIn: 'root' })
export class BacklogAdminService {
  private readonly base = '/api/v1/super-admin/backlog';

  constructor(private http: HttpClient) {}

  searchFeatures(
    filters: BacklogFeatureFilters,
    page = 0,
    size = 50,
  ): Observable<PageResponse<BacklogFeatureSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.domain) params = params.set('domain', filters.domain);
    if (filters.priority) params = params.set('priority', filters.priority);
    if (filters.search && filters.search.trim().length > 0) {
      params = params.set('search', filters.search.trim());
    }
    return this.http.get<PageResponse<BacklogFeatureSummary>>(`${this.base}/features`, { params });
  }

  getFeatureDetail(code: string): Observable<BacklogFeatureDetail> {
    return this.http.get<BacklogFeatureDetail>(`${this.base}/features/${encodeURIComponent(code)}`);
  }

  searchMarketingTasks(
    filters: BacklogMarketingFilters,
    page = 0,
    size = 50,
  ): Observable<PageResponse<BacklogMarketingTaskSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.search && filters.search.trim().length > 0) {
      params = params.set('search', filters.search.trim());
    }
    return this.http.get<PageResponse<BacklogMarketingTaskSummary>>(`${this.base}/marketing-tasks`, { params });
  }

  getFreshness(): Observable<BacklogFreshness> {
    return this.http.get<BacklogFreshness>(`${this.base}/freshness`);
  }

  triggerSync(): Observable<BacklogSyncResult> {
    return this.http.post<BacklogSyncResult>(`${this.base}/sync`, {});
  }
}
