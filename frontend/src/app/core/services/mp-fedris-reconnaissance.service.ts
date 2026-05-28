import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MpFedrisReconnaissanceRequest,
  MpFedrisReconnaissanceResponse,
} from '../models/mp-fedris-reconnaissance.model';

/**
 * SF-219-28b : wrapper HttpClient pour l'outil decisionnel
 * « Reconnaissance d'une maladie professionnelle Fedris BE » (F-219,
 * BE uniquement — Lois coordonnees du 03/06/1970 + AR du 28/03/1969
 * liste fermee modifie 06/12/2018 + AR du 16/12/1985 systeme ouvert +
 * Loi du 11/01/2018 reformant Fedris).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/mp-fedris-reconnaissance}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le regime francais (reconnaissance MP CPAM,
 * tableaux art. L. 461-2 CSS, CRRMP art. L. 461-1 CSS, prescription 2
 * ans art. L. 461-5 CSS) est juridiquement distinct dans ses tableaux
 * et sa procedure. Aucun mapping mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 */
@Injectable({ providedIn: 'root' })
export class MpFedrisReconnaissanceService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/mp-fedris-reconnaissance`;
  }

  analyze(
    caseFileId: string,
    request: MpFedrisReconnaissanceRequest,
  ): Observable<MpFedrisReconnaissanceResponse> {
    return this.http.post<MpFedrisReconnaissanceResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<MpFedrisReconnaissanceResponse> {
    return this.http.get<MpFedrisReconnaissanceResponse>(
      this.endpoint(caseFileId),
    );
  }
}
