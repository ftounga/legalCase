import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  BlogArticleDetail,
  BlogArticleSummary,
  BlogFilters,
  BlogPage,
} from './blog.model';

@Injectable({ providedIn: 'root' })
export class BlogService {
  private readonly base = '/api/v1/blog';
  private readonly http = inject(HttpClient);

  listArticles(filters: BlogFilters = {}): Observable<BlogPage<BlogArticleSummary>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.country) {
      params = params.set('country', filters.country);
    }
    if (filters.legalDomain) {
      params = params.set('legalDomain', filters.legalDomain);
    }
    return this.http.get<BlogPage<BlogArticleSummary>>(`${this.base}/articles`, { params });
  }

  getArticleBySlug(slug: string): Observable<BlogArticleDetail> {
    return this.http.get<BlogArticleDetail>(`${this.base}/articles/${encodeURIComponent(slug)}`);
  }
}
