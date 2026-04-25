import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecompensesRequest,
  RecompensesResponse,
} from '../models/recompenses.model';

/**
 * SF-FA-15-02 : service HTTP pour l'outil décisionnel Récompenses
 * (art. 1437/1469 Cciv). Backend SF-FA-15-01 (PR #572).
 */
@Injectable({ providedIn: 'root' })
export class RecompensesService {
  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RecompensesRequest): Observable<RecompensesResponse> {
    return this.http.post<RecompensesResponse>(
      `/api/v1/case-files/${caseFileId}/recompenses`,
      request,
    );
  }

  get(caseFileId: string): Observable<RecompensesResponse> {
    return this.http.get<RecompensesResponse>(
      `/api/v1/case-files/${caseFileId}/recompenses`,
    );
  }
}
