import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LiquidationCommunauteFrRequest,
  LiquidationCommunauteFrResponse,
} from '../models/liquidation-communaute-fr.model';

/**
 * SF-216-06 : wrapper HttpClient pour l'outil décisionnel "Liquidation
 * communauté légale" (F-FA-04, FR — art. 1467-1517 Cciv). Consomme l'API figée
 * dans SF-216-05 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/liquidation-communaute-fr
 *   GET  /api/v1/case-files/{caseFileId}/liquidation-communaute-fr
 */
@Injectable({ providedIn: 'root' })
export class LiquidationCommunauteFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de liquidation de communauté. */
  calculate(
    caseFileId: string,
    request: LiquidationCommunauteFrRequest,
  ): Observable<LiquidationCommunauteFrResponse> {
    return this.http.post<LiquidationCommunauteFrResponse>(
      `/api/v1/case-files/${caseFileId}/liquidation-communaute-fr`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<LiquidationCommunauteFrResponse> {
    return this.http.get<LiquidationCommunauteFrResponse>(
      `/api/v1/case-files/${caseFileId}/liquidation-communaute-fr`,
    );
  }
}
