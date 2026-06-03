import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeTunisienRequest,
  RegimeTunisienResponse,
} from '../models/regime-tunisien.model';

/**
 * SF-220-01 : wrapper HttpClient pour l'outil décisionnel
 * "Régime franco-tunisien (accord du 17/03/1988)" (F-IM-47). FR uniquement.
 * Consomme l'API figée dans SF-220-01 (backend).
 *
 * Pattern miroir de {@link RegroupementFamilialService}.
 */
@Injectable({ providedIn: 'root' })
export class RegimeTunisienService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RegimeTunisienRequest):
      Observable<RegimeTunisienResponse> {
    return this.http.post<RegimeTunisienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-tunisien-analysis`, request);
  }

  get(caseFileId: string): Observable<RegimeTunisienResponse> {
    return this.http.get<RegimeTunisienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-tunisien-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-47-regime-tunisien-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-47-regime-tunisien-fr';
}
