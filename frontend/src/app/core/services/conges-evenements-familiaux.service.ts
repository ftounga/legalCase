import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongesEvenementsFamiliauxRequest,
  CongesEvenementsFamiliauxResponse,
} from '../models/conges-evenements-familiaux.model';

/**
 * SF-218-44 : wrapper HttpClient pour l'outil décisionnel « Congés pour
 * évènements familiaux » (F-DT-76-conges-evenements-familiaux). FRANCE
 * uniquement. Consomme l'API figée dans SF-218-43 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/conges-evenements-familiaux-analysis
 *
 * Pattern miroir de {@link EpargneSalarialeConformiteService} (F-DT-53).
 */
@Injectable({ providedIn: 'root' })
export class CongesEvenementsFamiliauxService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CongesEvenementsFamiliauxRequest):
      Observable<CongesEvenementsFamiliauxResponse> {
    return this.http.post<CongesEvenementsFamiliauxResponse>(
      `/api/v1/case-files/${caseFileId}/conges-evenements-familiaux-analysis`, request);
  }

  get(caseFileId: string): Observable<CongesEvenementsFamiliauxResponse> {
    return this.http.get<CongesEvenementsFamiliauxResponse>(
      `/api/v1/case-files/${caseFileId}/conges-evenements-familiaux-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-76-conges-evenements-familiaux')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-76-conges-evenements-familiaux';
}
