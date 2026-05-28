import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeCollectifRenaultRequest,
  LicenciementBeCollectifRenaultResponse,
} from '../models/licenciement-be-collectif-renault.model';

/**
 * SF-219-07b : wrapper HttpClient pour l'outil décisionnel
 * « Licenciement collectif BE — Loi Renault » (F-219, BE uniquement —
 * Loi du 13/02/1998 + CCT n° 24 + CCT n° 39 + Directive 98/59/CE).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-collectif-renault}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent strict FR (le PSE français
 * relève d'une procédure DIRECCTE / DDETS différente avec
 * validation/homologation). Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeCollectifRenaultService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-collectif-renault`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeCollectifRenaultRequest,
  ): Observable<LicenciementBeCollectifRenaultResponse> {
    return this.http.post<LicenciementBeCollectifRenaultResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeCollectifRenaultResponse> {
    return this.http.get<LicenciementBeCollectifRenaultResponse>(
      this.endpoint(caseFileId),
    );
  }
}
