import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DiscriminationBeHandicapAmenagementRequest,
  DiscriminationBeHandicapAmenagementResponse,
} from '../models/discrimination-be-handicap-amenagement.model';

/**
 * SF-219-23b : wrapper HttpClient pour l'outil décisionnel
 * « refus d'aménagements raisonnables handicap BE » (F-219, BE
 * uniquement — Loi du 10/05/2007 tendant à lutter contre certaines
 * formes de discrimination + CCT n° 95 du 10/10/2008 du CNT — NAR +
 * Directive 2000/78/CE art. 5 + Convention ONU 13/12/2006 art. 5 et 27).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/discrimination-be-handicap-amenagement}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le régime français d'OETH (C. trav.
 * art. L. 5213-1 et s. + L. 5213-6 obligation d'aménagement raisonnable
 * + L. 1132-1 prohibition de la discrimination) constitue un dispositif
 * juridiquement distinct (taux d'emploi 6 %, contribution AGEFIPH,
 * indemnités L. 1226-14 inaptitude professionnelle). Aucun mapping
 * mécanique. Le gate {@code workspaceCountry} est porté côté composant ;
 * le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class DiscriminationBeHandicapAmenagementService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/discrimination-be-handicap-amenagement`;
  }

  analyze(
    caseFileId: string,
    request: DiscriminationBeHandicapAmenagementRequest,
  ): Observable<DiscriminationBeHandicapAmenagementResponse> {
    return this.http.post<DiscriminationBeHandicapAmenagementResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<DiscriminationBeHandicapAmenagementResponse> {
    return this.http.get<DiscriminationBeHandicapAmenagementResponse>(
      this.endpoint(caseFileId),
    );
  }
}
