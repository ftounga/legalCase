import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SeparationCorpsRequest,
  SeparationCorpsResponse,
} from '../models/separation-corps.model';

/**
 * SF-FA-21-02 : wrapper HttpClient pour l'outil décisionnel
 * "Séparation de corps + conversion divorce art. 296+306 Cciv"
 * (F-FA-21, FRANCE uniquement).
 *
 * Consomme l'API figée par SF-FA-21-01 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/separation-corps
 *   - GET  /api/v1/case-files/{caseFileId}/separation-corps
 */
@Injectable({ providedIn: 'root' })
export class SeparationCorpsService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: SeparationCorpsRequest):
      Observable<SeparationCorpsResponse> {
    return this.http.post<SeparationCorpsResponse>(
      `/api/v1/case-files/${caseFileId}/separation-corps`, request);
  }

  get(caseFileId: string): Observable<SeparationCorpsResponse> {
    return this.http.get<SeparationCorpsResponse>(
      `/api/v1/case-files/${caseFileId}/separation-corps`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-21-separation-corps')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-21-separation-corps';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: SeparationCorpsRequest): Observable<SeparationCorpsResponse> {
    return this.http.post<SeparationCorpsResponse>(
      `/api/v1/simulators/${SeparationCorpsService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
