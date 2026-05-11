import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeAlgerienRequest,
  RegimeAlgerienResponse,
} from '../models/regime-algerien.model';

/**
 * SF-IM-17-02 : wrapper HttpClient pour l'outil décisionnel "Régime
 * algérien" (F-IM-17). FR uniquement (Accord franco-algérien
 * du 27/12/1968 modifié).
 * Consomme l'API figée dans SF-IM-17-01 (backend, mergé PR #653).
 */
@Injectable({ providedIn: 'root' })
export class RegimeAlgerienService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RegimeAlgerienRequest): Observable<RegimeAlgerienResponse> {
    return this.http.post<RegimeAlgerienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-analysis`, request);
  }

  get(caseFileId: string): Observable<RegimeAlgerienResponse> {
    return this.http.get<RegimeAlgerienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-17-regime-algerien')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-17-regime-algerien';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: RegimeAlgerienRequest): Observable<RegimeAlgerienResponse> {
    return this.http.post<RegimeAlgerienResponse>(
      `/api/v1/simulators/${RegimeAlgerienService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
