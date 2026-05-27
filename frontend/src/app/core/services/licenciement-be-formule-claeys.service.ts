import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeFormuleClaeysRequest,
  LicenciementBeFormuleClaeysResponse,
} from '../models/licenciement-be-formule-claeys.model';

/**
 * SF-213-04b : wrapper HttpClient pour l'outil décisionnel "Préavis
 * Formule Claeys BE" (F-213, BE uniquement — ancien art. 82 §3
 * Loi 03/07/1978 + Cass. 28/02/2011 RG S.10.0073.F + Loi 26/12/2013
 * art. 67 clause de sauvegarde). Consomme l'API figée dans SF-213-04
 * backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-formule-claeys}.</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeFormuleClaeysService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-formule-claeys`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeFormuleClaeysRequest,
  ): Observable<LicenciementBeFormuleClaeysResponse> {
    return this.http.post<LicenciementBeFormuleClaeysResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeFormuleClaeysResponse> {
    return this.http.get<LicenciementBeFormuleClaeysResponse>(this.endpoint(caseFileId));
  }
}
