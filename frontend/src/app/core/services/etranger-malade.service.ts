import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EtrangerMaladeRequest,
  EtrangerMaladeResponse,
} from '../models/etranger-malade.model';

/**
 * SF-214-01 : wrapper HttpClient pour l'outil décisionnel
 * "Étranger malade L.425-9 CESEDA" (F-IM-25). FR uniquement.
 * Consomme l'API figée dans SF-214-01 (backend).
 *
 * Pattern miroir de {@link JldRetentionService}.
 */
@Injectable({ providedIn: 'root' })
export class EtrangerMaladeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: EtrangerMaladeRequest):
      Observable<EtrangerMaladeResponse> {
    return this.http.post<EtrangerMaladeResponse>(
      `/api/v1/case-files/${caseFileId}/etranger-malade-analysis`, request);
  }

  get(caseFileId: string): Observable<EtrangerMaladeResponse> {
    return this.http.get<EtrangerMaladeResponse>(
      `/api/v1/case-files/${caseFileId}/etranger-malade-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-25-etranger-malade-l4259-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-25-etranger-malade-l4259-fr';
}
