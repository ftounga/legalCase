import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BurnoutReconnaissanceMpRequest,
  BurnoutReconnaissanceMpResponse,
} from '../models/burnout-reconnaissance-mp.model';

/**
 * SF-212-28 : wrapper HttpClient pour l'outil décisionnel "Burn-out —
 * reconnaissance maladie professionnelle hors tableau" (F-DT-64, FR —
 * L. 461-1 al. 4 et 5 CSS ; CRRMP ; circulaire DGT 2016/01). Consomme
 * l'API figée dans SF-212-27 (backend).
 */
@Injectable({ providedIn: 'root' })
export class BurnoutReconnaissanceMpService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'évaluation des chances de reconnaissance pour le dossier. */
  calculate(
    caseFileId: string,
    request: BurnoutReconnaissanceMpRequest,
  ): Observable<BurnoutReconnaissanceMpResponse> {
    return this.http.post<BurnoutReconnaissanceMpResponse>(
      `/api/v1/case-files/${caseFileId}/burnout-reconnaissance-mp`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<BurnoutReconnaissanceMpResponse> {
    return this.http.get<BurnoutReconnaissanceMpResponse>(
      `/api/v1/case-files/${caseFileId}/burnout-reconnaissance-mp`,
    );
  }
}
