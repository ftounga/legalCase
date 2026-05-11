import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChangementStatutRequest, ChangementStatutResponse } from '../models/changement-statut.model';

/**
 * SF-IM-11-02 : wrapper HttpClient pour l'outil décisionnel "Changement de
 * statut" (F-IM-11). FR uniquement (régime CESEDA).
 * Consomme l'API figée dans SF-IM-11-01 (backend, mergé PR #635).
 */
@Injectable({ providedIn: 'root' })
export class ChangementStatutService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ChangementStatutRequest): Observable<ChangementStatutResponse> {
    return this.http.post<ChangementStatutResponse>(
      `/api/v1/case-files/${caseFileId}/changement-statut-analysis`, request);
  }

  get(caseFileId: string): Observable<ChangementStatutResponse> {
    return this.http.get<ChangementStatutResponse>(
      `/api/v1/case-files/${caseFileId}/changement-statut-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-11-changement-statut')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-11-changement-statut';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: ChangementStatutRequest): Observable<ChangementStatutResponse> {
    return this.http.post<ChangementStatutResponse>(
      `/api/v1/simulators/${ChangementStatutService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
