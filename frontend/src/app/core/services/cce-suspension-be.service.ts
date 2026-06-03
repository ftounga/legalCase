import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CceSuspensionBeRequest,
  CceSuspensionBeResponse,
} from '../models/cce-suspension-be.model';

/**
 * SF-221-05 : wrapper HttpClient pour l'outil décisionnel « Recours CCE en suspension
 * ordinaire (BE) » (F-IM-57-cce-suspension-be).
 *
 * BELGIQUE uniquement — conditions de la suspension ordinaire (art. 39/82 Loi
 * 15/12/1980, loi 15/09/2006) : urgence + risque de préjudice grave difficilement
 * réparable, greffé sur la requête en annulation 30j. 3e recours CCE distinct de
 * l'annulation 30j (F-IM-31) et de l'extrême urgence 5j (F-IM-32).
 *
 * Consomme l'API figée dans SF-221-05 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/cce-suspension-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/cce-suspension-be-analysis
 */
@Injectable({ providedIn: 'root' })
export class CceSuspensionBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CceSuspensionBeRequest):
      Observable<CceSuspensionBeResponse> {
    return this.http.post<CceSuspensionBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-suspension-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CceSuspensionBeResponse> {
    return this.http.get<CceSuspensionBeResponse>(
      `/api/v1/case-files/${caseFileId}/cce-suspension-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-57-cce-suspension-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-57-cce-suspension-be';
}
