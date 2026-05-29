import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RenouvellementDelaiRequest,
  RenouvellementDelaiResponse,
} from '../models/renouvellement-delai.model';

/**
 * SF-214-14 : wrapper HttpClient pour l'outil décisionnel
 * "Renouvellement délai dépôt" (F-IM-31-renouvellement-delai-depot-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-13 (backend).
 *
 * Pattern miroir de {@link VlsTsValidationService}.
 */
@Injectable({ providedIn: 'root' })
export class RenouvellementDelaiService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RenouvellementDelaiRequest):
      Observable<RenouvellementDelaiResponse> {
    return this.http.post<RenouvellementDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/renouvellement-delai-analysis`, request);
  }

  get(caseFileId: string): Observable<RenouvellementDelaiResponse> {
    return this.http.get<RenouvellementDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/renouvellement-delai-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-31-renouvellement-delai-depot-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-31-renouvellement-delai-depot-fr';
}
