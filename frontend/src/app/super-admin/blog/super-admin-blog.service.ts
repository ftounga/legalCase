import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BlogArticleSummary } from '../../blog/blog.model';

export interface RunNowResponse {
  success: boolean;
  message: string;
}

export interface CostSummary {
  anthropicMonthlyEur: number;
  anthropicProjectedEur: number;
  openaiMonthlyEur: number;
  openaiProjectedEur: number;
}

@Injectable({ providedIn: 'root' })
export class SuperAdminBlogService {
  private readonly base = '/api/v1/super-admin/blog';
  private readonly http = inject(HttpClient);

  listArticlesByStatus(status = 'NEEDS_REVIEW'): Observable<BlogArticleSummary[]> {
    return this.http.get<BlogArticleSummary[]>(`${this.base}/articles`, { params: { status } });
  }

  approve(articleId: string): Observable<BlogArticleSummary> {
    return this.http.post<BlogArticleSummary>(`${this.base}/articles/${articleId}/approve`, {});
  }

  reject(articleId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/articles/${articleId}/reject`, {});
  }

  runNow(): Observable<RunNowResponse> {
    return this.http.post<RunNowResponse>(`${this.base}/run-now`, {});
  }

  getCostSummary(): Observable<CostSummary> {
    return this.http.get<CostSummary>(`${this.base}/cost-summary`);
  }
}
