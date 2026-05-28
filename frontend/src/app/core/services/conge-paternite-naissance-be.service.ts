import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongePaterniteNaissanceBeRequest,
  CongePaterniteNaissanceBeResponse,
} from '../models/conge-paternite-naissance-be.model';

/**
 * SF-219-31b : wrapper HttpClient pour l'outil decisionnel
 * Conge paternite / naissance BE (F-219, BE uniquement — Loi du
 * 03/07/1978 art. 30 paragr. 2 + Loi du 12/08/2000 + Loi du
 * 07/04/2023 Deal pour l'emploi + AR du 17/10/1994 + AR du
 * 03/07/1996 indemnisation INAMI).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/conge-paternite-naissance-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le regime francais du conge de paternite
 * (Code du travail FR art. L. 1225-35 et s. — 25 jours calendaires
 * dont 7 obligatoires depuis la LFSS 2021, indemnisation CPAM IJSS
 * Maternite art. L. 331-8 CSS) est juridiquement distinct dans sa
 * duree, son fractionnement et son indemnisation. Aucun mapping
 * mecanique. Le gate {@code workspaceCountry} est porte cote
 * composant ; le backend renvoie 404 pour FR par coherence.</p>
 */
@Injectable({ providedIn: 'root' })
export class CongePaterniteNaissanceBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/conge-paternite-naissance-be`;
  }

  analyze(
    caseFileId: string,
    request: CongePaterniteNaissanceBeRequest,
  ): Observable<CongePaterniteNaissanceBeResponse> {
    return this.http.post<CongePaterniteNaissanceBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<CongePaterniteNaissanceBeResponse> {
    return this.http.get<CongePaterniteNaissanceBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
