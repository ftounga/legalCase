import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceDesunionBeRequest,
  DivorceDesunionBeResponse,
} from '../models/divorce-desunion-be.model';

/**
 * SF-FA-11-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour désunion irrémédiable" (Code civil belge, art. 229).
 * Consomme l'API figée dans SF-FA-11-01 (backend).
 *
 * BELGIQUE uniquement.
 */
@Injectable({ providedIn: 'root' })
export class DivorceDesunionBeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceDesunionBeRequest):
      Observable<DivorceDesunionBeResponse> {
    return this.http.post<DivorceDesunionBeResponse>(
      `/api/v1/case-files/${caseFileId}/desunion-irremediable-be`, request);
  }

  get(caseFileId: string): Observable<DivorceDesunionBeResponse> {
    return this.http.get<DivorceDesunionBeResponse>(
      `/api/v1/case-files/${caseFileId}/desunion-irremediable-be`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-11-desunion-irremediable-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-11-desunion-irremediable-be';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: DivorceDesunionBeRequest): Observable<DivorceDesunionBeResponse> {
    return this.http.post<DivorceDesunionBeResponse>(
      `/api/v1/simulators/${DivorceDesunionBeService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
