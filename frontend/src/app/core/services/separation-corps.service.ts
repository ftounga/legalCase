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
}
