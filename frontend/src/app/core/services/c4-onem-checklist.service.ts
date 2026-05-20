import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  C4OnemChecklistRequest,
  C4OnemChecklistResponse,
} from '../models/c4-onem-checklist.model';

/**
 * SF-207-04 (frontend) : wrapper HttpClient pour l'outil décisionnel
 * « C4 ONEM — checklist conformité » (F-207, Belgique uniquement).
 * Consomme l'API figée dans SF-207-02-backend (PR #1123).
 *
 * Endpoint : {@code /api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist}
 */
@Injectable({ providedIn: 'root' })
export class C4OnemChecklistService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/c4-onem-checklist`;
  }

  calculate(caseFileId: string, request: C4OnemChecklistRequest):
      Observable<C4OnemChecklistResponse> {
    return this.http.post<C4OnemChecklistResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<C4OnemChecklistResponse> {
    return this.http.get<C4OnemChecklistResponse>(this.endpoint(caseFileId));
  }
}
