import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CceExtremeUrgenceBeRequest,
  CceExtremeUrgenceBeResponse,
} from '../models/cce-extreme-urgence-be.model';

/**
 * SF-215-16 : wrapper HttpClient pour l'outil décisionnel
 * « Recours CCE extrême urgence 5j (BE) » (F-IM-32-cce-extreme-urgence-5j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge des étrangers) — pas la Centrale des Crédits.
 *
 * BELGIQUE uniquement — calcul du délai du recours en extrême urgence (5 jours
 * OUVRABLES, Loi 15/12/1980 art. 39/82).
 *
 * Consomme l'API figée dans SF-215-15 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/cce-extreme-urgence-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/cce-extreme-urgence-be-analysis
 *
 * Pattern miroir de {@link CceAnnulationBeService}.
 */
@Injectable({ providedIn: 'root' })
export class CceExtremeUrgenceBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CceExtremeUrgenceBeRequest):
      Observable<CceExtremeUrgenceBeResponse> {
    return this.http.post<CceExtremeUrgenceBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-extreme-urgence-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CceExtremeUrgenceBeResponse> {
    return this.http.get<CceExtremeUrgenceBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-extreme-urgence-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-32-cce-extreme-urgence-5j-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-32-cce-extreme-urgence-5j-be';
}
