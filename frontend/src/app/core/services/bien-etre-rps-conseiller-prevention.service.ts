import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BienEtreRpsConseillerPreventionRequest,
  BienEtreRpsConseillerPreventionResponse,
} from '../models/bien-etre-rps-conseiller-prevention.model';

/**
 * SF-219-30b : wrapper HttpClient pour l'outil decisionnel « Saisine du
 * Conseiller en Prevention Aspects Psychosociaux (CPAP) — procedure RPS
 * interne BE » (F-219, BE uniquement — Loi du 04/08/1996 art. 32sexies
 * et art. 32bis a 32sexiesdecies + AR du 10/04/2014 art. 11 a 32).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/bien-etre-rps-conseiller-prevention}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le regime francais (Code du travail FR art.
 * L. 4121-1 et s. obligation employeur de securite, referent harcelement
 * art. L. 1153-5-1, CSE consultation art. L. 2312-9, document unique
 * d'evaluation des risques art. R. 4121-1, services de sante au travail
 * SST AR 22/02/1989 et reforme 2 aout 2021) est juridiquement distinct :
 * pas de CPAP belge, pas de delais 10 j / 3 mois / 2 mois, mediation
 * interne facultative non encadree par AR. Aucune transposition
 * mecanique. Le gate {@code workspaceCountry} est porte cote
 * composant ; le backend renvoie 404 pour FR par coherence.</p>
 */
@Injectable({ providedIn: 'root' })
export class BienEtreRpsConseillerPreventionService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/bien-etre-rps-conseiller-prevention`;
  }

  analyze(
    caseFileId: string,
    request: BienEtreRpsConseillerPreventionRequest,
  ): Observable<BienEtreRpsConseillerPreventionResponse> {
    return this.http.post<BienEtreRpsConseillerPreventionResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<BienEtreRpsConseillerPreventionResponse> {
    return this.http.get<BienEtreRpsConseillerPreventionResponse>(
      this.endpoint(caseFileId),
    );
  }
}
