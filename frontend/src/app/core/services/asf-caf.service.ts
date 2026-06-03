import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AsfCafRequest, AsfCafResponse } from '../models/asf-caf.model';

/**
 * SF-222-01 : wrapper HttpClient pour l'outil décisionnel "Allocation de soutien
 * familial" (F-FA-ASF-CAF, FR — art. L. 523-1 CSS). Consomme l'API figée backend.
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/asf-caf-analysis
 *   GET  /api/v1/case-files/{caseFileId}/asf-caf-analysis
 */
@Injectable({ providedIn: 'root' })
export class AsfCafService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse ASF. */
  calculate(caseFileId: string, request: AsfCafRequest): Observable<AsfCafResponse> {
    return this.http.post<AsfCafResponse>(
      `/api/v1/case-files/${caseFileId}/asf-caf-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AsfCafResponse> {
    return this.http.get<AsfCafResponse>(
      `/api/v1/case-files/${caseFileId}/asf-caf-analysis`,
    );
  }
}
