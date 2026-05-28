import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClauseEcolageBeRequest,
  ClauseEcolageBeResponse,
} from '../models/clause-ecolage-be.model';

/**
 * SF-219-17b : wrapper HttpClient pour l'outil décisionnel « clause
 * d'écolage BE » (F-219, BE uniquement — art. 22bis Loi du 03/07/1978
 * sur les contrats de travail, inséré par la Loi du 27/12/2006 M.B.
 * 28/12/2006, modifié par la Loi du 22/12/2017 « Pacte de compétitivité »
 * + CCT n° 43 du 02/05/1988 du CNT — RMMMG mensuel + CCT n° 13 du
 * 02/02/2013 du CNT — cadre subsidiaire formations qualifiantes
 * sectorielles).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/clause-ecolage-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. La France dispose d'un régime distinct dit
 * « clause de dédit-formation » d'origine jurisprudentielle (Cass. soc.
 * 17/07/1991 n° 88-40.201) — pas de plafonds chiffrés analogues au § 4
 * art. 22bis BE (½ × RMMMG mensuel pour le coût minimum, 36 mois pour la
 * durée d'efficacité, dégressivité par tiers 100/66/33, plafond absolu
 * 80 % du coût réel). Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class ClauseEcolageBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/clause-ecolage-be`;
  }

  calculate(
    caseFileId: string,
    request: ClauseEcolageBeRequest,
  ): Observable<ClauseEcolageBeResponse> {
    return this.http.post<ClauseEcolageBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<ClauseEcolageBeResponse> {
    return this.http.get<ClauseEcolageBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
