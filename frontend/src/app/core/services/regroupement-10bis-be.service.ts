import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Regroupement10bisBeRequest,
  Regroupement10bisBeResponse,
} from '../models/regroupement-10bis-be.model';

/**
 * SF-215-06 : wrapper HttpClient pour l'outil décisionnel
 * « Regroupement familial art. 10bis (BE) » (F-IM-27-regroupement-10bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité d'un regroupement familial sous
 * l'art. 10bis de la Loi du 15/12/1980 (vers un séjour LIMITÉ — carte A —
 * d'où la condition supplémentaire de validité du titre du regroupant à la
 * date d'analyse). Seuil de ressources mensuelles fixé à 120 % du Revenu
 * d'intégration sociale (AR 17/05/2007).
 *
 * Consomme l'API figée dans SF-215-05 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/regroupement-10bis-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/regroupement-10bis-be-analysis
 *
 * Pattern miroir de {@link Regroupement10terBeService}.
 */
@Injectable({ providedIn: 'root' })
export class Regroupement10bisBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Regroupement10bisBeRequest):
      Observable<Regroupement10bisBeResponse> {
    return this.http.post<Regroupement10bisBeResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-10bis-be-analysis`, request);
  }

  get(caseFileId: string): Observable<Regroupement10bisBeResponse> {
    return this.http.get<Regroupement10bisBeResponse>(
      `/api/v1/case-files/${caseFileId}/regroupement-10bis-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-27-regroupement-10bis-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-27-regroupement-10bis-be';
}
