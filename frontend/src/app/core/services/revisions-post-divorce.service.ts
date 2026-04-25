import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RevisionsPostDivorceRequest,
  RevisionsPostDivorceResponse,
} from '../models/revisions-post-divorce.model';

/**
 * SF-FA-13-02 : wrapper HttpClient pour l'outil décisionnel
 * "Révisions post-divorce" (F-FA-13). Consomme l'API figée dans
 * SF-FA-13-01 (backend, parallèle).
 */
@Injectable({ providedIn: 'root' })
export class RevisionsPostDivorceService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RevisionsPostDivorceRequest):
      Observable<RevisionsPostDivorceResponse> {
    return this.http.post<RevisionsPostDivorceResponse>(
      `/api/v1/case-files/${caseFileId}/revisions-post-divorce`, request);
  }

  get(caseFileId: string): Observable<RevisionsPostDivorceResponse> {
    return this.http.get<RevisionsPostDivorceResponse>(
      `/api/v1/case-files/${caseFileId}/revisions-post-divorce`);
  }
}
