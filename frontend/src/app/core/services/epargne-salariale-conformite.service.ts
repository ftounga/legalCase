import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EpargneSalarialeConformiteRequest,
  EpargneSalarialeConformiteResponse,
} from '../models/epargne-salariale-conformite.model';

/**
 * SF-218-42 : wrapper HttpClient pour l'outil décisionnel « Épargne salariale —
 * conformité (intéressement / participation / partage de la valeur) »
 * (F-DT-53-epargne-salariale-conformite). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-41 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/epargne-salariale-conformite-analysis
 *
 * Pattern miroir de {@link PpvExonerationService} (F-DT-52, SF-218-40).
 */
@Injectable({ providedIn: 'root' })
export class EpargneSalarialeConformiteService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: EpargneSalarialeConformiteRequest):
      Observable<EpargneSalarialeConformiteResponse> {
    return this.http.post<EpargneSalarialeConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/epargne-salariale-conformite-analysis`, request);
  }

  get(caseFileId: string): Observable<EpargneSalarialeConformiteResponse> {
    return this.http.get<EpargneSalarialeConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/epargne-salariale-conformite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-53-epargne-salariale-conformite')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-53-epargne-salariale-conformite';
}
