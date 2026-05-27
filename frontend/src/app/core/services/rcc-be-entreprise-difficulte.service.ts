import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RccBeEntrepriseDifficulteRequest,
  RccBeEntrepriseDifficulteResponse,
} from '../models/rcc-be-entreprise-difficulte.model';

/**
 * SF-219-03b : wrapper HttpClient pour l'outil décisionnel
 * « RCC BE — entreprise en difficulté / restructuration » (F-219, BE
 * uniquement — Loi 26/12/2013 + CCT n° 17 + AR 03/05/2007 + AR de
 * reconnaissance ministérielle).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/rcc-be-entreprise-difficulte}.</p>
 *
 * <p>Outil <b>BE-only</b> — aucun équivalent strict en droit français.
 * Le gate {@code workspaceCountry} est porté côté composant ; le
 * backend renvoie 404 pour FR par cohérence (préserve l'isolation
 * BE-only).</p>
 */
@Injectable({ providedIn: 'root' })
export class RccBeEntrepriseDifficulteService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rcc-be-entreprise-difficulte`;
  }

  calculate(
    caseFileId: string,
    request: RccBeEntrepriseDifficulteRequest,
  ): Observable<RccBeEntrepriseDifficulteResponse> {
    return this.http.post<RccBeEntrepriseDifficulteResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<RccBeEntrepriseDifficulteResponse> {
    return this.http.get<RccBeEntrepriseDifficulteResponse>(
      this.endpoint(caseFileId),
    );
  }
}
