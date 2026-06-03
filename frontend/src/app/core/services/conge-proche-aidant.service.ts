import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongeProcheAidantRequest,
  CongeProcheAidantResponse,
} from '../models/conge-proche-aidant.model';

/**
 * SF-218-48 : wrapper HttpClient pour l'outil décisionnel « Congé de proche
 * aidant » (F-DT-79-conge-proche-aidant). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-47 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/conge-proche-aidant-analysis
 *
 * Pattern miroir de {@link CongeParentalEducationService} (F-DT-78).
 */
@Injectable({ providedIn: 'root' })
export class CongeProcheAidantService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CongeProcheAidantRequest):
      Observable<CongeProcheAidantResponse> {
    return this.http.post<CongeProcheAidantResponse>(
      `/api/v1/case-files/${caseFileId}/conge-proche-aidant-analysis`, request);
  }

  get(caseFileId: string): Observable<CongeProcheAidantResponse> {
    return this.http.get<CongeProcheAidantResponse>(
      `/api/v1/case-files/${caseFileId}/conge-proche-aidant-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-79-conge-proche-aidant')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-79-conge-proche-aidant';
}
