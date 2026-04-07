import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TitleDecisionRequest, TitleDecisionResponse } from '../models/immigration-title-decision.model';

@Injectable({ providedIn: 'root' })
export class ImmigrationTitleDecisionService {

  constructor(private http: HttpClient) {}

  resolve(caseFileId: string, request: TitleDecisionRequest): Observable<TitleDecisionResponse> {
    return this.http.post<TitleDecisionResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/title-decision`,
      request
    );
  }

  get(caseFileId: string): Observable<TitleDecisionResponse> {
    return this.http.get<TitleDecisionResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/title-decision`
    );
  }
}
