import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AppelCaaCassationRequest,
  AppelCaaCassationResponse,
} from '../models/appel-caa-cassation.model';

/**
 * SF-214-34 : wrapper HttpClient pour l'outil décisionnel
 * "Appel CAA / cassation CE" (F-IM-41-appel-caa-cassation-ce-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-33 (backend).
 *
 * Pattern miroir de {@link NaturalisationRecoursTaService} (F-IM-40).
 */
@Injectable({ providedIn: 'root' })
export class AppelCaaCassationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AppelCaaCassationRequest):
      Observable<AppelCaaCassationResponse> {
    return this.http.post<AppelCaaCassationResponse>(
      `/api/v1/case-files/${caseFileId}/appel-caa-cassation-analysis`, request);
  }

  get(caseFileId: string): Observable<AppelCaaCassationResponse> {
    return this.http.get<AppelCaaCassationResponse>(
      `/api/v1/case-files/${caseFileId}/appel-caa-cassation-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-41-appel-caa-cassation-ce-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-41-appel-caa-cassation-ce-fr';
}
