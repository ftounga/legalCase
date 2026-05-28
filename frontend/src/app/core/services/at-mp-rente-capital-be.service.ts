import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AtMpRenteCapitalBeRequest,
  AtMpRenteCapitalBeResponse,
} from '../models/at-mp-rente-capital-be.model';

/**
 * SF-219-29b : wrapper HttpClient pour l'outil decisionnel
 * Rente AT/MP vs capitalisation BE (F-219, BE uniquement — Loi du
 * 10/04/1971 art. 24 + Lois coordonnees 03/06/1970 art. 35 + AR
 * 21/12/1971 + AR 10/12/1987 + AR 24/02/2005).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/at-mp-rente-capital-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le regime francais des rentes AT/MP CPAM
 * (art. L. 434-1 et s. CSS, seuils differents, bareme indicatif art.
 * R. 434-2, voies de recours TASS / Pole social TJ) est juridiquement
 * distinct dans son bareme et sa procedure. Aucun mapping mecanique.
 * Le gate {@code workspaceCountry} est porte cote composant ; le
 * backend renvoie 404 pour FR par coherence.</p>
 */
@Injectable({ providedIn: 'root' })
export class AtMpRenteCapitalBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/at-mp-rente-capital-be`;
  }

  analyze(
    caseFileId: string,
    request: AtMpRenteCapitalBeRequest,
  ): Observable<AtMpRenteCapitalBeResponse> {
    return this.http.post<AtMpRenteCapitalBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<AtMpRenteCapitalBeResponse> {
    return this.http.get<AtMpRenteCapitalBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
