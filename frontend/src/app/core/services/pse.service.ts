import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PseRequest, PseResponse } from '../models/pse.model';

/**
 * SF-DT-14-02 : wrapper HttpClient pour l'outil décisionnel
 * "PSE — critères de validité" (F-DT-14). FR uniquement.
 * Consomme l'API figée dans SF-DT-14-01 (backend, mergé PR #623).
 */
@Injectable({ providedIn: 'root' })
export class PseService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PseRequest): Observable<PseResponse> {
    return this.http.post<PseResponse>(
      `/api/v1/case-files/${caseFileId}/pse-analysis`, request);
  }

  get(caseFileId: string): Observable<PseResponse> {
    return this.http.get<PseResponse>(
      `/api/v1/case-files/${caseFileId}/pse-analysis`);
  }
}
