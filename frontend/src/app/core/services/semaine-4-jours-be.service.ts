import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Semaine4JoursBeRequest,
  Semaine4JoursBeResponse,
} from '../models/semaine-4-jours-be.model';

/**
 * SF-219-18b : wrapper HttpClient pour l'outil décisionnel
 * « semaine de 4 jours BE » (F-219, BE uniquement — Loi du 03/10/2022
 * « Deal pour l'emploi » M.B. 10/11/2022, art. 5 + art. 6 + Loi
 * 16/03/1971 art. 19 + Loi 03/07/1978 art. 25 + Loi 08/04/1965
 * art. 12).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/semaine-4-jours-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. La semaine de 4 jours française (C. trav.
 * art. L. 3122-2 et s. — aménagement collectif par accord
 * d'entreprise, Loi 13/06/1998 + Loi 19/01/2000) constitue un régime
 * juridiquement distinct (négociation collective, pas demande
 * individuelle). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class Semaine4JoursBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/semaine-4-jours-be`;
  }

  analyze(
    caseFileId: string,
    request: Semaine4JoursBeRequest,
  ): Observable<Semaine4JoursBeResponse> {
    return this.http.post<Semaine4JoursBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<Semaine4JoursBeResponse> {
    return this.http.get<Semaine4JoursBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
