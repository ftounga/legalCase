import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PrestationCompensatoireRequest,
  PrestationCompensatoireResponse,
} from '../models/prestation-compensatoire-sf216.model';

/**
 * SF-216-02 : wrapper HttpClient pour l'outil décisionnel "Prestation
 * compensatoire" (F-FA-01, FR — art. 270-281 Cciv). Consomme l'API figée
 * dans SF-216-01 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/prestation-compensatoire
 *   GET  /api/v1/case-files/{caseFileId}/prestation-compensatoire
 */
@Injectable({ providedIn: 'root' })
export class PrestationCompensatoireSf216Service {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de prestation compensatoire. */
  calculate(
    caseFileId: string,
    request: PrestationCompensatoireRequest,
  ): Observable<PrestationCompensatoireResponse> {
    return this.http.post<PrestationCompensatoireResponse>(
      `/api/v1/case-files/${caseFileId}/prestation-compensatoire`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<PrestationCompensatoireResponse> {
    return this.http.get<PrestationCompensatoireResponse>(
      `/api/v1/case-files/${caseFileId}/prestation-compensatoire`,
    );
  }
}
