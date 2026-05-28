import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InterimBeIndemniteFinMissionRequest,
  InterimBeIndemniteFinMissionResponse,
} from '../models/interim-be-indemnite-fin-mission.model';

/**
 * SF-219-15b : wrapper HttpClient pour l'outil décisionnel
 * « indemnité de fin de mission intérim BE » (F-219, BE uniquement —
 * Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 + CCT n° 322bis
 * du 16/12/2010 + AR du 30/03/1967 art. 19 + CCT sectorielles +
 * jurisprudence Cass. BE 04/02/1991 et 03/05/2010).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/interim-be-indemnite-fin-mission}.</p>
 *
 * <p>Outil <b>BE-only</b>. La France dispose d'une IFM forfaitaire
 * 10 % (C. trav. fr. art. L. 1251-32) que la jurisprudence Cass. BE
 * refuse explicitement de transposer (Cass. 03/05/2010, S.09.0086.F ;
 * Cass. 23/06/2003, S.02.0103.F). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class InterimBeIndemniteFinMissionService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/interim-be-indemnite-fin-mission`;
  }

  calculate(
    caseFileId: string,
    request: InterimBeIndemniteFinMissionRequest,
  ): Observable<InterimBeIndemniteFinMissionResponse> {
    return this.http.post<InterimBeIndemniteFinMissionResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<InterimBeIndemniteFinMissionResponse> {
    return this.http.get<InterimBeIndemniteFinMissionResponse>(
      this.endpoint(caseFileId),
    );
  }
}
