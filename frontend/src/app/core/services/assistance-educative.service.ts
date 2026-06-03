import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AssistanceEducativeRequest,
  AssistanceEducativeResponse,
} from '../models/assistance-educative.model';

/**
 * SF-222-04 : wrapper HttpClient pour l'outil décisionnel "Assistance éducative"
 * — mineur en danger (F-FA-ASSISTANCE-EDUCATIVE, FR — art. 375 et s. Cciv).
 * Consomme l'API figée backend.
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/assistance-educative-analysis
 *   GET  /api/v1/case-files/{caseFileId}/assistance-educative-analysis
 */
@Injectable({ providedIn: 'root' })
export class AssistanceEducativeService {
  constructor(private http: HttpClient) {}

  /** POST — évalue le danger et oriente vers la mesure adaptée, puis persiste. */
  calculate(
    caseFileId: string,
    request: AssistanceEducativeRequest,
  ): Observable<AssistanceEducativeResponse> {
    return this.http.post<AssistanceEducativeResponse>(
      `/api/v1/case-files/${caseFileId}/assistance-educative-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AssistanceEducativeResponse> {
    return this.http.get<AssistanceEducativeResponse>(
      `/api/v1/case-files/${caseFileId}/assistance-educative-analysis`,
    );
  }
}
