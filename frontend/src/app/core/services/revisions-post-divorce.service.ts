import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RevisionsPostDivorceRequest,
  RevisionsPostDivorceResponse,
} from '../models/revisions-post-divorce.model';

/**
 * SF-FA-13-02 : wrapper HttpClient pour l'outil décisionnel
 * "Révisions post-divorce" (F-FA-13). Consomme l'API figée dans
 * SF-FA-13-01 (backend, parallèle).
 */
@Injectable({ providedIn: 'root' })
export class RevisionsPostDivorceService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RevisionsPostDivorceRequest):
      Observable<RevisionsPostDivorceResponse> {
    return this.http.post<RevisionsPostDivorceResponse>(
      `/api/v1/case-files/${caseFileId}/revisions-post-divorce`, request);
  }

  get(caseFileId: string): Observable<RevisionsPostDivorceResponse> {
    return this.http.get<RevisionsPostDivorceResponse>(
      `/api/v1/case-files/${caseFileId}/revisions-post-divorce`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-13-revisions-post-divorce')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-13-revisions-post-divorce';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: RevisionsPostDivorceRequest): Observable<RevisionsPostDivorceResponse> {
    return this.http.post<RevisionsPostDivorceResponse>(
      `/api/v1/simulators/${RevisionsPostDivorceService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
