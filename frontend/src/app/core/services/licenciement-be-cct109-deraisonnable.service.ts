import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeCct109DeraisonnableRequest,
  LicenciementBeCct109DeraisonnableResponse,
} from '../models/licenciement-be-cct109-deraisonnable.model';

/**
 * SF-213-10b : wrapper HttpClient pour l'outil décisionnel
 * « Score CCT n° 109 BE — licenciement manifestement déraisonnable »
 * (F-213, BE uniquement — CCT 109 du 12/02/2014 art. 9).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-cct109-deraisonnable}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (barème
 * Macron L. 1235-3 C. trav.) avec d'autres règles d'indemnisation. Le
 * gate {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence (préserve l'isolation BE-only).</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeCct109DeraisonnableService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeCct109DeraisonnableRequest,
  ): Observable<LicenciementBeCct109DeraisonnableResponse> {
    return this.http.post<LicenciementBeCct109DeraisonnableResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeCct109DeraisonnableResponse> {
    return this.http.get<LicenciementBeCct109DeraisonnableResponse>(
      this.endpoint(caseFileId),
    );
  }
}
