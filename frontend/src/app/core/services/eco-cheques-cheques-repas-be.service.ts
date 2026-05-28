import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EcoChequesChequesRepasBeRequest,
  EcoChequesChequesRepasBeResponse,
} from '../models/eco-cheques-cheques-repas-be.model';

/**
 * SF-219-21b : wrapper HttpClient pour l'outil décisionnel
 * « éco-chèques + chèques-repas BE » (F-219, BE uniquement —
 * CCT n°98 du CNT du 20/02/2009 pour les éco-chèques + Loi du
 * 25/04/2014 portant des dispositions diverses + AR du 03/02/2010
 * modifiant l'art. 19bis de l'AR du 28/11/1969 portant exécution de
 * la loi du 27/06/1969 sur la sécurité sociale des travailleurs).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/eco-cheques-cheques-repas-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Les titres-restaurant français (CGI
 * art. 81 19° ter) reposent sur un mécanisme analogue mais avec un
 * plafond d'exonération distinct (7,18 EUR/jour en 2024, contribution
 * employeur 50-60 %) et il n'existe pas d'équivalent direct des
 * éco-chèques en droit français. Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class EcoChequesChequesRepasBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/eco-cheques-cheques-repas-be`;
  }

  analyze(
    caseFileId: string,
    request: EcoChequesChequesRepasBeRequest,
  ): Observable<EcoChequesChequesRepasBeResponse> {
    return this.http.post<EcoChequesChequesRepasBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<EcoChequesChequesRepasBeResponse> {
    return this.http.get<EcoChequesChequesRepasBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
