import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Naturalisation12bisBeRequest,
  Naturalisation12bisBeResponse,
} from '../models/naturalisation-12bis-be.model';

/**
 * SF-215-08 : wrapper HttpClient pour l'outil décisionnel
 * « Naturalisation art. 12bis (BE) » (F-IM-28-naturalisation-12bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité d'une déclaration de
 * nationalité belge sous l'art. 12bis du Code de la nationalité belge.
 * Deux voies (5 ans / 10 ans) — verdict `VOIE_5_ANS` / `VOIE_10_ANS` /
 * `AUCUNE` et durée manquante en mois.
 *
 * Consomme l'API figée dans SF-215-07 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/naturalisation-12bis-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/naturalisation-12bis-be-analysis
 *
 * Pattern miroir de {@link Regroupement10bisBeService} (SF-215-06).
 */
@Injectable({ providedIn: 'root' })
export class Naturalisation12bisBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Naturalisation12bisBeRequest):
      Observable<Naturalisation12bisBeResponse> {
    return this.http.post<Naturalisation12bisBeResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-12bis-be-analysis`, request);
  }

  get(caseFileId: string): Observable<Naturalisation12bisBeResponse> {
    return this.http.get<Naturalisation12bisBeResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-12bis-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-28-naturalisation-12bis-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-28-naturalisation-12bis-be';
}
