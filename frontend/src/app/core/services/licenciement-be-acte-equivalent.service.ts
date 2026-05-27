import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeActeEquivalentRequest,
  LicenciementBeActeEquivalentResponse,
} from '../models/licenciement-be-acte-equivalent.model';

/**
 * SF-213-09b : wrapper HttpClient pour l'outil décisionnel
 * « Acte équipollent à rupture (Belgique) » (F-213, BE uniquement —
 * Loi du 03/07/1978 art. 20 + Cass. BE 23/12/1957).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-acte-equivalent}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (prise
 * d'acte L. 1237-19 C. trav. FR + résiliation judiciaire). Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend renvoie
 * 404 pour FR par cohérence (préserve l'isolation BE-only).</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeActeEquivalentService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-acte-equivalent`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeActeEquivalentRequest,
  ): Observable<LicenciementBeActeEquivalentResponse> {
    return this.http.post<LicenciementBeActeEquivalentResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeActeEquivalentResponse> {
    return this.http.get<LicenciementBeActeEquivalentResponse>(
      this.endpoint(caseFileId),
    );
  }
}
