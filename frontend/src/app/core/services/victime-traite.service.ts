import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VictimeTraiteRequest,
  VictimeTraiteResponse,
} from '../models/victime-traite.model';

/**
 * SF-214-22 : wrapper HttpClient pour l'outil décisionnel
 * « Victime de traite — protection L. 425-1 » (F-IM-35). FR uniquement.
 * Consomme l'API figée dans SF-214-21 (backend).
 *
 * Pattern miroir de {@link RecepisseAttestationService}.
 */
@Injectable({ providedIn: 'root' })
export class VictimeTraiteService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VictimeTraiteRequest):
      Observable<VictimeTraiteResponse> {
    return this.http.post<VictimeTraiteResponse>(
      `/api/v1/case-files/${caseFileId}/victime-traite-analysis`, request);
  }

  get(caseFileId: string): Observable<VictimeTraiteResponse> {
    return this.http.get<VictimeTraiteResponse>(
      `/api/v1/case-files/${caseFileId}/victime-traite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-35-victime-traite-l4251-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-35-victime-traite-l4251-fr';
}
