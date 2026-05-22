import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DelegationApFrRequest,
  DelegationApFrResponse,
} from '../models/delegation-ap-fr.model';

/**
 * SF-216-10 : wrapper HttpClient pour l'outil décisionnel "Délégation d'autorité
 * parentale" (F-FA-XX-delegation-ap, FR — art. 376-1 Cciv).
 * Consomme l'API figée dans SF-216-09 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/delegation-ap-fr
 *   GET  /api/v1/case-files/{caseFileId}/delegation-ap-fr
 */
@Injectable({ providedIn: 'root' })
export class DelegationApFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse délégation AP. */
  calculate(
    caseFileId: string,
    request: DelegationApFrRequest,
  ): Observable<DelegationApFrResponse> {
    return this.http.post<DelegationApFrResponse>(
      `/api/v1/case-files/${caseFileId}/delegation-ap-fr`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<DelegationApFrResponse> {
    return this.http.get<DelegationApFrResponse>(
      `/api/v1/case-files/${caseFileId}/delegation-ap-fr`,
    );
  }
}
