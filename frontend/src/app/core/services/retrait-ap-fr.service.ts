import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RetraitAutoriteParentaleRequest,
  RetraitAutoriteParentaleResponse,
} from '../models/retrait-ap-fr.model';

/**
 * SF-216-12 : wrapper HttpClient pour l'outil décisionnel "Retrait d'autorité
 * parentale" (F-FA-RETRAIT-AP, FR — art. 378-381 Cciv + loi 2022-140 LMVSS).
 * Consomme l'API figée dans SF-216-11 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/retrait-autorite-parentale
 *   GET  /api/v1/case-files/{caseFileId}/retrait-autorite-parentale
 */
@Injectable({ providedIn: 'root' })
export class RetraitApFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse retrait AP. */
  calculate(
    caseFileId: string,
    request: RetraitAutoriteParentaleRequest,
  ): Observable<RetraitAutoriteParentaleResponse> {
    return this.http.post<RetraitAutoriteParentaleResponse>(
      `/api/v1/case-files/${caseFileId}/retrait-autorite-parentale`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RetraitAutoriteParentaleResponse> {
    return this.http.get<RetraitAutoriteParentaleResponse>(
      `/api/v1/case-files/${caseFileId}/retrait-autorite-parentale`,
    );
  }
}
