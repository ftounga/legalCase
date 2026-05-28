import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EtudiantJobisteBeRequest,
  EtudiantJobisteBeResponse,
} from '../models/etudiant-jobiste-be.model';

/**
 * SF-219-13b : wrapper HttpClient pour l'outil décisionnel
 * « Étudiant jobiste (Belgique) » (F-219, BE uniquement — Loi du
 * 03/07/1978 Titre VII + Loi-programme du 24/12/2002 + AR du 14/07/1995
 * + Loi-programme du 22/12/2023 pérennisant le quota à 600h/an).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/etudiant-jobiste-be}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent FR direct (le « job
 * étudiant » FR relève du droit commun salarié avec abattements
 * limités par l'art. L. 241-3-2 CSS, sans quota horaire annuel ni
 * régime spécifique unifié). Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class EtudiantJobisteBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/etudiant-jobiste-be`;
  }

  calculate(
    caseFileId: string,
    request: EtudiantJobisteBeRequest,
  ): Observable<EtudiantJobisteBeResponse> {
    return this.http.post<EtudiantJobisteBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<EtudiantJobisteBeResponse> {
    return this.http.get<EtudiantJobisteBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
