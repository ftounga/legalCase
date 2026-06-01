import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReglementInterieurValiditeRequest,
  ReglementInterieurValiditeResponse,
} from '../models/reglement-interieur-validite.model';

/**
 * SF-218-36 : wrapper HttpClient pour l'outil décisionnel « Règlement intérieur :
 * validité » (F-DT-100-reglement-interieur-validite). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-35 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/reglement-interieur-validite-analysis
 *
 * Pattern miroir de {@link NaoNegociationAnnuelleService} (F-DT-66, SF-218-30).
 */
@Injectable({ providedIn: 'root' })
export class ReglementInterieurValiditeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ReglementInterieurValiditeRequest):
      Observable<ReglementInterieurValiditeResponse> {
    return this.http.post<ReglementInterieurValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/reglement-interieur-validite-analysis`, request);
  }

  get(caseFileId: string): Observable<ReglementInterieurValiditeResponse> {
    return this.http.get<ReglementInterieurValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/reglement-interieur-validite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-100-reglement-interieur-validite')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-100-reglement-interieur-validite';
}
