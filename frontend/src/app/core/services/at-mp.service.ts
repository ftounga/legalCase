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
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-33-at-mp';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AtMpRequest): Observable<AtMpResponse> {
    return this.http.post<AtMpResponse>(
      `/api/v1/case-files/${caseFileId}/at-mp-analysis`, request);
  }

  get(caseFileId: string): Observable<AtMpResponse> {
    return this.http.get<AtMpResponse>(
      `/api/v1/case-files/${caseFileId}/at-mp-analysis`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: AtMpRequest): Observable<AtMpResponse> {
    return this.http.post<AtMpResponse>(
      `/api/v1/simulators/${AtMpService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
