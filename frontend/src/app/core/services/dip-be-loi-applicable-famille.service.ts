import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DipBeLoiApplicableFamilleRequest,
  DipBeLoiApplicableFamilleResponse,
} from '../models/dip-be-loi-applicable-famille.model';

/**
 * SF-223-07 : wrapper HttpClient pour l'outil décisionnel "Loi applicable en
 * droit de la famille (Belgique — DIP)" (`dip-be-loi-applicable-famille`).
 * Consomme l'API figée dans SF-223-07 (backend).
 */
@Injectable({ providedIn: 'root' })
export class DipBeLoiApplicableFamilleService {
  constructor(private http: HttpClient) {}

  /** POST — détermine la loi applicable et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: DipBeLoiApplicableFamilleRequest,
  ): Observable<DipBeLoiApplicableFamilleResponse> {
    return this.http.post<DipBeLoiApplicableFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/dip-be-loi-applicable-famille-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<DipBeLoiApplicableFamilleResponse> {
    return this.http.get<DipBeLoiApplicableFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/dip-be-loi-applicable-famille-analysis`,
    );
  }
}
