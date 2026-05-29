import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MnaEvaluationAgeRequest, MnaEvaluationAgeResponse } from '../models/mna-evaluation-age.model';

/**
 * SF-214-28 : wrapper HttpClient pour l'outil décisionnel
 * "MNA — évaluation de l'âge / recours JE" (F-IM-38-mna-evaluation-age-fr).
 * FRANCE UNIQUEMENT. Consomme l'API figée dans SF-214-27 (backend).
 *
 * Pattern miroir de {@link AjCndaService}.
 */
@Injectable({ providedIn: 'root' })
export class MnaEvaluationAgeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: MnaEvaluationAgeRequest): Observable<MnaEvaluationAgeResponse> {
    return this.http.post<MnaEvaluationAgeResponse>(
      `/api/v1/case-files/${caseFileId}/mna-evaluation-age-analysis`, request);
  }

  get(caseFileId: string): Observable<MnaEvaluationAgeResponse> {
    return this.http.get<MnaEvaluationAgeResponse>(
      `/api/v1/case-files/${caseFileId}/mna-evaluation-age-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-38-mna-evaluation-age-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-38-mna-evaluation-age-fr';
}
