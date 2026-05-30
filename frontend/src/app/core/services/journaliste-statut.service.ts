import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  JournalisteStatutRequest,
  JournalisteStatutResponse,
} from '../models/journaliste-statut.model';

/**
 * SF-218-16 : wrapper HttpClient pour l'outil décisionnel « Statut du
 * journaliste professionnel : clauses spécifiques et indemnité de
 * congédiement » (F-DT-105-journaliste-statut). FRANCE uniquement. Consomme
 * l'API figée dans SF-218-15 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/journaliste-statut-analysis
 *
 * Pattern miroir de {@link ParticulierEmployeurCesuService} (F-DT-108, SF-218-14).
 */
@Injectable({ providedIn: 'root' })
export class JournalisteStatutService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: JournalisteStatutRequest):
      Observable<JournalisteStatutResponse> {
    return this.http.post<JournalisteStatutResponse>(
      `/api/v1/case-files/${caseFileId}/journaliste-statut-analysis`, request);
  }

  get(caseFileId: string): Observable<JournalisteStatutResponse> {
    return this.http.get<JournalisteStatutResponse>(
      `/api/v1/case-files/${caseFileId}/journaliste-statut-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-105-journaliste-statut')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-105-journaliste-statut';
}
