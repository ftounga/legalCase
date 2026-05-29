import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesPresenceProuveeRequest,
  AesPresenceProuveeResponse,
} from '../models/aes-presence-prouvee.model';

/**
 * SF-214-12 : wrapper HttpClient pour l'outil décisionnel
 * « AES — calcul de présence prouvée » (F-IM-30). FR uniquement.
 * Consomme l'API figée dans SF-214-11 (backend).
 *
 * Pattern miroir de {@link RegroupementFamilialService}.
 */
@Injectable({ providedIn: 'root' })
export class AesPresenceProuveeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AesPresenceProuveeRequest):
      Observable<AesPresenceProuveeResponse> {
    return this.http.post<AesPresenceProuveeResponse>(
      `/api/v1/case-files/${caseFileId}/aes-presence-prouvee-analysis`, request);
  }

  get(caseFileId: string): Observable<AesPresenceProuveeResponse> {
    return this.http.get<AesPresenceProuveeResponse>(
      `/api/v1/case-files/${caseFileId}/aes-presence-prouvee-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-30-aes-presence-prouvee-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-30-aes-presence-prouvee-fr';
}
