import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AutoriteParentaleRequest,
  AutoriteParentaleResponse,
} from '../models/autorite-parentale.model';

/**
 * SF-FA-19-02 : wrapper HttpClient pour l'outil décisionnel
 * "Autorité parentale — exercice" (F-FA-19, FRANCE uniquement,
 * art. 372-373 / 373-2-10 Cciv).
 *
 * Consomme l'API figée par SF-FA-19-01 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/autorite-parentale → calcul + persistance
 *   - GET  /api/v1/case-files/{caseFileId}/autorite-parentale → récupération
 */
@Injectable({ providedIn: 'root' })
export class AutoriteParentaleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AutoriteParentaleRequest):
      Observable<AutoriteParentaleResponse> {
    return this.http.post<AutoriteParentaleResponse>(
      `/api/v1/case-files/${caseFileId}/autorite-parentale`, request);
  }

  get(caseFileId: string): Observable<AutoriteParentaleResponse> {
    return this.http.get<AutoriteParentaleResponse>(
      `/api/v1/case-files/${caseFileId}/autorite-parentale`);
  }
}
