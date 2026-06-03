import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RttAcquisitionRequest,
  RttAcquisitionResponse,
} from '../models/rtt-acquisition.model';

/**
 * SF-218-50 : wrapper HttpClient pour l'outil décisionnel « RTT — acquisition
 * selon accord d'aménagement » (F-DT-80-rtt-acquisition). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-49 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/rtt-acquisition-analysis
 *
 * DISTINCT des heures supplémentaires (F-DT-19) et de la monétisation de RTT
 * (F-DT-51).
 */
@Injectable({ providedIn: 'root' })
export class RttAcquisitionService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RttAcquisitionRequest):
      Observable<RttAcquisitionResponse> {
    return this.http.post<RttAcquisitionResponse>(
      `/api/v1/case-files/${caseFileId}/rtt-acquisition-analysis`, request);
  }

  get(caseFileId: string): Observable<RttAcquisitionResponse> {
    return this.http.get<RttAcquisitionResponse>(
      `/api/v1/case-files/${caseFileId}/rtt-acquisition-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-80-rtt-acquisition')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-80-rtt-acquisition';
}
