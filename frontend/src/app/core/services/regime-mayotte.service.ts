import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeMayotteRequest,
  RegimeMayotteResponse,
} from '../models/regime-mayotte.model';

/**
 * SF-220-02 : wrapper HttpClient pour l'outil décisionnel
 * "Portée territoriale du titre à Mayotte" (F-IM-48). FR uniquement.
 * Consomme l'API figée dans SF-220-02 (backend).
 *
 * Pattern miroir de {@link RegimeTunisienService}.
 */
@Injectable({ providedIn: 'root' })
export class RegimeMayotteService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RegimeMayotteRequest):
      Observable<RegimeMayotteResponse> {
    return this.http.post<RegimeMayotteResponse>(
      `/api/v1/case-files/${caseFileId}/regime-mayotte-analysis`, request);
  }

  get(caseFileId: string): Observable<RegimeMayotteResponse> {
    return this.http.get<RegimeMayotteResponse>(
      `/api/v1/case-files/${caseFileId}/regime-mayotte-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-48-regime-mayotte-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-48-regime-mayotte-fr';
}
