import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DesaccordsParentauxRequest,
  DesaccordsParentauxResponse,
} from '../models/desaccords-parentaux.model';

/**
 * SF-FA-19-06 : wrapper HttpClient pour l'outil décisionnel
 * "Désaccords parentaux art. 373-2-10" (F-FA-19, FRANCE uniquement).
 *
 * Consomme l'API figée par SF-FA-19-05 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/desaccords-parentaux
 *   - GET  /api/v1/case-files/{caseFileId}/desaccords-parentaux
 */
@Injectable({ providedIn: 'root' })
export class DesaccordsParentauxService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DesaccordsParentauxRequest):
      Observable<DesaccordsParentauxResponse> {
    return this.http.post<DesaccordsParentauxResponse>(
      `/api/v1/case-files/${caseFileId}/desaccords-parentaux`, request);
  }

  get(caseFileId: string): Observable<DesaccordsParentauxResponse> {
    return this.http.get<DesaccordsParentauxResponse>(
      `/api/v1/case-files/${caseFileId}/desaccords-parentaux`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-19-desaccords-parentaux')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-19-desaccords-parentaux';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: DesaccordsParentauxRequest): Observable<DesaccordsParentauxResponse> {
    return this.http.post<DesaccordsParentauxResponse>(
      `/api/v1/simulators/${DesaccordsParentauxService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
