import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CceAnnulationBeRequest,
  CceAnnulationBeResponse,
} from '../models/cce-annulation-be.model';

/**
 * SF-215-14 : wrapper HttpClient pour l'outil décisionnel
 * « Recours CCE annulation 30j (BE) » (F-IM-31-cce-annulation-30j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge des étrangers) — pas la Centrale des Crédits.
 *
 * BELGIQUE uniquement — calcul du délai du recours en annulation (30 jours
 * calendaires, Loi 15/12/1980 art. 39/2 §2 et 39/57 §1er).
 *
 * Consomme l'API figée dans SF-215-13 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/cce-annulation-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/cce-annulation-be-analysis
 *
 * Pattern miroir de {@link Regroupement10terBeService}.
 */
@Injectable({ providedIn: 'root' })
export class CceAnnulationBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CceAnnulationBeRequest):
      Observable<CceAnnulationBeResponse> {
    return this.http.post<CceAnnulationBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-annulation-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CceAnnulationBeResponse> {
    return this.http.get<CceAnnulationBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-annulation-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-31-cce-annulation-30j-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-31-cce-annulation-30j-be';
}
