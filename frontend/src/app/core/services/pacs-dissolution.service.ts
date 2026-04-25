import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PacsDissolutionRequest,
  PacsDissolutionResponse,
} from '../models/pacs-dissolution.model';

/**
 * SF-FA-20-02 : wrapper HttpClient pour l'outil décisionnel
 * "Dissolution PACS art. 515-7 Cciv" (F-FA-20, FRANCE uniquement).
 *
 * Consomme l'API figée par SF-FA-20-01 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/pacs-dissolution
 *   - GET  /api/v1/case-files/{caseFileId}/pacs-dissolution
 */
@Injectable({ providedIn: 'root' })
export class PacsDissolutionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PacsDissolutionRequest):
      Observable<PacsDissolutionResponse> {
    return this.http.post<PacsDissolutionResponse>(
      `/api/v1/case-files/${caseFileId}/pacs-dissolution`, request);
  }

  get(caseFileId: string): Observable<PacsDissolutionResponse> {
    return this.http.get<PacsDissolutionResponse>(
      `/api/v1/case-files/${caseFileId}/pacs-dissolution`);
  }
}
