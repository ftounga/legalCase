import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreditTempsBeRequest,
  CreditTempsBeResponse,
} from '../models/credit-temps-be.model';

/**
 * SF-DT-29-02 : wrapper HttpClient pour l'outil décisionnel
 * "Crédit-temps / interruption de carrière BE" (F-DT-29). BE uniquement.
 * Consomme l'API figée dans SF-DT-29-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class CreditTempsBeService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-29-credit-temps-be';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: CreditTempsBeRequest):
      Observable<CreditTempsBeResponse> {
    return this.http.post<CreditTempsBeResponse>(
      `/api/v1/case-files/${caseFileId}/credit-temps-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CreditTempsBeResponse> {
    return this.http.get<CreditTempsBeResponse>(
      `/api/v1/case-files/${caseFileId}/credit-temps-be-analysis`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: CreditTempsBeRequest): Observable<CreditTempsBeResponse> {
    return this.http.post<CreditTempsBeResponse>(
      `/api/v1/simulators/${CreditTempsBeService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
