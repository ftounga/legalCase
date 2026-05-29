import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RetraitTitreFraudeRequest,
  RetraitTitreFraudeResponse,
} from '../models/retrait-titre-fraude.model';

/**
 * SF-214-42 : wrapper HttpClient pour l'outil décisionnel
 * "Retrait titre pour fraude" (F-IM-45-retrait-titre-fraude-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-41 (backend).
 *
 * Pattern miroir de {@link AppelCaaCassationService} (F-IM-41).
 */
@Injectable({ providedIn: 'root' })
export class RetraitTitreFraudeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RetraitTitreFraudeRequest):
      Observable<RetraitTitreFraudeResponse> {
    return this.http.post<RetraitTitreFraudeResponse>(
      `/api/v1/case-files/${caseFileId}/retrait-titre-fraude-analysis`, request);
  }

  get(caseFileId: string): Observable<RetraitTitreFraudeResponse> {
    return this.http.get<RetraitTitreFraudeResponse>(
      `/api/v1/case-files/${caseFileId}/retrait-titre-fraude-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-45-retrait-titre-fraude-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-45-retrait-titre-fraude-fr';
}
