import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TeletravailBeCct85149Request,
  TeletravailBeCct85149Response,
} from '../models/teletravail-be-cct-85-149.model';

/**
 * SF-219-16b : wrapper HttpClient pour l'outil décisionnel « télétravail
 * BE — CCT n° 85 / CCT n° 149 » (F-219, BE uniquement — CCT n° 85 du
 * 09/11/2005 rendue obligatoire AR 13/06/2006 + CCT n° 149 du 26/01/2021
 * + Loi du 03/10/2022 « Deal pour l'emploi » droit à la déconnexion).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/teletravail-be-cct-85-149}.</p>
 *
 * <p>Outil <b>BE-only</b>. La France dispose d'un cadre télétravail
 * structurellement différent (C. trav. art. L. 1222-9 et s. + ANI
 * 26/11/2020, pas de CCT contraignante CNT, déconnexion encadrée par
 * C. trav. L. 2242-17 — négociation annuelle). Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend renvoie
 * 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class TeletravailBeCct85149Service {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/teletravail-be-cct-85-149`;
  }

  calculate(
    caseFileId: string,
    request: TeletravailBeCct85149Request,
  ): Observable<TeletravailBeCct85149Response> {
    return this.http.post<TeletravailBeCct85149Response>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<TeletravailBeCct85149Response> {
    return this.http.get<TeletravailBeCct85149Response>(
      this.endpoint(caseFileId),
    );
  }
}
