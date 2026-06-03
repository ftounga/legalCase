import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PpvExonerationRequest,
  PpvExonerationResponse,
} from '../models/ppv-exoneration.model';

/**
 * SF-218-40 : wrapper HttpClient pour l'outil décisionnel « PPV — exonération
 * (prime de partage de la valeur) » (F-DT-52-ppv-exoneration). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-39 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/ppv-exoneration-analysis
 *
 * Pattern miroir de {@link RttMonetisationService} (F-DT-51, SF-218-38).
 */
@Injectable({ providedIn: 'root' })
export class PpvExonerationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: PpvExonerationRequest):
      Observable<PpvExonerationResponse> {
    return this.http.post<PpvExonerationResponse>(
      `/api/v1/case-files/${caseFileId}/ppv-exoneration-analysis`, request);
  }

  get(caseFileId: string): Observable<PpvExonerationResponse> {
    return this.http.get<PpvExonerationResponse>(
      `/api/v1/case-files/${caseFileId}/ppv-exoneration-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-52-ppv-exoneration')` et sur le
   * seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-52-ppv-exoneration';
}
