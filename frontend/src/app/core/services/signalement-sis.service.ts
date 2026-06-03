import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SignalementSisRequest,
  SignalementSisResponse,
} from '../models/signalement-sis.model';

/**
 * SF-220-06 : wrapper HttpClient pour l'outil décisionnel "contestation /
 * radiation d'un signalement SIS aux fins de non-admission" (F-IM-52). FR
 * uniquement. Consomme l'API figée dans SF-220-06 (backend).
 *
 * Pattern miroir de {@link DecheanceNationaliteService}.
 */
@Injectable({ providedIn: 'root' })
export class SignalementSisService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: SignalementSisRequest):
      Observable<SignalementSisResponse> {
    return this.http.post<SignalementSisResponse>(
      `/api/v1/case-files/${caseFileId}/signalement-sis-analysis`, request);
  }

  get(caseFileId: string): Observable<SignalementSisResponse> {
    return this.http.get<SignalementSisResponse>(
      `/api/v1/case-files/${caseFileId}/signalement-sis-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-52-signalement-sis-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-52-signalement-sis-fr';
}
