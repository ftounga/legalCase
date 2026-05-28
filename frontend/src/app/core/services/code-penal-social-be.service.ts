import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CodePenalSocialBeRequest,
  CodePenalSocialBeResponse,
} from '../models/code-penal-social-be.model';

/**
 * SF-219-24b : wrapper HttpClient pour l'outil décisionnel
 * « Code pénal social BE — qualification d'infraction + niveau de
 * sanction 1 à 4 » (F-219, BE uniquement — Loi du 06/06/2010
 * introduisant le Code pénal social, M.B. 01/07/2010, e.e.v.
 * 01/07/2011).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/code-penal-social-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le régime français de répression du travail
 * illégal (C. trav. art. L. 8221-1 et s. + C. pén. art. 121-2 personne
 * morale × 5) constitue un dispositif juridiquement distinct dans ses
 * quanta et sa procédure. Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class CodePenalSocialBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/code-penal-social-be`;
  }

  analyze(
    caseFileId: string,
    request: CodePenalSocialBeRequest,
  ): Observable<CodePenalSocialBeResponse> {
    return this.http.post<CodePenalSocialBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<CodePenalSocialBeResponse> {
    return this.http.get<CodePenalSocialBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
