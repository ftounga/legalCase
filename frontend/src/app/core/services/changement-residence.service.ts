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
}
