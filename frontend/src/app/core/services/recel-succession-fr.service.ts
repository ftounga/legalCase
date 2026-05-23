import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecelSuccessionRequest,
  RecelSuccessionResponse,
} from '../models/recel-succession-fr.model';

/**
 * SF-216-22 : wrapper HttpClient pour l'outil décisionnel "Recel de
 * succession" (F-FA-RECEL-SUCCESSION, FR — art. 778 Cciv).
 * Consomme l'API figée dans SF-216-21.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/recel-succession
 *   GET  /api/v1/case-files/{caseFileId}/recel-succession
 */
@Injectable({ providedIn: 'root' })
export class RecelSuccessionFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Recel successoral. */
  calculate(
    caseFileId: string,
    request: RecelSuccessionRequest,
  ): Observable<RecelSuccessionResponse> {
    return this.http.post<RecelSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/recel-succession`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RecelSuccessionResponse> {
    return this.http.get<RecelSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/recel-succession`,
    );
  }
}
