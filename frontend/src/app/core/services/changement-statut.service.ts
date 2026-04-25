import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChangementStatutRequest, ChangementStatutResponse } from '../models/changement-statut.model';

/**
 * SF-IM-11-02 : wrapper HttpClient pour l'outil décisionnel "Changement de
 * statut" (F-IM-11). FR uniquement (régime CESEDA).
 * Consomme l'API figée dans SF-IM-11-01 (backend, mergé PR #635).
 */
@Injectable({ providedIn: 'root' })
export class ChangementStatutService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ChangementStatutRequest): Observable<ChangementStatutResponse> {
    return this.http.post<ChangementStatutResponse>(
      `/api/v1/case-files/${caseFileId}/changement-statut-analysis`, request);
  }

  get(caseFileId: string): Observable<ChangementStatutResponse> {
    return this.http.get<ChangementStatutResponse>(
      `/api/v1/case-files/${caseFileId}/changement-statut-analysis`);
  }
}
