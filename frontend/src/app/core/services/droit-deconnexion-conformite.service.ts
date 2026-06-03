import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DroitDeconnexionConformiteRequest,
  DroitDeconnexionConformiteResponse,
} from '../models/droit-deconnexion-conformite.model';

/**
 * SF-218-54 : wrapper HttpClient pour l'outil décisionnel « Droit à la
 * déconnexion — conformité » (F-DT-83-droit-deconnexion-conformite). FRANCE
 * uniquement. Consomme l'API figée dans SF-218-53 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/droit-deconnexion-conformite-analysis
 *
 * Pattern miroir de {@link EpargneSalarialeConformiteService} (F-DT-53).
 */
@Injectable({ providedIn: 'root' })
export class DroitDeconnexionConformiteService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DroitDeconnexionConformiteRequest):
      Observable<DroitDeconnexionConformiteResponse> {
    return this.http.post<DroitDeconnexionConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/droit-deconnexion-conformite-analysis`, request);
  }

  get(caseFileId: string): Observable<DroitDeconnexionConformiteResponse> {
    return this.http.get<DroitDeconnexionConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/droit-deconnexion-conformite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-83-droit-deconnexion-conformite')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-83-droit-deconnexion-conformite';
}
