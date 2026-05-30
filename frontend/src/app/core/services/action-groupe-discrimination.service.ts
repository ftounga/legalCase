import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionGroupeDiscriminationRequest,
  ActionGroupeDiscriminationResponse,
} from '../models/action-groupe-discrimination.model';

/**
 * SF-218-10 : wrapper HttpClient pour l'outil décisionnel « Action de groupe en
 * discrimination » (F-DT-90-action-groupe-discrimination). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-09 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/action-groupe-discrimination-analysis
 *
 * Pattern miroir de {@link SaisieRemunerationService} (F-DT-89, SF-218-08).
 */
@Injectable({ providedIn: 'root' })
export class ActionGroupeDiscriminationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ActionGroupeDiscriminationRequest):
      Observable<ActionGroupeDiscriminationResponse> {
    return this.http.post<ActionGroupeDiscriminationResponse>(
      `/api/v1/case-files/${caseFileId}/action-groupe-discrimination-analysis`, request);
  }

  get(caseFileId: string): Observable<ActionGroupeDiscriminationResponse> {
    return this.http.get<ActionGroupeDiscriminationResponse>(
      `/api/v1/case-files/${caseFileId}/action-groupe-discrimination-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-90-action-groupe-discrimination')`
   * et sur le seed `decision_tool_visibility_rules` (DecisionToolVisibilityIntegrityIT).
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-90-action-groupe-discrimination';
}
