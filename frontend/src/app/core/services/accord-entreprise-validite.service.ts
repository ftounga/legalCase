import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AccordEntrepriseValiditeRequest,
  AccordEntrepriseValiditeResponse,
} from '../models/accord-entreprise-validite.model';

/**
 * SF-218-32 : wrapper HttpClient pour l'outil décisionnel « Accord d'entreprise :
 * validité (conditions de majorité) » (F-DT-67-accord-entreprise-validite).
 * FRANCE uniquement. Consomme l'API figée dans SF-218-31 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/accord-entreprise-validite-analysis
 *
 * Pattern miroir de {@link NaoNegociationAnnuelleService} (F-DT-66, SF-218-30).
 */
@Injectable({ providedIn: 'root' })
export class AccordEntrepriseValiditeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AccordEntrepriseValiditeRequest):
      Observable<AccordEntrepriseValiditeResponse> {
    return this.http.post<AccordEntrepriseValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/accord-entreprise-validite-analysis`, request);
  }

  get(caseFileId: string): Observable<AccordEntrepriseValiditeResponse> {
    return this.http.get<AccordEntrepriseValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/accord-entreprise-validite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-67-accord-entreprise-validite')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-67-accord-entreprise-validite';
}
