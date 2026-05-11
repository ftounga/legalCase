import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CrrvRefusVisaRequest,
  CrrvRefusVisaResponse,
} from '../models/crrv-refus-visa.model';

/**
 * SF-208-07 : wrapper HttpClient pour l'outil decisionnel
 * "CRRV recours refus de visa 2 mois FR" (F-IM-23). FR uniquement.
 * Consomme l'API figee dans SF-208-03 (backend, PR #915).
 */
@Injectable({ providedIn: 'root' })
export class CrrvRefusVisaService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CrrvRefusVisaRequest):
      Observable<CrrvRefusVisaResponse> {
    return this.http.post<CrrvRefusVisaResponse>(
      `/api/v1/case-files/${caseFileId}/crrv-refus-visa-analysis`, request);
  }

  get(caseFileId: string): Observable<CrrvRefusVisaResponse> {
    return this.http.get<CrrvRefusVisaResponse>(
      `/api/v1/case-files/${caseFileId}/crrv-refus-visa-analysis`);
  }
}
