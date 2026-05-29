import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegroupementFamilialRequest,
  RegroupementFamilialResponse,
} from '../models/regroupement-familial.model';

/**
 * SF-214-04 : wrapper HttpClient pour l'outil décisionnel
 * "Regroupement familial L.434-1+ CESEDA" (F-IM-26). FR uniquement.
 * Consomme l'API figée dans SF-214-03 (backend).
 *
 * Pattern miroir de {@link EtrangerMaladeService}.
 */
@Injectable({ providedIn: 'root' })
export class RegroupementFamilialService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RegroupementFamilialRequest):
      Observable<RegroupementFamilialResponse> {
    return this.http.post<RegroupementFamilialResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-familial-analysis`, request);
  }

  get(caseFileId: string): Observable<RegroupementFamilialResponse> {
    return this.http.get<RegroupementFamilialResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-familial-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-26-regroupement-familial-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-26-regroupement-familial-fr';
}
