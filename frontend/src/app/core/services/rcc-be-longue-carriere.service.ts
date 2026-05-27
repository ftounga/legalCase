import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RccBeLongueCarriereRequest,
  RccBeLongueCarriereResponse,
} from '../models/rcc-be-longue-carriere.model';

/**
 * SF-219-02b : wrapper HttpClient pour l'outil décisionnel
 * « RCC BE — longue carrière » (F-219, BE uniquement — Loi 26/12/2013 +
 * CCT n° 17 du 19/12/1974 + AR 03/05/2007 art. 3 — conditions 59+/40).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/rcc-be-longue-carriere}.</p>
 *
 * <p>Outil <b>BE-only</b> — aucun équivalent strict en droit français. Le
 * gate {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence (préserve l'isolation BE-only —
 * pattern miroir SF-207-06b / SF-213-08b).</p>
 *
 * <p>Distinct des autres outils RCC BE :
 * <ul>
 *   <li>{@code rcc-be-conditions} (SF-207-06b) — analyseur 4 régimes parallèles
 *       (général, métiers lourds, longue carrière, entreprise en difficulté).</li>
 *   <li>{@code rcc-be-indemnite-complementaire} (SF-207-07b) — calculateur
 *       d'indemnité complémentaire mensuelle générique.</li>
 *   <li>{@code rcc-be-longue-carriere} (cette SF) — analyseur dédié au régime
 *       longue carrière (AR 03/05/2007 art. 3 — 59+/40 — conditions cumulatives
 *       + indemnité indicative).</li>
 * </ul></p>
 */
@Injectable({ providedIn: 'root' })
export class RccBeLongueCarriereService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rcc-be-longue-carriere`;
  }

  calculate(
    caseFileId: string,
    request: RccBeLongueCarriereRequest,
  ): Observable<RccBeLongueCarriereResponse> {
    return this.http.post<RccBeLongueCarriereResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<RccBeLongueCarriereResponse> {
    return this.http.get<RccBeLongueCarriereResponse>(
      this.endpoint(caseFileId),
    );
  }
}
