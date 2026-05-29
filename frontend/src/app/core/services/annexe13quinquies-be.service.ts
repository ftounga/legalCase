import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Annexe13quinquiesBeRequest,
  Annexe13quinquiesBeResponse,
} from '../models/annexe13quinquies-be.model';

/**
 * SF-215-18 : wrapper HttpClient pour l'outil décisionnel
 * « Annexe 13quinquies OQT + interdiction d'entrée (BE) »
 * (F-IM-33-annexe13quinquies-ie-be).
 *
 * BELGIQUE uniquement — l'annexe 13quinquies est un OQT assorti d'une interdiction
 * d'entrée Schengen (Loi 15/12/1980, art. 74/11). L'outil calcule la durée de l'IE,
 * sa date de fin, la date de levée précoce possible et le délai du recours en
 * annulation devant le CCE (Conseil du Contentieux des Étrangers).
 *
 * Consomme l'API figée dans SF-215-17 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/annexe13quinquies-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/annexe13quinquies-be-analysis
 *
 * Pattern miroir de {@link CceAnnulationBeService}.
 */
@Injectable({ providedIn: 'root' })
export class Annexe13quinquiesBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Annexe13quinquiesBeRequest):
      Observable<Annexe13quinquiesBeResponse> {
    return this.http.post<Annexe13quinquiesBeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13quinquies-be-analysis`, request);
  }

  get(caseFileId: string): Observable<Annexe13quinquiesBeResponse> {
    return this.http.get<Annexe13quinquiesBeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13quinquies-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-33-annexe13quinquies-ie-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-33-annexe13quinquies-ie-be';
}
