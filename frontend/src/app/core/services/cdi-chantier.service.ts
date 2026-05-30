import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CdiChantierRequest,
  CdiChantierResponse,
} from '../models/cdi-chantier.model';

/**
 * SF-218-26 : wrapper HttpClient pour l'outil décisionnel « Licenciement CDI de
 * chantier / d'opération » (F-DT-37-licenciement-cdi-chantier). FRANCE
 * uniquement. Consomme l'API figée dans SF-218-25 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/cdi-chantier-analysis
 *
 * Pattern miroir de {@link ApprentissageRuptureService} (F-DT-110, SF-218-24).
 */
@Injectable({ providedIn: 'root' })
export class CdiChantierService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CdiChantierRequest):
      Observable<CdiChantierResponse> {
    return this.http.post<CdiChantierResponse>(
      `/api/v1/case-files/${caseFileId}/cdi-chantier-analysis`, request);
  }

  get(caseFileId: string): Observable<CdiChantierResponse> {
    return this.http.get<CdiChantierResponse>(
      `/api/v1/case-files/${caseFileId}/cdi-chantier-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-37-licenciement-cdi-chantier')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-37-licenciement-cdi-chantier';
}
