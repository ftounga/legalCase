import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConciliationCphBcaRequest,
  ConciliationCphBcaResponse,
} from '../models/conciliation-cph-bca.model';

/**
 * SF-212-38 : wrapper HttpClient pour l'outil décisionnel "Conciliation CPH
 * — BCO" (F-DT-84, FR — R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT —
 * barème transactions BCA). Consomme l'API figée dans SF-212-37 (backend).
 *
 * <p>F-212 19/19 — dernier outil livré du bundle F-212.</p>
 */
@Injectable({ providedIn: 'root' })
export class ConciliationCphBcaService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de conciliation pour le dossier. */
  calculate(
    caseFileId: string,
    request: ConciliationCphBcaRequest,
  ): Observable<ConciliationCphBcaResponse> {
    return this.http.post<ConciliationCphBcaResponse>(
      `/api/v1/case-files/${caseFileId}/conciliation-cph-bca`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<ConciliationCphBcaResponse> {
    return this.http.get<ConciliationCphBcaResponse>(
      `/api/v1/case-files/${caseFileId}/conciliation-cph-bca`,
    );
  }
}
