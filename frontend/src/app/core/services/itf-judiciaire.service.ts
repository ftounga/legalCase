import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ItfJudiciaireRequest,
  ItfJudiciaireResponse,
} from '../models/itf-judiciaire.model';

/**
 * SF-214-38 : wrapper HttpClient pour l'outil décisionnel
 * "ITF judiciaire" (F-IM-43-itf-judiciaire-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-37 (backend).
 *
 * Pattern miroir de {@link AssignationResidenceService} (F-IM-42).
 */
@Injectable({ providedIn: 'root' })
export class ItfJudiciaireService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ItfJudiciaireRequest):
      Observable<ItfJudiciaireResponse> {
    return this.http.post<ItfJudiciaireResponse>(
      `/api/v1/case-files/${caseFileId}/itf-judiciaire-analysis`, request);
  }

  get(caseFileId: string): Observable<ItfJudiciaireResponse> {
    return this.http.get<ItfJudiciaireResponse>(
      `/api/v1/case-files/${caseFileId}/itf-judiciaire-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-43-itf-judiciaire-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-43-itf-judiciaire-fr';
}
