import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TravailNoirBeDimonaRequest,
  TravailNoirBeDimonaResponse,
} from '../models/travail-noir-be-dimona.model';

/**
 * SF-219-26b : wrapper HttpClient pour l'outil décisionnel « Travail
 * noir BE — DIMONA, requalification et sanctions » (F-219, BE
 * uniquement — Loi-programme du 24/12/2002 art. 167-184 + AR du
 * 05/11/2002 + Code pénal social art. 181 niveau 4 + Loi 22/04/2003
 * art. 28 amende ONSS forfaitaire 3 ×).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/travail-noir-be-dimona}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le régime français du travail dissimulé
 * (C. trav. art. L. 8221-1 et s. — DPAE, parquet, amende 45 000 € pp /
 * 225 000 € pm art. L. 8224-1, URSSAF majoration 25 %) constitue un
 * dispositif juridiquement distinct dans ses quanta et sa procédure.
 * Aucun mapping mécanique. Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class TravailNoirBeDimonaService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/travail-noir-be-dimona`;
  }

  analyze(
    caseFileId: string,
    request: TravailNoirBeDimonaRequest,
  ): Observable<TravailNoirBeDimonaResponse> {
    return this.http.post<TravailNoirBeDimonaResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<TravailNoirBeDimonaResponse> {
    return this.http.get<TravailNoirBeDimonaResponse>(
      this.endpoint(caseFileId),
    );
  }
}
