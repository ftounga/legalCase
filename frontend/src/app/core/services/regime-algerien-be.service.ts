import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeAlgerienBeRequest,
  RegimeAlgerienBeResponse,
} from '../models/regime-algerien-be.model';

/**
 * SF-223-05 : wrapper HttpClient pour l'outil décisionnel "Régime algérien —
 * reconnaissance mariage / talaq / dot (Belgique)" (`regime-algerien-be`).
 * Consomme l'API figée dans SF-223-05 (backend).
 */
@Injectable({ providedIn: 'root' })
export class RegimeAlgerienBeService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie le sort de l'acte algérien et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: RegimeAlgerienBeRequest,
  ): Observable<RegimeAlgerienBeResponse> {
    return this.http.post<RegimeAlgerienBeResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-be-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RegimeAlgerienBeResponse> {
    return this.http.get<RegimeAlgerienBeResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-be-analysis`,
    );
  }
}
