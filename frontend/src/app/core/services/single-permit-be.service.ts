import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SinglePermitBeRequest,
  SinglePermitBeResponse,
} from '../models/single-permit-be.model';

/**
 * SF-215-02 : wrapper HttpClient pour l'outil décisionnel « Permis unique BE »
 * (F-IM-25-single-permit-be). BELGIQUE uniquement — combine travail + séjour
 * (single permit, Loi 15/12/1980 art. 61/25-2+ et Accord coopération 02/02/2018).
 *
 * Consomme l'API figée dans SF-215-01 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/single-permit-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/single-permit-be-analysis
 *
 * Pattern miroir de {@link EtrangerMaladeService} / {@link JldRetentionService}.
 */
@Injectable({ providedIn: 'root' })
export class SinglePermitBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: SinglePermitBeRequest):
      Observable<SinglePermitBeResponse> {
    return this.http.post<SinglePermitBeResponse>(
      `/api/v1/case-files/${caseFileId}/single-permit-be-analysis`, request);
  }

  get(caseFileId: string): Observable<SinglePermitBeResponse> {
    return this.http.get<SinglePermitBeResponse>(
      `/api/v1/case-files/${caseFileId}/single-permit-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-25-single-permit-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-25-single-permit-be';
}
