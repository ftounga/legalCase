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

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-19-autorite-parentale')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-19-autorite-parentale';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: AutoriteParentaleRequest): Observable<AutoriteParentaleResponse> {
    return this.http.post<AutoriteParentaleResponse>(
      `/api/v1/simulators/${AutoriteParentaleService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
