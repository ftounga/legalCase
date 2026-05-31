import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HarcelementProcedureInterneRequest,
  HarcelementProcedureInterneResponse,
} from '../models/harcelement-procedure-interne.model';

/**
 * SF-218-28 : wrapper HttpClient pour l'outil décisionnel « Harcèlement :
 * procédure interne de traitement d'un signalement »
 * (F-DT-59-harcelement-procedure-interne). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-27 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/harcelement-procedure-interne-analysis
 *
 * Pattern miroir de {@link CadreDirigeantStatutService} (F-DT-107, SF-218-20).
 */
@Injectable({ providedIn: 'root' })
export class HarcelementProcedureInterneService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: HarcelementProcedureInterneRequest):
      Observable<HarcelementProcedureInterneResponse> {
    return this.http.post<HarcelementProcedureInterneResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-procedure-interne-analysis`, request);
  }

  get(caseFileId: string): Observable<HarcelementProcedureInterneResponse> {
    return this.http.get<HarcelementProcedureInterneResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-procedure-interne-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-59-harcelement-procedure-interne')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-59-harcelement-procedure-interne';
}
