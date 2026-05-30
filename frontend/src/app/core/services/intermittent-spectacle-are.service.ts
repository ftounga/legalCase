import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IntermittentSpectacleAreRequest,
  IntermittentSpectacleAreResponse,
} from '../models/intermittent-spectacle-are.model';

/**
 * SF-218-18 : wrapper HttpClient pour l'outil décisionnel « Intermittent du
 * spectacle : ouverture des droits ARE (annexes 8/10) »
 * (F-DT-106-intermittent-spectacle-are). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-17 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/intermittent-spectacle-are-analysis
 *
 * Pattern miroir de {@link JournalisteStatutService} (F-DT-105, SF-218-16).
 */
@Injectable({ providedIn: 'root' })
export class IntermittentSpectacleAreService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: IntermittentSpectacleAreRequest):
      Observable<IntermittentSpectacleAreResponse> {
    return this.http.post<IntermittentSpectacleAreResponse>(
      `/api/v1/case-files/${caseFileId}/intermittent-spectacle-are-analysis`, request);
  }

  get(caseFileId: string): Observable<IntermittentSpectacleAreResponse> {
    return this.http.get<IntermittentSpectacleAreResponse>(
      `/api/v1/case-files/${caseFileId}/intermittent-spectacle-are-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-106-intermittent-spectacle-are')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-106-intermittent-spectacle-are';
}
