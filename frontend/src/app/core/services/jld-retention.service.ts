import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  JldRetentionRequest,
  JldRetentionResponse,
} from '../models/jld-retention.model';

/**
 * SF-208-05 : wrapper HttpClient pour l'outil decisionnel
 * "JLD retention administrative FR" (F-IM-21). FR uniquement.
 * Consomme l'API figee dans SF-208-01 (backend, PR #915).
 *
 * Pattern miroir de {@link OqtfAvecDelaiService}.
 */
@Injectable({ providedIn: 'root' })
export class JldRetentionService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: JldRetentionRequest):
      Observable<JldRetentionResponse> {
    return this.http.post<JldRetentionResponse>(
      `/api/v1/case-files/${caseFileId}/jld-retention-analysis`, request);
  }

  get(caseFileId: string): Observable<JldRetentionResponse> {
    return this.http.get<JldRetentionResponse>(
      `/api/v1/case-files/${caseFileId}/jld-retention-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-21-jld-retention-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-21-jld-retention-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: JldRetentionRequest): Observable<JldRetentionResponse> {
    return this.http.post<JldRetentionResponse>(
      `/api/v1/simulators/${JldRetentionService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
