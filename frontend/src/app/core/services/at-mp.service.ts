import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AtMpRequest, AtMpResponse } from '../models/at-mp.model';

/**
 * SF-DT-33-02 : wrapper HttpClient pour l'outil décisionnel "Accident du travail /
 * Maladie professionnelle" (F-DT-33). FR uniquement.
 * Consomme l'API figée dans SF-DT-33-01 (backend, mergé PR #649).
 */
@Injectable({ providedIn: 'root' })
export class AtMpService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AtMpRequest): Observable<AtMpResponse> {
    return this.http.post<AtMpResponse>(
      `/api/v1/case-files/${caseFileId}/at-mp-analysis`, request);
  }

  get(caseFileId: string): Observable<AtMpResponse> {
    return this.http.get<AtMpResponse>(
      `/api/v1/case-files/${caseFileId}/at-mp-analysis`);
  }
}
