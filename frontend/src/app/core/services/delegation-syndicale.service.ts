import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DelegationSyndicaleRequest,
  DelegationSyndicaleResponse,
} from '../models/delegation-syndicale.model';

/**
 * SF-218-34 : wrapper HttpClient pour l'outil décisionnel « Délégué syndical /
 * RSS : désignation et protection » (F-DT-69-delegation-syndicale-protection).
 * FRANCE uniquement. Consomme l'API figée dans SF-218-33 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/delegation-syndicale-analysis
 *
 * Pattern miroir de {@link NaoNegociationAnnuelleService} (F-DT-66, SF-218-30).
 */
@Injectable({ providedIn: 'root' })
export class DelegationSyndicaleService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DelegationSyndicaleRequest):
      Observable<DelegationSyndicaleResponse> {
    return this.http.post<DelegationSyndicaleResponse>(
      `/api/v1/case-files/${caseFileId}/delegation-syndicale-analysis`, request);
  }

  get(caseFileId: string): Observable<DelegationSyndicaleResponse> {
    return this.http.get<DelegationSyndicaleResponse>(
      `/api/v1/case-files/${caseFileId}/delegation-syndicale-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-69-delegation-syndicale-protection')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-69-delegation-syndicale-protection';
}
