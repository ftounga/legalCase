import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecompensesRequest,
  RecompensesResponse,
} from '../models/recompenses.model';

/**
 * SF-FA-15-02 : service HTTP pour l'outil décisionnel Récompenses
 * (art. 1437/1469 Cciv). Backend SF-FA-15-01 (PR #572).
 */
@Injectable({ providedIn: 'root' })
export class RecompensesService {
  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RecompensesRequest): Observable<RecompensesResponse> {
    return this.http.post<RecompensesResponse>(
      `/api/v1/case-files/${caseFileId}/recompenses`,
      request,
    );
  }

  get(caseFileId: string): Observable<RecompensesResponse> {
    return this.http.get<RecompensesResponse>(
      `/api/v1/case-files/${caseFileId}/recompenses`,
    );
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-15-recompenses')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-15-recompenses';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: RecompensesRequest): Observable<RecompensesResponse> {
    return this.http.post<RecompensesResponse>(
      `/api/v1/simulators/${RecompensesService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
