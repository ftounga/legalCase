import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IndemnitePreavisRequest,
  IndemnitePreavisResponse,
} from '../models/indemnite-preavis.model';

/**
 * SF-DT-25-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité compensatrice de préavis" (F-DT-25, FR uniquement).
 * Consomme l'API figée dans SF-DT-25-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class IndemnitePreavisService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: IndemnitePreavisRequest):
      Observable<IndemnitePreavisResponse> {
    return this.http.post<IndemnitePreavisResponse>(
      `/api/v1/case-files/${caseFileId}/indemnite-preavis`, request);
  }

  get(caseFileId: string): Observable<IndemnitePreavisResponse> {
    return this.http.get<IndemnitePreavisResponse>(
      `/api/v1/case-files/${caseFileId}/indemnite-preavis`);
  }
}
