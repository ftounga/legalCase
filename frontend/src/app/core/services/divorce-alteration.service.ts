import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceAlterationRequest,
  DivorceAlterationResponse,
} from '../models/divorce-alteration.model';

/**
 * SF-FA-08-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour altération définitive du lien conjugal" (F-FA-08).
 * Consomme l'API figée dans SF-FA-08-01 (backend PR #513).
 */
@Injectable({ providedIn: 'root' })
export class DivorceAlterationService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceAlterationRequest):
      Observable<DivorceAlterationResponse> {
    return this.http.post<DivorceAlterationResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-alteration`, request);
  }

  get(caseFileId: string): Observable<DivorceAlterationResponse> {
    return this.http.get<DivorceAlterationResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-alteration`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-08-divorce-alteration')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-08-divorce-alteration';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: DivorceAlterationRequest): Observable<DivorceAlterationResponse> {
    return this.http.post<DivorceAlterationResponse>(
      `/api/v1/simulators/${DivorceAlterationService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
