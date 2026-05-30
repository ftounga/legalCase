import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PourvoiCassationSocRequest,
  PourvoiCassationSocResponse,
} from '../models/pourvoi-cassation-soc.model';

/**
 * SF-218-06 : wrapper HttpClient pour l'outil décisionnel « Pourvoi en
 * cassation chambre sociale » (F-DT-87-pourvoi-cassation-soc). FRANCE
 * uniquement. Consomme l'API figée dans SF-218-05 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/pourvoi-cassation-soc-analysis
 *
 * Pattern miroir de {@link AppelCphService} (F-DT-86, SF-218-02).
 */
@Injectable({ providedIn: 'root' })
export class PourvoiCassationSocService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: PourvoiCassationSocRequest):
      Observable<PourvoiCassationSocResponse> {
    return this.http.post<PourvoiCassationSocResponse>(
      `/api/v1/case-files/${caseFileId}/pourvoi-cassation-soc-analysis`, request);
  }

  get(caseFileId: string): Observable<PourvoiCassationSocResponse> {
    return this.http.get<PourvoiCassationSocResponse>(
      `/api/v1/case-files/${caseFileId}/pourvoi-cassation-soc-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-87-pourvoi-cassation-soc')` et
   * sur le seed `decision_tool_visibility_rules` (DecisionToolVisibilityIntegrityIT).
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-87-pourvoi-cassation-soc';
}
