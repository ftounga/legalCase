import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LiquidationPartageBeRequest,
  LiquidationPartageBeResponse,
} from '../models/liquidation-partage-be.model';

/**
 * F-217 SF-217-03 : wrapper HttpClient pour l'outil décisionnel
 * "Liquidation-partage post-divorce (Belgique)" (`liquidation-partage-be`).
 * Consomme l'API figée dans SF-217-02 (backend).
 */
@Injectable({ providedIn: 'root' })
export class LiquidationPartageBeService {
  constructor(private http: HttpClient) {}

  /** POST — positionne et persiste l'avancement de la procédure du dossier. */
  calculate(
    caseFileId: string,
    request: LiquidationPartageBeRequest,
  ): Observable<LiquidationPartageBeResponse> {
    return this.http.post<LiquidationPartageBeResponse>(
      `/api/v1/case-files/${caseFileId}/liquidation-partage-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<LiquidationPartageBeResponse> {
    return this.http.get<LiquidationPartageBeResponse>(
      `/api/v1/case-files/${caseFileId}/liquidation-partage-be`,
    );
  }
}
