import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SaisieRemunerationRequest,
  SaisieRemunerationResponse,
} from '../models/saisie-remuneration.model';

/**
 * SF-218-08 : wrapper HttpClient pour l'outil décisionnel « Saisie sur
 * rémunération (quotité saisissable) » (F-DT-89-saisie-arret-remuneration).
 * FRANCE uniquement. Consomme l'API figée dans SF-218-07 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/saisie-remuneration-analysis
 *
 * Pattern miroir de {@link ExecutionJugementCphService} (F-DT-88, SF-218-04).
 */
@Injectable({ providedIn: 'root' })
export class SaisieRemunerationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: SaisieRemunerationRequest):
      Observable<SaisieRemunerationResponse> {
    return this.http.post<SaisieRemunerationResponse>(
      `/api/v1/case-files/${caseFileId}/saisie-remuneration-analysis`, request);
  }

  get(caseFileId: string): Observable<SaisieRemunerationResponse> {
    return this.http.get<SaisieRemunerationResponse>(
      `/api/v1/case-files/${caseFileId}/saisie-remuneration-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-89-saisie-arret-remuneration')` et
   * sur le seed `decision_tool_visibility_rules` (DecisionToolVisibilityIntegrityIT).
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-89-saisie-arret-remuneration';
}
