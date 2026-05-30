import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AppelCphRequest,
  AppelCphResponse,
} from '../models/appel-cph.model';

/**
 * SF-218-02 : wrapper HttpClient pour l'outil décisionnel « Appel CPH cour
 * d'appel » (F-DT-86-appel-cph-cour-appel). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-01 (backend, déjà mergé) :
 *   POST/GET /api/v1/case-files/{caseFileId}/appel-cph-analysis
 *
 * Pattern miroir de {@link AppelCaaCassationService} (F-IM-41).
 */
@Injectable({ providedIn: 'root' })
export class AppelCphService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AppelCphRequest):
      Observable<AppelCphResponse> {
    return this.http.post<AppelCphResponse>(
      `/api/v1/case-files/${caseFileId}/appel-cph-analysis`, request);
  }

  get(caseFileId: string): Observable<AppelCphResponse> {
    return this.http.get<AppelCphResponse>(
      `/api/v1/case-files/${caseFileId}/appel-cph-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-86-appel-cph-cour-appel')` et sur
   * le seed `decision_tool_visibility_rules` (DecisionToolVisibilityIntegrityIT).
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-86-appel-cph-cour-appel';
}
