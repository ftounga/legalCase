import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SynthesisSearchResponse } from '../models/search.model';

@Injectable({ providedIn: 'root' })
export class SynthesisSearchService {
  private readonly apiUrl = '/api/v1/search';

  constructor(private http: HttpClient) {}

  search(query: string): Observable<SynthesisSearchResponse> {
    const params = new HttpParams().set('q', query);
    return this.http.get<SynthesisSearchResponse>(this.apiUrl, { params });
  }
}
