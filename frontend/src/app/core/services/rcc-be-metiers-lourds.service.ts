import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RccBeMetiersLourdsRequest,
  RccBeMetiersLourdsResponse,
} from '../models/rcc-be-metiers-lourds.model';

/**
 * SF-219-01b : wrapper HttpClient pour l'outil décisionnel
 * « RCC métiers lourds (Belgique) » (F-219, BE uniquement —
 * CCT 17 + AR 03/05/2007 art. 3).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/rcc-be-metiers-lourds}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (pré-retraite
 * supprimée 2010, dispositifs amiante / C2P pénibilité). Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend renvoie
 * 404 pour FR par cohérence (préserve l'isolation BE-only).</p>
 */
@Injectable({ providedIn: 'root' })
export class RccBeMetiersLourdsService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rcc-be-metiers-lourds`;
  }

  calculate(
    caseFileId: string,
    request: RccBeMetiersLourdsRequest,
  ): Observable<RccBeMetiersLourdsResponse> {
    return this.http.post<RccBeMetiersLourdsResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<RccBeMetiersLourdsResponse> {
    return this.http.get<RccBeMetiersLourdsResponse>(
      this.endpoint(caseFileId),
    );
  }
}
