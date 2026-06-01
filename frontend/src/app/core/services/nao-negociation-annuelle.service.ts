import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NaoNegociationAnnuelleRequest,
  NaoNegociationAnnuelleResponse,
} from '../models/nao-negociation-annuelle.model';

/**
 * SF-218-30 : wrapper HttpClient pour l'outil décisionnel « NAO : négociation
 * annuelle obligatoire » (F-DT-66-nao-negociation-annuelle). FRANCE uniquement.
 * Consomme l'API figée dans SF-218-29 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/nao-negociation-annuelle-analysis
 *
 * Pattern miroir de {@link HarcelementProcedureInterneService} (F-DT-59, SF-218-28).
 */
@Injectable({ providedIn: 'root' })
export class NaoNegociationAnnuelleService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: NaoNegociationAnnuelleRequest):
      Observable<NaoNegociationAnnuelleResponse> {
    return this.http.post<NaoNegociationAnnuelleResponse>(
      `/api/v1/case-files/${caseFileId}/nao-negociation-annuelle-analysis`, request);
  }

  get(caseFileId: string): Observable<NaoNegociationAnnuelleResponse> {
    return this.http.get<NaoNegociationAnnuelleResponse>(
      `/api/v1/case-files/${caseFileId}/nao-negociation-annuelle-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-66-nao-negociation-annuelle')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-66-nao-negociation-annuelle';
}
