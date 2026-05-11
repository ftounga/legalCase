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
}
