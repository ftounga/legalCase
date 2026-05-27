import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeProtectionDelegueeRequest,
  LicenciementBeProtectionDelegueeResponse,
} from '../models/licenciement-be-protection-deleguee.model';

/**
 * SF-213-08b : wrapper HttpClient pour l'outil décisionnel
 * « Licenciement BE — protection délégué syndical » (F-213, BE uniquement —
 * Loi 19/03/1991 + CCT n° 5 du 24/05/1971).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-protection-deleguee}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (statut
 * protégé délégué / CSE / conseiller prud'homme, L. 2411-1 et s. C. trav.)
 * avec d'autres procédures et indemnités. Le gate {@code workspaceCountry}
 * est porté côté composant ; le backend renvoie 404 pour FR par
 * cohérence (préserve l'isolation BE-only).</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeProtectionDelegueeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-protection-deleguee`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeProtectionDelegueeRequest,
  ): Observable<LicenciementBeProtectionDelegueeResponse> {
    return this.http.post<LicenciementBeProtectionDelegueeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeProtectionDelegueeResponse> {
    return this.http.get<LicenciementBeProtectionDelegueeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
