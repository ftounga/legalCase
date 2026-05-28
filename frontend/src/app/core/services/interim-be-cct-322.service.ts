import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InterimBeCct322Request,
  InterimBeCct322Response,
} from '../models/interim-be-cct-322.model';

/**
 * SF-219-14b : wrapper HttpClient pour l'outil décisionnel « statut
 * intérim BE — CCT n° 322 » (F-219, BE uniquement — Loi du 24/07/1987 +
 * CCT n° 322 du 14/06/2010 + CCT n° 108 du 16/07/2013 + AR 11/10/1976 +
 * Loi-programme du 27/12/2012).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/interim-be-cct-322}.</p>
 *
 * <p>Outil <b>BE-only</b>. La France dispose d'un régime de travail
 * temporaire structurellement différent (C. trav. art. L. 1251-1 et s.,
 * DPAE non Dimona, motifs et plafonds distincts). Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend renvoie
 * 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class InterimBeCct322Service {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/interim-be-cct-322`;
  }

  calculate(
    caseFileId: string,
    request: InterimBeCct322Request,
  ): Observable<InterimBeCct322Response> {
    return this.http.post<InterimBeCct322Response>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<InterimBeCct322Response> {
    return this.http.get<InterimBeCct322Response>(
      this.endpoint(caseFileId),
    );
  }
}
