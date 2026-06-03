import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RttMonetisationRequest,
  RttMonetisationResponse,
} from '../models/rtt-monetisation.model';

/**
 * SF-218-38 : wrapper HttpClient pour l'outil décisionnel « RTT — monétisation
 * (rachat de jours de RTT) » (F-DT-51-rtt-monetisation). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-37 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/rtt-monetisation-analysis
 *
 * Pattern miroir de {@link ReglementInterieurValiditeService} (F-DT-100,
 * SF-218-36).
 */
@Injectable({ providedIn: 'root' })
export class RttMonetisationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RttMonetisationRequest):
      Observable<RttMonetisationResponse> {
    return this.http.post<RttMonetisationResponse>(
      `/api/v1/case-files/${caseFileId}/rtt-monetisation-analysis`, request);
  }

  get(caseFileId: string): Observable<RttMonetisationResponse> {
    return this.http.get<RttMonetisationResponse>(
      `/api/v1/case-files/${caseFileId}/rtt-monetisation-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-51-rtt-monetisation')` et sur le
   * seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-51-rtt-monetisation';
}
