import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeProtectionGrossesseRequest,
  LicenciementBeProtectionGrossesseResponse,
} from '../models/licenciement-be-protection-grossesse.model';

/**
 * SF-213-05b : wrapper HttpClient pour l'outil décisionnel "Protection
 * grossesse BE" (F-213, BE uniquement — Loi 16/03/1971 art. 40). Consomme
 * l'API figée dans SF-213-05 backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-protection-grossesse}.</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeProtectionGrossesseService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-protection-grossesse`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeProtectionGrossesseRequest,
  ): Observable<LicenciementBeProtectionGrossesseResponse> {
    return this.http.post<LicenciementBeProtectionGrossesseResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeProtectionGrossesseResponse> {
    return this.http.get<LicenciementBeProtectionGrossesseResponse>(this.endpoint(caseFileId));
  }
}
