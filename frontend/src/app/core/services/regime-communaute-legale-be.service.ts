import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeCommunauteLegaleBeRequest,
  RegimeCommunauteLegaleBeResponse,
} from '../models/regime-communaute-legale-be.model';

/**
 * F-217 SF-217-03 : wrapper HttpClient pour l'outil décisionnel
 * "Régime de communauté légale (Belgique)" (`regime-mat-be-communaute-legale`).
 * Consomme l'API figée dans SF-217-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class RegimeCommunauteLegaleBeService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie et persiste la composition du patrimoine du dossier. */
  calculate(
    caseFileId: string,
    request: RegimeCommunauteLegaleBeRequest,
  ): Observable<RegimeCommunauteLegaleBeResponse> {
    return this.http.post<RegimeCommunauteLegaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/regime-mat-be-communaute-legale`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RegimeCommunauteLegaleBeResponse> {
    return this.http.get<RegimeCommunauteLegaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/regime-mat-be-communaute-legale`,
    );
  }
}
