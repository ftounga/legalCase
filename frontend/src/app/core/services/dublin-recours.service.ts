import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DublinRecoursRequest,
  DublinRecoursResponse,
} from '../models/dublin-recours.model';

/**
 * SF-208-06 : wrapper HttpClient pour l'outil decisionnel
 * "Dublin recours 7 j suspensif FR" (F-IM-22). FR uniquement.
 * Consomme l'API figee dans SF-208-02 (backend, PR #915).
 */
@Injectable({ providedIn: 'root' })
export class DublinRecoursService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DublinRecoursRequest):
      Observable<DublinRecoursResponse> {
    return this.http.post<DublinRecoursResponse>(
      `/api/v1/case-files/${caseFileId}/dublin-recours-analysis`, request);
  }

  get(caseFileId: string): Observable<DublinRecoursResponse> {
    return this.http.get<DublinRecoursResponse>(
      `/api/v1/case-files/${caseFileId}/dublin-recours-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-22-dublin-recours-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-22-dublin-recours-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: DublinRecoursRequest): Observable<DublinRecoursResponse> {
    return this.http.post<DublinRecoursResponse>(
      `/api/v1/simulators/${DublinRecoursService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
