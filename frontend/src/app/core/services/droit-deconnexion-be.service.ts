import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DroitDeconnexionBeRequest,
  DroitDeconnexionBeResponse,
} from '../models/droit-deconnexion-be.model';

/**
 * SF-219-19b : wrapper HttpClient pour l'outil décisionnel
 * « droit à la déconnexion BE » (F-219, BE uniquement — Loi du
 * 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022, art. 16 + AR
 * du 19/02/2023 fixant l'entrée en vigueur au 01/04/2023 + CCT n° 149
 * du Conseil national du travail).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/droit-deconnexion-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le droit à la déconnexion français (C. trav.
 * art. L. 2242-17, 7° issu de la Loi n° 2016-1088 du 08/08/2016 dite
 * « El Khomri ») constitue un régime juridiquement distinct (obligation
 * de négociation annuelle dans les entreprises ≥ 50 salariés, sans
 * obligation de résultat — la non-conclusion d'un accord n'expose pas
 * l'employeur aux mêmes sanctions). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class DroitDeconnexionBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/droit-deconnexion-be`;
  }

  analyze(
    caseFileId: string,
    request: DroitDeconnexionBeRequest,
  ): Observable<DroitDeconnexionBeResponse> {
    return this.http.post<DroitDeconnexionBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<DroitDeconnexionBeResponse> {
    return this.http.get<DroitDeconnexionBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
