import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ExecutionJugementCphRequest,
  ExecutionJugementCphResponse,
} from '../models/execution-jugement-cph.model';

/**
 * SF-218-04 : wrapper HttpClient pour l'outil décisionnel « Exécution du jugement
 * CPH / AGS » (F-DT-88-execution-jugement-cph). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-03 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/execution-jugement-cph-analysis
 *
 * Pattern miroir de {@link AppelCphService} (SF-218-02, outil frère F-DT-86).
 */
@Injectable({ providedIn: 'root' })
export class ExecutionJugementCphService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ExecutionJugementCphRequest):
      Observable<ExecutionJugementCphResponse> {
    return this.http.post<ExecutionJugementCphResponse>(
      `/api/v1/case-files/${caseFileId}/execution-jugement-cph-analysis`, request);
  }

  get(caseFileId: string): Observable<ExecutionJugementCphResponse> {
    return this.http.get<ExecutionJugementCphResponse>(
      `/api/v1/case-files/${caseFileId}/execution-jugement-cph-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-88-execution-jugement-cph')` et sur
   * le seed `decision_tool_visibility_rules` (DecisionToolVisibilityIntegrityIT).
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-88-execution-jugement-cph';
}
