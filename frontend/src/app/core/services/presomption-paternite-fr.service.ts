import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PresomptionPaterniteRequest,
  PresomptionPaterniteResponse,
} from '../models/presomption-paternite-fr.model';

/**
 * SF-216-26 : wrapper HttpClient pour l'outil décisionnel "Présomption
 * de paternité du mari et désaveu" (F-FA-PRESOMPTION-PATERNITE, FR —
 * art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1).
 * Consomme l'API figée dans SF-216-25.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/presomption-paternite
 *   GET  /api/v1/case-files/{caseFileId}/presomption-paternite
 */
@Injectable({ providedIn: 'root' })
export class PresomptionPaterniteFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Présomption de paternité. */
  calculate(
    caseFileId: string,
    request: PresomptionPaterniteRequest,
  ): Observable<PresomptionPaterniteResponse> {
    return this.http.post<PresomptionPaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/presomption-paternite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<PresomptionPaterniteResponse> {
    return this.http.get<PresomptionPaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/presomption-paternite`,
    );
  }
}
