import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AjCndaRequest, AjCndaResponse } from '../models/aj-cnda.model';

/**
 * SF-214-20 : wrapper HttpClient pour l'outil décisionnel
 * "Aide juridictionnelle CNDA" (F-IM-34-aj-cnda-fr). FRANCE UNIQUEMENT.
 * Consomme l'API figée dans SF-214-19 (backend).
 *
 * Pattern miroir de {@link VlsTsValidationService}.
 */
@Injectable({ providedIn: 'root' })
export class AjCndaService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AjCndaRequest): Observable<AjCndaResponse> {
    return this.http.post<AjCndaResponse>(
      `/api/v1/case-files/${caseFileId}/aj-cnda-analysis`, request);
  }

  get(caseFileId: string): Observable<AjCndaResponse> {
    return this.http.get<AjCndaResponse>(
      `/api/v1/case-files/${caseFileId}/aj-cnda-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-34-aj-cnda-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-34-aj-cnda-fr';
}
