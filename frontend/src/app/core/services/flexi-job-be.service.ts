import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FlexiJobBeRequest,
  FlexiJobBeResponse,
} from '../models/flexi-job-be.model';

/**
 * SF-219-12b : wrapper HttpClient pour l'outil décisionnel
 * « Flexi-job (Belgique) » (F-219, BE uniquement — Loi-programme du
 * 26/12/2013 art. 13 à 28 + extensions 2015 / 2018 / 2023 +
 * AR 02/06/2024).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/flexi-job-be}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent FR (CDD d'usage et CDI
 * intérimaire FR partagent une logique d'occupation atypique mais le
 * régime de cotisations, de formalisme Dimona et de plafonds annuels
 * est structurellement incomparable). Le gate {@code workspaceCountry}
 * est porté côté composant ; le backend renvoie 404 pour FR par
 * cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class FlexiJobBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/flexi-job-be`;
  }

  calculate(
    caseFileId: string,
    request: FlexiJobBeRequest,
  ): Observable<FlexiJobBeResponse> {
    return this.http.post<FlexiJobBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<FlexiJobBeResponse> {
    return this.http.get<FlexiJobBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
