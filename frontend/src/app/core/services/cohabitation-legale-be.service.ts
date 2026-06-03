import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CohabitationLegaleBeRequest,
  CohabitationLegaleBeResponse,
} from '../models/cohabitation-legale-be.model';

/**
 * SF-223-01 : wrapper HttpClient pour l'outil décisionnel "Régime de la
 * cohabitation légale en Belgique" (`cohabitation-legale-be`). Consomme l'API
 * figée dans SF-223-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class CohabitationLegaleBeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse le régime de la cohabitation légale et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: CohabitationLegaleBeRequest,
  ): Observable<CohabitationLegaleBeResponse> {
    return this.http.post<CohabitationLegaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/cohabitation-legale-be-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<CohabitationLegaleBeResponse> {
    return this.http.get<CohabitationLegaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/cohabitation-legale-be-analysis`,
    );
  }
}
