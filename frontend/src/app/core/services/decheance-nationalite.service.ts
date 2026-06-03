import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DecheanceNationaliteRequest,
  DecheanceNationaliteResponse,
} from '../models/decheance-nationalite.model';

/**
 * SF-220-05 : wrapper HttpClient pour l'outil décisionnel
 * "validité d'une mesure de déchéance de nationalité" (F-IM-51). FR uniquement.
 * Consomme l'API figée dans SF-220-05 (backend).
 *
 * Pattern miroir de {@link PacsVpfService}.
 */
@Injectable({ providedIn: 'root' })
export class DecheanceNationaliteService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DecheanceNationaliteRequest):
      Observable<DecheanceNationaliteResponse> {
    return this.http.post<DecheanceNationaliteResponse>(
      `/api/v1/case-files/${caseFileId}/decheance-nationalite-analysis`, request);
  }

  get(caseFileId: string): Observable<DecheanceNationaliteResponse> {
    return this.http.get<DecheanceNationaliteResponse>(
      `/api/v1/case-files/${caseFileId}/decheance-nationalite-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-51-decheance-nationalite-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-51-decheance-nationalite-fr';
}
