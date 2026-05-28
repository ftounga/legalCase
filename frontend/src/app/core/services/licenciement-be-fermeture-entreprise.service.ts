import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeFermetureEntrepriseRequest,
  LicenciementBeFermetureEntrepriseResponse,
} from '../models/licenciement-be-fermeture-entreprise.model';

/**
 * SF-219-06b : wrapper HttpClient pour l'outil décisionnel
 * « Licenciement BE — fermeture d'entreprise » (F-219, BE uniquement —
 * Loi 26/06/2002 + AR 23/03/2007 + CCT n° 9bis ; Fonds de Fermeture des
 * Entreprises FFE).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-fermeture-entreprise}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent strict FR (FNGS couvre
 * la garantie des salaires en cas de redressement / liquidation, mais
 * ne porte pas d'indemnité de fermeture forfaitaire indépendante).
 * Le gate {@code workspaceCountry} est porté côté composant ; le
 * backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeFermetureEntrepriseService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-fermeture-entreprise`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeFermetureEntrepriseRequest,
  ): Observable<LicenciementBeFermetureEntrepriseResponse> {
    return this.http.post<LicenciementBeFermetureEntrepriseResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeFermetureEntrepriseResponse> {
    return this.http.get<LicenciementBeFermetureEntrepriseResponse>(
      this.endpoint(caseFileId),
    );
  }
}
