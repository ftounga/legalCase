import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CspCrpConformiteRequest,
  CspCrpConformiteResponse,
} from '../models/csp-crp-fr.model';

/**
 * SF-212-08 : wrapper HttpClient pour l'outil décisionnel "CSP/CRP —
 * conformité de la proposition" (F-DT-44, FR — L. 1233-65 à L. 1233-70 CT ;
 * ANI CSP 19/07/2011 ; DARES). Consomme l'API figée dans SF-212-07 (backend).
 */
@Injectable({ providedIn: 'root' })
export class CspCrpFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de conformité CSP pour le dossier. */
  calculate(
    caseFileId: string,
    request: CspCrpConformiteRequest,
  ): Observable<CspCrpConformiteResponse> {
    return this.http.post<CspCrpConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/csp-crp-conformite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<CspCrpConformiteResponse> {
    return this.http.get<CspCrpConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/csp-crp-conformite`,
    );
  }
}
