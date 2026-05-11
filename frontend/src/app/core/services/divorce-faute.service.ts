import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceFauteRequest,
  DivorceFauteResponse,
} from '../models/divorce-faute.model';

/**
 * SF-FA-09-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour faute" (F-FA-09, art. 242 Cciv).
 * Consomme l'API figée dans SF-FA-09-01 (backend PR #515).
 */
@Injectable({ providedIn: 'root' })
export class DivorceFauteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceFauteRequest):
      Observable<DivorceFauteResponse> {
    return this.http.post<DivorceFauteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-faute`, request);
  }

  get(caseFileId: string): Observable<DivorceFauteResponse> {
    return this.http.get<DivorceFauteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-faute`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-09-divorce-faute')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-09-divorce-faute';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: DivorceFauteRequest): Observable<DivorceFauteResponse> {
    return this.http.post<DivorceFauteResponse>(
      `/api/v1/simulators/${DivorceFauteService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
