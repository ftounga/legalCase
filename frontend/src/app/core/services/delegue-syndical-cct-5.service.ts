import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DelegueSyndicalCct5Request,
  DelegueSyndicalCct5Response,
} from '../models/delegue-syndical-cct-5.model';

/**
 * SF-219-10b : wrapper HttpClient pour l'outil décisionnel
 * « Statut du délégué syndical — CCT n° 5 » (F-219, BE uniquement —
 * CCT n° 5 du 24/05/1971 conclue au CNT + CCT 5bis/5ter + AR 26/01/1972 +
 * CCT sectorielles).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/delegue-syndical-cct-5}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent strict FR (le délégué
 * syndical FR relève du Code du travail art. L. 2143-1 et s., élection
 * et désignation distinctes). Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class DelegueSyndicalCct5Service {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/delegue-syndical-cct-5`;
  }

  calculate(
    caseFileId: string,
    request: DelegueSyndicalCct5Request,
  ): Observable<DelegueSyndicalCct5Response> {
    return this.http.post<DelegueSyndicalCct5Response>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<DelegueSyndicalCct5Response> {
    return this.http.get<DelegueSyndicalCct5Response>(
      this.endpoint(caseFileId),
    );
  }
}
