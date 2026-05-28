import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ElectionsSocialesBeRequest,
  ElectionsSocialesBeResponse,
} from '../models/elections-sociales-be.model';

/**
 * SF-219-09b : wrapper HttpClient pour l'outil décisionnel
 * « Élections sociales (Belgique) » (F-219, BE uniquement — Loi du
 * 04/12/2007 + AR du 25/05/2012 + Loi 04/08/1996 art. 49 + Loi
 * 19/03/1991 protection candidats).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/elections-sociales-be}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent FR (CSE français = calendrier
 * libre déclenché par l'employeur, seuils 11/50/300, pas de jour Y
 * national imposé). Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class ElectionsSocialesBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/elections-sociales-be`;
  }

  calculate(
    caseFileId: string,
    request: ElectionsSocialesBeRequest,
  ): Observable<ElectionsSocialesBeResponse> {
    return this.http.post<ElectionsSocialesBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<ElectionsSocialesBeResponse> {
    return this.http.get<ElectionsSocialesBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
