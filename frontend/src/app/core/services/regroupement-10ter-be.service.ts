import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Regroupement10terBeRequest,
  Regroupement10terBeResponse,
} from '../models/regroupement-10ter-be.model';

/**
 * SF-215-04 : wrapper HttpClient pour l'outil décisionnel
 * « Regroupement familial art. 10ter (BE) » (F-IM-26-regroupement-10ter-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité d'un regroupement familial sous
 * l'art. 10ter de la Loi du 15/12/1980, avec seuil de ressources mensuelles
 * fixé à 120 % du Revenu d'intégration sociale (AR 17/05/2007).
 *
 * Consomme l'API figée dans SF-215-03 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/regroupement-10ter-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/regroupement-10ter-be-analysis
 *
 * Pattern miroir de {@link SinglePermitBeService}.
 */
@Injectable({ providedIn: 'root' })
export class Regroupement10terBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Regroupement10terBeRequest):
      Observable<Regroupement10terBeResponse> {
    return this.http.post<Regroupement10terBeResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-10ter-be-analysis`, request);
  }

  get(caseFileId: string): Observable<Regroupement10terBeResponse> {
    return this.http.get<Regroupement10terBeResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-10ter-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-26-regroupement-10ter-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-26-regroupement-10ter-be';
}
