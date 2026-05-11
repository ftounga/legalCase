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
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-23-crrv-refus-visa-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-23-crrv-refus-visa-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: CrrvRefusVisaRequest): Observable<CrrvRefusVisaResponse> {
    return this.http.post<CrrvRefusVisaResponse>(
      `/api/v1/simulators/${CrrvRefusVisaService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
