import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  StagiaireGratificationRequest,
  StagiaireGratificationResponse,
} from '../models/stagiaire-gratification.model';

/**
 * SF-218-22 : wrapper HttpClient pour l'outil décisionnel « Stagiaire :
 * gratification minimale et requalification en CDI »
 * (F-DT-109-stagiaire-gratification-requalification). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-21 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/stagiaire-gratification-analysis
 *
 * Pattern miroir de {@link CadreDirigeantStatutService} (F-DT-107, SF-218-20).
 */
@Injectable({ providedIn: 'root' })
export class StagiaireGratificationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: StagiaireGratificationRequest):
      Observable<StagiaireGratificationResponse> {
    return this.http.post<StagiaireGratificationResponse>(
      `/api/v1/case-files/${caseFileId}/stagiaire-gratification-analysis`, request);
  }

  get(caseFileId: string): Observable<StagiaireGratificationResponse> {
    return this.http.get<StagiaireGratificationResponse>(
      `/api/v1/case-files/${caseFileId}/stagiaire-gratification-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-109-stagiaire-gratification-requalification')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-109-stagiaire-gratification-requalification';
}
