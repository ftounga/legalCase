import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EgaliteFemmesHommesBeRequest,
  EgaliteFemmesHommesBeResponse,
} from '../models/egalite-femmes-hommes-be.model';

/**
 * SF-219-22b : wrapper HttpClient pour l'outil décisionnel
 * « égalité salariale femmes / hommes BE » (F-219, BE uniquement —
 * Loi du 22/04/2012 visant à lutter contre l'écart salarial entre
 * hommes et femmes + AR du 17/08/2013 portant exécution de l'art. 2
 * + AR du 25/04/2014 formulaires standardisés + CCT n° 25 du
 * 15/10/1975 + C. pén. social art. 195/1).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/egalite-femmes-hommes-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le régime français d'index égalité
 * professionnelle (C. trav. art. L. 1142-7 et s. ; Décret n° 2019-15
 * du 08/01/2019) constitue un dispositif juridiquement distinct
 * (notation 100 points, plan de rattrapage si &lt; 75/100,
 * publication obligatoire). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class EgaliteFemmesHommesBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/egalite-femmes-hommes-be`;
  }

  analyze(
    caseFileId: string,
    request: EgaliteFemmesHommesBeRequest,
  ): Observable<EgaliteFemmesHommesBeResponse> {
    return this.http.post<EgaliteFemmesHommesBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<EgaliteFemmesHommesBeResponse> {
    return this.http.get<EgaliteFemmesHommesBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
