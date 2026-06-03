import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongeParentalEducationRequest,
  CongeParentalEducationResponse,
} from '../models/conge-parental-education.model';

/**
 * SF-218-46 : wrapper HttpClient pour l'outil décisionnel « Congé parental
 * d'éducation » (F-DT-78-conge-parental-education). FRANCE uniquement. Consomme
 * l'API figée dans SF-218-45 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/conge-parental-education-analysis
 *
 * Pattern miroir de {@link CongesEvenementsFamiliauxService} (F-DT-76).
 */
@Injectable({ providedIn: 'root' })
export class CongeParentalEducationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CongeParentalEducationRequest):
      Observable<CongeParentalEducationResponse> {
    return this.http.post<CongeParentalEducationResponse>(
      `/api/v1/case-files/${caseFileId}/conge-parental-education-analysis`, request);
  }

  get(caseFileId: string): Observable<CongeParentalEducationResponse> {
    return this.http.get<CongeParentalEducationResponse>(
      `/api/v1/case-files/${caseFileId}/conge-parental-education-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-78-conge-parental-education')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-78-conge-parental-education';
}
