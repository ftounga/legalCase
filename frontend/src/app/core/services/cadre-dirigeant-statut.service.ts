import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CadreDirigeantStatutRequest,
  CadreDirigeantStatutResponse,
} from '../models/cadre-dirigeant-statut.model';

/**
 * SF-218-20 : wrapper HttpClient pour l'outil décisionnel « Cadre dirigeant :
 * qualification (3 critères cumulatifs) » (F-DT-107-cadre-dirigeant-statut).
 * FRANCE uniquement. Consomme l'API figée dans SF-218-19 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/cadre-dirigeant-statut-analysis
 *
 * Pattern miroir de {@link IntermittentSpectacleAreService} (F-DT-106, SF-218-18).
 */
@Injectable({ providedIn: 'root' })
export class CadreDirigeantStatutService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CadreDirigeantStatutRequest):
      Observable<CadreDirigeantStatutResponse> {
    return this.http.post<CadreDirigeantStatutResponse>(
      `/api/v1/case-files/${caseFileId}/cadre-dirigeant-statut-analysis`, request);
  }

  get(caseFileId: string): Observable<CadreDirigeantStatutResponse> {
    return this.http.get<CadreDirigeantStatutResponse>(
      `/api/v1/case-files/${caseFileId}/cadre-dirigeant-statut-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-107-cadre-dirigeant-statut')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-107-cadre-dirigeant-statut';
}
