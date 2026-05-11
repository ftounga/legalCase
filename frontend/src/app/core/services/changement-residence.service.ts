import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ChangementResidenceRequest,
  ChangementResidenceResponse,
} from '../models/changement-residence.model';

/**
 * SF-FA-19-04 : wrapper HttpClient pour l'outil décisionnel
 * "Changement de résidence" (F-FA-19, FRANCE uniquement, art. 373-2 Cciv).
 *
 * Consomme l'API figée par SF-FA-19-03 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/changement-residence → calcul + persistance
 *   - GET  /api/v1/case-files/{caseFileId}/changement-residence → récupération
 */
@Injectable({ providedIn: 'root' })
export class ChangementResidenceService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ChangementResidenceRequest):
      Observable<ChangementResidenceResponse> {
    return this.http.post<ChangementResidenceResponse>(
      `/api/v1/case-files/${caseFileId}/changement-residence`, request);
  }

  get(caseFileId: string): Observable<ChangementResidenceResponse> {
    return this.http.get<ChangementResidenceResponse>(
      `/api/v1/case-files/${caseFileId}/changement-residence`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-19-changement-residence')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-19-changement-residence';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: ChangementResidenceRequest): Observable<ChangementResidenceResponse> {
    return this.http.post<ChangementResidenceResponse>(
      `/api/v1/simulators/${ChangementResidenceService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
